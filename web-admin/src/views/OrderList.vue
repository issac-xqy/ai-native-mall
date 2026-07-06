<template>
  <div class="order-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
          <el-button type="primary" @click="loadOrders" :icon="Refresh">刷新</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-tabs v-model="activeStatus" @tab-change="loadOrders">
        <el-tab-pane label="全部订单" :name="null" />
        <el-tab-pane label="待处理" :name="1" />
        <el-tab-pane label="已发货" :name="2" />
        <el-tab-pane label="已完成" :name="3" />
        <el-tab-pane label="已取消" :name="4" />
      </el-tabs>

      <el-table :data="orders" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="商品信息" min-width="250">
          <template #default="{ row }">
            <div v-for="item in row.items" :key="item.id" class="order-item">
              {{ item.productName }} x {{ item.quantity }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 1" 
              type="success" 
              size="small" 
              @click="handleShip(row)"
            >
              发货
            </el-button>
            <el-tag v-else-if="row.status === 2" type="info" size="small">已发货</el-tag>
            <el-tag v-else-if="row.status === 3" type="success" size="small">已完成</el-tag>
            <el-tag v-else type="danger" size="small">已取消</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipDialogVisible" title="订单发货" width="400px">
      <el-form :model="shipForm" label-width="80px">
        <el-form-item label="物流公司">
          <el-input v-model="shipForm.logisticsCompany" placeholder="如：顺丰、中通" />
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { get, put } from '../utils/request'

const orders = ref<any[]>([])
const loading = ref(false)
const activeStatus = ref<number | null>(null)
const shipDialogVisible = ref(false)
const shipForm = ref({
  orderNo: '',
  logisticsCompany: '',
  trackingNo: ''
})

const loadOrders = async () => {
  loading.value = true
  try {
    const url = activeStatus.value !== null 
      ? `/api/admin/order/list?status=${activeStatus.value}` 
      : '/api/admin/order/list'
    
    const data = await get<any>(url)
    if (data.success) {
      orders.value = data.data || []
    }
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

const handleShip = (row: any) => {
  shipForm.value = { orderNo: row.orderNo, logisticsCompany: '', trackingNo: '' }
  shipDialogVisible.value = true
}

const confirmShip = async () => {
  try {
    const data = await put<any>(`/api/order/${shipForm.value.orderNo}/ship`, {
      logisticsCompany: shipForm.value.logisticsCompany,
      trackingNo: shipForm.value.trackingNo
    })
    if (data.success) {
      ElMessage.success('发货成功')
      shipDialogVisible.value = false
      loadOrders()
    } else {
      ElMessage.error(data.message || '发货失败')
    }
  } catch (error) {
    ElMessage.error('请求异常')
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'warning', 2: 'info', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.order-list {
  padding: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-item {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}
</style>
