<template>
  <div class="wallet-page">
    <el-row :gutter="20">
      <!-- 左侧：钱包信息 -->
      <el-col :span="8">
        <el-card class="wallet-card">
          <template #header>
            <div class="card-header">
              <span>我的钱包</span>
            </div>
          </template>
          
          <div class="balance-section">
            <div class="balance-label">可用余额</div>
            <div class="balance-amount">¥{{ walletInfo?.balance || '0.00' }}</div>
          </div>
          
          <el-divider />
          
          <div class="stats-section">
            <div class="stat-item">
              <div class="stat-label">累计充值</div>
              <div class="stat-value">¥{{ walletInfo?.totalRecharge || '0.00' }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">累计消费</div>
              <div class="stat-value">¥{{ walletInfo?.totalSpent || '0.00' }}</div>
            </div>
          </div>
        </el-card>
        
        <!-- 充值卡片 -->
        <el-card class="recharge-card" style="margin-top: 20px;">
          <template #header>
            <span>充值</span>
          </template>
          
          <el-form @submit.prevent="handleRecharge">
            <el-form-item label="充值金额">
              <el-input-number 
                v-model="rechargeAmount" 
                :min="1" 
                :max="10000" 
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
            
            <!-- 快速充值选项 -->
            <div class="quick-recharge">
              <el-button 
                v-for="amount in [50, 100, 200, 500, 1000, 2000]" 
                :key="amount"
                size="small"
                @click="rechargeAmount = amount"
              >
                ¥{{ amount }}
              </el-button>
            </div>
            
            <el-form-item label="充值方式">
              <el-radio-group v-model="rechargeType">
                <el-radio :value="1">微信</el-radio>
                <el-radio :value="2">支付宝</el-radio>
                <el-radio :value="3">银行卡</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                @click="handleRecharge" 
                :loading="rechargeLoading"
                style="width: 100%"
              >
                立即充值
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      
      <!-- 右侧：充值记录 & 消费记录 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-tabs v-model="activeRecordTab" @tab-change="handleTabChange">
                <el-tab-pane label="充值记录" name="recharge"></el-tab-pane>
                <el-tab-pane label="消费记录" name="spending"></el-tab-pane>
              </el-tabs>
              <el-radio-group v-if="activeRecordTab === 'recharge'" v-model="recordStatus" size="small" @change="loadRecords">
                <el-radio-button :value="undefined">全部</el-radio-button>
                <el-radio-button :value="1">成功</el-radio-button>
                <el-radio-button :value="0">待支付</el-radio-button>
                <el-radio-button :value="2">失败</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          
          <!-- 充值记录表格 -->
          <el-table v-if="activeRecordTab === 'recharge'" :data="rechargeRecords" v-loading="loading" style="width: 100%">
            <el-table-column prop="tradeNo" label="交易单号" width="200" />
            
            <el-table-column label="充值金额" width="120">
              <template #default="{ row }">
                <span class="amount-text">¥{{ row.amount }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="充值方式" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.rechargeType === 1" type="success" size="small">微信</el-tag>
                <el-tag v-else-if="row.rechargeType === 2" type="warning" size="small">支付宝</el-tag>
                <el-tag v-else-if="row.rechargeType === 3" type="info" size="small">银行卡</el-tag>
                <el-tag v-else size="small">其他</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" type="success" size="small">成功</el-tag>
                <el-tag v-else-if="row.status === 0" type="warning" size="small">待支付</el-tag>
                <el-tag v-else-if="row.status === 2" type="danger" size="small">失败</el-tag>
                <el-tag v-else-if="row.status === 3" type="info" size="small">已退款</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="createTime" label="充值时间" width="180" />
            
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
          </el-table>
          
          <!-- 消费记录表格 -->
          <el-table v-else :data="spendingRecords" v-loading="loading" style="width: 100%">
            <el-table-column prop="tradeNo" label="订单号" width="200" />
            
            <el-table-column label="消费金额" width="120">
              <template #default="{ row }">
                <span class="amount-text" style="color: #f56c6c;">-¥{{ row.amount }}</span>
              </template>
            </el-table-column>
            
            <el-table-column prop="products" label="消费商品" show-overflow-tooltip />
            
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag type="danger" size="small">{{ row.remark }}</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="createTime" label="消费时间" width="180" />
          </el-table>
          
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            style="margin-top: 20px; justify-content: center"
            @current-change="handlePageChange"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post } from '../utils/request'
import { useUserStore } from '../stores/user'

const walletInfo = ref<any>(null)
const rechargeRecords = ref<any[]>([])
const spendingRecords = ref<any[]>([])
const loading = ref(false)
const rechargeLoading = ref(false)
const rechargeAmount = ref(100)
const rechargeType = ref(1)
const recordStatus = ref<number | undefined>(undefined)
const activeRecordTab = ref('recharge')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 加载钱包信息
const loadWalletInfo = async () => {
  try {
    const data = await get<any>('/api/wallet/info')
    if (data.success) {
      walletInfo.value = data.data
    }
  } catch (error: any) {
    console.error('加载钱包信息失败', error)
    // 如果是 401，request.ts 会自动跳转登录页
  }
}

// 加载充值记录
const loadRechargeRecords = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      pageNum: pageNum.value.toString(),
      pageSize: pageSize.value.toString()
    })
    
    if (recordStatus.value !== undefined) {
      params.append('status', recordStatus.value.toString())
    }
    
    const data = await get<any>(`/api/wallet/recharge-records?${params}`)
    if (data.success && data.data) {
      rechargeRecords.value = data.data.records || data.data
      total.value = data.data.total || 0
    }
  } catch (error) {
    console.error('加载充值记录失败', error)
  } finally {
    loading.value = false
  }
}

