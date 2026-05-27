<template>
  <div class="orders">
    <h2>我的订单</h2>
    
    <!-- 骨架屏 Loading -->
    <el-card v-if="loading" class="order-card" v-for="i in 2" :key="i">
      <el-skeleton :rows="3" animated />
    </el-card>
    
    <!-- 空状态 -->
    <div v-else-if="orders.length === 0" class="empty-orders">
      <el-empty :description="getEmptyDescription()">
        <template #image>
          <el-icon :size="80" color="#c0c4cc"><ShoppingCart /></el-icon>
        </template>
        <div class="empty-actions">
          <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
          <el-button v-if="statusFilter !== ''" @click="resetFilter">查看全部订单</el-button>
          <el-button @click="loadOrders">刷新</el-button>
        </div>
      </el-empty>
    </div>
    
    <!-- 订单列表 -->
    <div v-else>
      <!-- 订单状态筛选 -->
      <div class="order-tabs">
        <el-radio-group v-model="statusFilter" @change="loadOrders">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button :value="0">待支付</el-radio-button>
          <el-radio-button :value="1">已支付</el-radio-button>
          <el-radio-button :value="2">已发货</el-radio-button>
          <el-radio-button :value="3">已完成</el-radio-button>
        </el-radio-group>
      </div>
      
      <el-card v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <div class="order-info">
            <span class="order-no">订单号: {{ order.orderNo }}</span>
            <span class="order-time">{{ formatDate(order.createTime) }}</span>
          </div>
          <el-tag :type="getStatusType(order.status)" size="large">
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>
        
        <div class="order-body">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <div class="item-info">
              <span class="item-name">{{ item.productName }}</span>
              <span class="item-spec">数量: {{ item.quantity }}</span>
            </div>
            <span class="item-price">¥{{ item.price }}</span>
          </div>
        </div>
        
        <div class="order-footer">
          <div class="order-total">
            <span class="total-label">订单总额:</span>
            <span class="total-amount">¥{{ order.totalAmount }}</span>
          </div>
          <div class="order-actions">
            <!-- 待支付：去支付、取消订单 -->
            <template v-if="order.status === 0">
              <el-button 
                type="primary" 
                size="small"
                @click="handlePay(order)"
              >
                去支付
              </el-button>
              <el-button 
                type="danger" 
                size="small"
                @click="handleCancel(order)"
              >
                取消订单
              </el-button>
            </template>
            
            <!-- 已支付：查看详情（支付后不能直接取消，应走退款流程） -->
            <template v-else-if="order.status === 1">
              <el-button 
                size="small"
                @click="handleViewDetail(order)"
              >
                查看详情
              </el-button>
              <el-button 
                type="warning" 
                size="small"
                @click="handleRefund(order)"
              >
                申请退款
              </el-button>
            </template>
            
            <!-- 已发货：确认收货、查看详情 -->
            <template v-else-if="order.status === 2">
              <el-button 
                type="success" 
                size="small"
                @click="handleConfirm(order)"
              >
                确认收货
              </el-button>
              <el-button 
                size="small"
                @click="handleViewDetail(order)"
              >
                查看详情
              </el-button>
            </template>
            
            <!-- 已完成：查看详情 -->
            <template v-else-if="order.status === 3">
              <el-button 
                size="small"
                @click="handleViewDetail(order)"
              >
                查看详情
              </el-button>
            </template>
            
            <!-- 已取消：查看详情 -->
            <template v-else-if="order.status === 4">
              <el-button 
                size="small"
                @click="handleViewDetail(order)"
              >
                查看详情
              </el-button>
            </template>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { get, put } from '../utils/request'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const orders = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref<string | number>('')

const getEmptyDescription = () => {
  const currentStatus = String(statusFilter.value)
  if (currentStatus === '') return '您还没有下过订单哦~'
  const statusMap: Record<string, string> = {
    '0': '暂无待支付订单',
    '1': '暂无已支付订单',
    '2': '暂无已发货订单',
    '3': '暂无已完成订单',
    '4': '暂无已取消订单'
  }
  return statusMap[currentStatus] || '暂无相关订单'
}

const resetFilter = () => {
  statusFilter.value = ''
  loadOrders()
}

const loadOrders = async () => {
  loading.value = true
  try {
    const userId = userStore.userInfo?.id
    if (!userId) {
      ElMessage.warning('请先登录')
      return
    }
    
    const url = statusFilter.value !== '' && statusFilter.value !== undefined
      ? `/api/order/list?status=${statusFilter.value}`
      : `/api/order/list`
    
    console.log('加载订单 - URL:', url, '状态过滤:', statusFilter.value)
    const data = await get<any>(url)
    orders.value = data.data || []
    console.log('订单数据:', orders.value)
  } catch (error: any) {
    console.error('加载订单失败', error)
    ElMessage.error('加载订单失败，请重试')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status: number) => {
  const types = ['warning', 'success', 'primary', 'info', 'danger']
  return types[status] || 'info'
}

const getStatusText = (status: number) => {
  const texts = ['待支付', '已支付', '已发货', '已完成', '已取消']
  return texts[status] || '未知'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 去支付
const handlePay = (order: any) => {
  router.push({
    path: '/payment',
    query: {
      orderNo: order.orderNo,
      amount: order.totalAmount
    }
  })
}

// 确认收货
const handleConfirm = async (order: any) => {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'success'
    })
    
    await put(`/api/order/${order.orderNo}/confirm`, {
      confirmTime: new Date().toISOString()
    })
    
    ElMessage.success('确认收货成功')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('确认收货失败', error)
    }
  }
}

// 查看详情
const handleViewDetail = (_order: any) => {
  ElMessage.info('订单详情功能开发中')
}

// 取消订单
const handleCancel = async (order: any) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await put(`/api/order/${order.orderNo}/cancel`)
    
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消订单失败', error)
    }
  }
}

// 申请退款
const handleRefund = async (order: any) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入退款原因', '申请退款', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '请输入退款原因'
    })
    
    await put(`/api/order/${order.orderNo}/refund`, {
      reason: value
    })
    
    ElMessage.success('退款申请成功，退款金额将在1-3个工作日原路返回')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('申请退款失败', error)
    }
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.orders {
  max-width: 1200px;
  margin: 0 auto;
}

.empty-orders {
  padding: 60px 0;
}

.empty-text {
  color: #909399;
  font-size: 14px;
  margin: 10px 0;
}

.order-tabs {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.order-card {
  margin-bottom: 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.order-no {
  font-weight: 500;
  color: #303133;
}

.order-time {
  font-size: 12px;
  color: #909399;
}

.order-body {
  margin-bottom: 15px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
}

.order-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.item-name {
  color: #303133;
  font-weight: 500;
}

.item-spec {
  font-size: 12px;
  color: #909399;
}

.item-price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.order-total {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.total-label {
  color: #909399;
  font-size: 14px;
}

.total-amount {
  color: #f56c6c;
  font-size: 24px;
  font-weight: bold;
}

.order-actions {
  display: flex;
  gap: 10px;
}
</style>
