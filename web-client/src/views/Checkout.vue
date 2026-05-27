<template>
  <div class="checkout">
    <h2>确认订单</h2>
    
    <el-row :gutter="20">
      <el-col :span="16">
        <!-- 收货地址 -->
        <el-card class="address-card">
          <template #header>
            <span>收货地址</span>
          </template>
          <el-form :model="addressForm" label-width="80px">
            <el-form-item label="收货人">
              <el-input v-model="addressForm.name" placeholder="请输入收货人姓名" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="addressForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="详细地址">
              <el-input v-model="addressForm.address" type="textarea" :rows="3" placeholder="请输入详细地址" />
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 商品清单 -->
        <el-card class="items-card" style="margin-top: 20px;">
          <template #header>
            <span>商品清单</span>
          </template>
          <div v-for="item in orderItems" :key="item.productId" class="order-item">
            <el-row :gutter="20" align="middle">
              <el-col :span="14">
                <h4>{{ item.productName }}</h4>
              </el-col>
              <el-col :span="4">
                <span>¥{{ item.price }}</span>
              </el-col>
              <el-col :span="3">
                <span>× {{ item.quantity }}</span>
              </el-col>
              <el-col :span="3">
                <span class="item-total">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 支付方式 -->
        <el-card class="payment-card">
          <template #header>
            <span>支付方式</span>
          </template>
          <el-alert
            title="使用钱包余额支付"
            type="info"
            :closable="false"
            show-icon
          >
            <p>可用余额：<strong>¥{{ walletBalance }}</strong></p>
            <el-link type="primary" @click="router.push('/wallet')">余额不足？去充值</el-link>
          </el-alert>
        </el-card>

        <!-- 订单汇总 -->
        <el-card class="summary-card" style="margin-top: 20px;">
          <template #header>
            <span>订单汇总</span>
          </template>
          <div class="summary-row">
            <span>商品金额</span>
            <span>¥{{ totalAmount.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>运费</span>
            <span>¥0.00</span>
          </div>
          <el-divider />
          <div class="summary-row total">
            <span>应付总额</span>
            <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
          </div>
          
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            @click="submitOrder"
          >
            提交订单
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '../stores/cart'
import { post, put, get } from '../utils/request'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()
const userStore = useUserStore()

const orderItems = ref<any[]>([])
const submitting = ref(false)
const walletBalance = ref(0)

const addressForm = ref({
  name: '张三',
  phone: '13800138000',
  address: '北京市朝阳区xxx街道123号'
})

const totalAmount = computed(() => {
  return orderItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

onMounted(() => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login?redirect=' + encodeURIComponent(route.fullPath))
    return
  }
  
  // 加载钱包余额
  loadWalletBalance()
  
  // 从路由参数或购物车获取商品
  if (route.query.fromCart === 'true') {
    orderItems.value = cartStore.items
  } else if (route.query.productId) {
    // 立即购买单个商品
    orderItems.value = [{
      productId: parseInt(route.query.productId as string),
      productName: route.query.productName as string,
      price: parseFloat(route.query.price as string),
      quantity: parseInt(route.query.quantity as string)
    }]
  }
})

const loadWalletBalance = async () => {
  try {
    const data = await get<any>('/api/wallet/balance')
    if (data.success) {
      walletBalance.value = data.data
    }
  } catch (error) {
    console.error('加载钱包余额失败', error)
  }
}

const submitOrder = async () => {
  if (!addressForm.value.name || !addressForm.value.phone || !addressForm.value.address) {
    ElMessage.warning('请填写完整的收货地址')
    return
  }

  // 检查钱包余额
  if (walletBalance.value < totalAmount.value) {
    const needAmount = totalAmount.value - walletBalance.value
    ElMessageBox.confirm(
      `钱包余额不足（¥${walletBalance.value}），还差¥${needAmount.toFixed(2)}，是否前往充值？`,
      '余额不足',
      {
        confirmButtonText: '去充值',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      router.push('/wallet')
    }).catch(() => {})
    return
  }

  submitting.value = true
  
  try {
    const orderNo = `ORDER${Date.now()}`
    
    // 1. 创建订单
    const data = await post('/api/order', {
      orderNo,
      receiverName: addressForm.value.name,
      receiverPhone: addressForm.value.phone,
      receiverAddress: addressForm.value.address,
      items: orderItems.value.map(item => ({
        productId: item.productId,
        productName: item.productName,
        quantity: item.quantity,
        price: item.price
      }))
    })
    
    console.log('订单创建响应:', data)
    
    if (data.success) {
      // 2. 立即支付
      const payData = await put(`/api/order/${orderNo}/pay`, {
        paymentMethod: 'wallet'
      })
      
      if (payData.success) {
        // 清空购物车
        if (route.query.fromCart === 'true') {
          cartStore.clearCart()
        }
        
        ElMessage.success('支付成功')
        
        // 跳转到订单详情页
        setTimeout(() => {
          router.push('/orders')
        }, 1000)
      } else {
        ElMessage.error(payData.message || '支付失败')
      }
    } else {
      ElMessage.error(data.message || '下单失败')
    }
  } catch (error: any) {
    console.error('下单失败', error)
    ElMessage.error('下单失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout {
  max-width: 1200px;
  margin: 0 auto;
}

.address-card,
.items-card,
.payment-card,
.summary-card {
  margin-bottom: 20px;
}

.order-item {
  padding: 15px 0;
  border-bottom: 1px solid #eee;
}

.order-item:last-child {
  border-bottom: none;
}

.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.payment-icon {
  font-size: 20px;
  margin-right: 8px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin: 10px 0;
  color: #606266;
}

.summary-row.total {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-top: 15px;
}

.total-amount {
  color: #f56c6c;
  font-size: 24px;
}

.submit-btn {
  width: 100%;
  margin-top: 20px;
  font-size: 16px;
  height: 48px;
}
</style>