// 加载消费记录
const loadSpendingRecords = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      pageNum: pageNum.value.toString(),
      pageSize: pageSize.value.toString()
    })
    
    const data = await get<any>(`/api/wallet/spending-records?${params}`)
    if (data.success && data.data) {
      spendingRecords.value = data.data.records || []
      total.value = data.data.total || 0
    }
  } catch (error) {
    console.error('加载消费记录失败', error)
  } finally {
    loading.value = false
  }
}

// 统一加载记录方法
const loadRecords = async () => {
  if (activeRecordTab.value === 'recharge') {
    loadRechargeRecords()
  } else {
    loadSpendingRecords()
  }
}

// Tab切换
const handleTabChange = () => {
  pageNum.value = 1
  loadRecords()
}

// 页码切换
const handlePageChange = () => {
  loadRecords()
}

// 充值
const handleRecharge = async () => {
  if (rechargeAmount.value <= 0) {
    ElMessage.warning('请输入充值金额')
    return
  }
  
  rechargeLoading.value = true
  try {
    // 使用 URLSearchParams 替代 FormData，以 application/x-www-form-urlencoded 格式发送
    const params = new URLSearchParams()
    params.append('amount', rechargeAmount.value.toString())
    params.append('rechargeType', rechargeType.value.toString())
    
    const data = await post<any>('/api/wallet/recharge', params)
    
    if (data.success) {
      ElMessage.success('充值成功')
      loadWalletInfo()
      loadRecords()
    } else {
      ElMessage.error(data.message || '充值失败')
    }
  } catch (error: any) {
    console.error('充值失败', error)
    // request.ts 已经处理了错误提示，这里不需要重复提示
  } finally {
    rechargeLoading.value = false
  }
}

onMounted(() => {
  loadWalletInfo()
  loadRecords()
})
</script>

<style scoped>
.wallet-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wallet-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.wallet-card :deep(.el-card__header) {
  border-bottom: none;
  color: white;
}

.balance-section {
  text-align: center;
  padding: 20px 0;
}

.balance-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 10px;
}

.balance-amount {
  font-size: 36px;
  font-weight: bold;
}

.stats-section {
  display: flex;
  justify-content: space-around;
  padding: 10px 0;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
}

.recharge-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.quick-recharge {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.amount-text {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}
</style>
