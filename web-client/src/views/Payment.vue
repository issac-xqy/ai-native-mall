<template>
  <div class="payment">
    <el-card class="payment-card">
      <!-- 安全提示 -->
      <el-alert
        title="安全支付保障"
        type="success"
        :closable="false"
        show-icon
        class="security-alert"
      >
        <template #default>
          <div class="security-info">
            <span>SSL 加密传输 | 资金安全保障 | 隐私信息保护</span>
          </div>
        </template>
      </el-alert>
      
      <div class="payment-header">
        <h2>支付订单</h2>
        <p class="subtitle">请确认以下订单信息并完成支付</p>
      </div>

      <el-divider />

      <div class="payment-info">
        <div class="info-row">
          <span class="label">订单编号</span>
          <span class="value">{{ orderNo }}</span>
        </div>
        <div class="info-row">
          <span class="label">支付金额</span>
          <span class="amount">¥{{ amount }}</span>
        </div>
      </div>

      <el-divider />

      <div class="payment-methods">
        <h3>选择支付方式</h3>
        
        <div class="method-grid">
          <div
            v-for="method in paymentMethods"
            :key="method.id"
            :class="['method-item', { active: selectedMethod === method.id }]"
            @click="selectedMethod = method.id"
          >
            <div class="method-name">{{ method.name }}</div>
            <div class="method-desc">{{ method.desc }}</div>
          </div>
        </div>
      </div>

      <div class="payment-actions">
        <el-button size="large" @click="goBack">返回</el-button>
        <el-button
          type="primary"
          size="large"
          :loading="processing"
          @click="processPayment"
        >
          确认支付 ¥{{ amount }}
        </el-button>
      </div>

      <el-alert
        v-if="paymentSuccess"
        title="支付成功！"
        type="success"
        :closable="false"
        show-icon
        style="margin-top: 20px;"
      >
        <p>订单支付成功，正在跳转到订单详情页...</p>
      </el-alert>
      
      <!-- 信任标识 -->
      <div class="trust-badges">
        <div class="trust-item">
          <el-icon :size="24" color="#67c23a"><CircleCheck /></el-icon>
          <span>营业执照认证</span>
        </div>
        <div class="trust-item">
          <el-icon :size="24" color="#67c23a"><CircleCheck /></el-icon>
          <span>ICP 备案</span>
        </div>
        <div class="trust-item">
          <el-icon :size="24" color="#67c23a"><CircleCheck /></el-icon>
          <span>消费者保障</span>
        </div>
      </div>
      
      <!-- 客服入口 -->
      <div class="customer-service">
        <span class="cs-label">支付遇到问题？</span>
        <el-link type="primary" @click="router.push('/ai-chat')">联系AI在线客服</el-link>
        <span class="cs-divider">|</span>
        <span class="cs-phone">客服电话：400-888-8888</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { put } from '../utils/request'

const router = useRouter()
const route = useRoute()

const orderNo = ref('')
const amount = ref(0)
const selectedMethod = ref('alipay')
const processing = ref(false)
const paymentSuccess = ref(false)

const paymentMethods = [
  { id: 'alipay', name: '支付宝', desc: '推荐使用' },
  { id: 'wechat', name: '微信支付', desc: '便捷支付' },
  { id: 'bank', name: '银行卡', desc: '企业用户' }
]

onMounted(() => {
  orderNo.value = (route.query.orderNo as string) || ''
  amount.value = parseFloat(route.query.amount as string) || 0
})

const processPayment = async () => {
  processing.value = true
  
  try {
    const data = await put(`/api/order/${orderNo.value}/pay`, {
      paymentMethod: selectedMethod.value,
      paymentTime: new Date().toISOString()
    })
    
    if (data.success) {
      paymentSuccess.value = true
      ElMessage.success('支付成功')
      
      // 2秒后跳转到订单详情页
      setTimeout(() => {
        router.push('/orders')
      }, 2000)
    } else {
      // 处理余额不足的情况
      if (data.needRecharge) {
        const needAmount = data.needRecharge
        ElMessageBox.confirm(
          `钱包余额不足，还差¥${needAmount}，是否前往钱包充值？`,
          '支付失败',
          {
            confirmButtonText: '去充值',
            cancelButtonText: '返回',
            type: 'warning'
          }
        ).then(() => {
          router.push('/wallet')
        }).catch(() => {})
      } else {
        ElMessage.error(data.message || '支付失败')
      }
    }
  } catch (error: any) {
    console.error('支付失败', error)
    ElMessage.error('支付失败: ' + (error.message || '请稍后重试'))
  } finally {
    processing.value = false
  }
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped>
.payment {
  max-width: 800px;
  margin: 0 auto;
}

.payment-card {
  padding: 40px;
}

.security-alert {
  margin-bottom: 20px;
}

.security-info {
  font-size: 14px;
  color: #606266;
}

.payment-header {
  text-align: center;
  margin-bottom: 20px;
}

.payment-header h2 {
  color: #303133;
  margin: 0;
}

.subtitle {
  color: #909399;
  font-size: 14px;
  margin: 10px 0 0 0;
}

.payment-info {
  margin: 30px 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  font-size: 16px;
}

.label {
  color: #909399;
}

.value {
  color: #303133;
  font-weight: 500;
}

.amount {
  color: #f56c6c;
  font-size: 28px;
  font-weight: bold;
}

.payment-methods h3 {
  margin-bottom: 20px;
  color: #303133;
}

.method-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.method-item {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.method-item:hover {
  border-color: #409EFF;
  background: #f5f7fa;
}

.method-item.active {
  border-color: #409EFF;
  background: #ecf5ff;
}

.method-name {
  font-size: 16px;
  color: #303133;
  margin-bottom: 5px;
}

.method-desc {
  font-size: 12px;
  color: #909399;
}

.payment-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-top: 30px;
}

.payment-actions .el-button {
  min-width: 150px;
  font-size: 16px;
  height: 48px;
}

.trust-badges {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-top: 30px;
  padding: 20px 0;
  border-top: 1px solid #ebeef5;
}

.trust-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.customer-service {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 14px;
}

.cs-label {
  color: #909399;
}

.cs-divider {
  color: #dcdfe6;
}

.cs-phone {
  color: #606266;
  font-weight: 500;
}
</style>
