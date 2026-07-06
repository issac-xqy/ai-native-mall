<template>
  <div class="admin-dashboard">
    <!-- 总览卡片 -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="4">
        <el-tooltip content="系统中所有商品的数量（包含草稿、已上架、已下架）" placement="bottom">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <div class="stat-value">{{ overview.totalProducts }}</div>
                <div class="stat-label">商品总数</div>
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>

      <el-col :span="4">
        <el-tooltip content="当前正在销售中的商品数量（用户可见）" placement="bottom">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <div class="stat-value">{{ overview.publishedProducts }}</div>
                <div class="stat-label">已上架</div>
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>

      <el-col :span="4">
        <el-tooltip content="未发布的商品数量（草稿 + 已下架）" placement="bottom">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <div class="stat-value">{{ overview.unpublishedProducts }}</div>
                <div class="stat-label">未上架</div>
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>

      <el-col :span="4">
        <el-tooltip content="所有商品被浏览的总次数（列表页展示即计数）" placement="bottom">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <div class="stat-value">{{ formatNumber(overview.totalViews) }}</div>
                <div class="stat-label">总浏览量</div>
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>

      <el-col :span="4">
        <el-tooltip content="所有商品的销售总量（已成交订单的商品数量）" placement="bottom">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <div class="stat-value">{{ formatNumber(overview.totalSales) }}</div>
                <div class="stat-label">总销量</div>
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>

      <el-col :span="4">
        <el-tooltip content="已完成情感分析的商品占比（有用户评论并分析的商品 / 已上架商品）" placement="bottom">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <div class="stat-value">{{ overview.aiCoverage }}%</div>
                <div class="stat-label">分析覆盖率</div>
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <!-- 第二行：发布状态、浏览量TOP、销量TOP -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <el-card class="equal-height-card">
          <template #header>
            <span class="card-title">
              发布状态分布
              <el-tooltip content="展示商品在不同发布状态（草稿/已上架/已下架）的分布情况" placement="top">
                <el-icon style="margin-left: 5px; color: #909399; cursor: help;"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <div class="status-chart">
            <div class="status-item">
              <div class="status-label">草稿</div>
              <el-progress :percentage="getPercentage(publishStatus.draft)" color="#E6A23C" />
              <div class="status-value">{{ publishStatus.draft }}</div>
            </div>
            <div class="status-item">
              <div class="status-label">已上架</div>
              <el-progress :percentage="getPercentage(publishStatus.published)" color="#67C23A" />
              <div class="status-value">{{ publishStatus.published }}</div>
            </div>
            <div class="status-item">
              <div class="status-label">已下架</div>
              <el-progress :percentage="getPercentage(publishStatus.unpublished)" color="#909399" />
              <div class="status-value">{{ publishStatus.unpublished }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 浏览量 TOP10 -->
      <el-col :span="8">
        <el-card class="equal-height-card">
          <template #header>
            <span class="card-title">
              浏览量 TOP 10
              <el-tooltip content="被浏览次数最多的10个商品（用户在列表页看到即计数）" placement="top">
                <el-icon style="margin-left: 5px; color: #909399; cursor: help;"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <div class="top-list">
            <div v-for="(item, index) in topViews" :key="item.id" class="top-item">
              <div class="rank" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
              <el-image :src="item.image" class="top-image" fit="cover" />
              <div class="top-info">
                <div class="top-name">{{ item.name }}</div>
                <div class="top-stats">
                  <span class="stat">{{ item.view_count }}</span>
                  <span class="stat">{{ item.click_count }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 销量 TOP10 -->
      <el-col :span="8">
        <el-card class="equal-height-card">
          <template #header>
            <span class="card-title">
              销量 TOP 10
              <el-tooltip content="销售量最高的10个商品（按成交订单统计）" placement="top">
                <el-icon style="margin-left: 5px; color: #909399; cursor: help;"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <div class="top-list">
            <div v-for="(item, index) in topSales" :key="item.id" class="top-item">
              <div class="rank" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
              <el-image :src="item.image" class="top-image" fit="cover" />
              <div class="top-info">
                <div class="top-name">{{ item.name }}</div>
                <div class="top-stats">
                  <span class="stat">¥{{ item.price }}</span>
                  <span class="stat">{{ item.sales }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：订单统计 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span class="card-title">
              订单统计
              <el-tooltip content="展示今日订单数、历史总订单数及待处理订单（待支付+已支付）" placement="top">
                <el-icon style="margin-left: 5px; color: #909399; cursor: help;"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-row :gutter="20" class="order-stats-row">
            <el-col :span="8">
              <div class="order-stat-item">
                <div class="order-stat-value">{{ overview.todayOrders }}</div>
                <div class="order-stat-label">今日订单</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="order-stat-item">
                <div class="order-stat-value">{{ overview.totalOrders }}</div>
                <div class="order-stat-label">总订单数</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="order-stat-item">
                <div class="order-stat-value" style="color: #F56C6C">{{ overview.pendingOrders }}</div>
                <div class="order-stat-label">待处理订单</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'
import { get } from '../utils/request'

const overview = reactive({
  totalProducts: 0,
  publishedProducts: 0,
  unpublishedProducts: 0,
  totalViews: 0,
  totalSales: 0,
  todayOrders: 0,
  totalOrders: 0,
  pendingOrders: 0,
  aiCoverage: 0
})

const publishStatus = reactive({
  draft: 0,
  published: 0,
  unpublished: 0
})

const topViews = ref<any[]>([])
const topSales = ref<any[]>([])

// 加载总览数据
const loadOverview = async () => {
  try {
    const data = await get<any>('/api/admin/statistics/overview')
    if (data.success) {
      Object.assign(overview, data.data)
    }
  } catch (error) {
    console.error('加载总览数据失败', error)
  }
}

// 加载发布状态统计
const loadPublishStatus = async () => {
  try {
    const data = await get<any>('/api/admin/statistics/publish-status')
    if (data.success) {
      Object.assign(publishStatus, data.data)
    }
  } catch (error) {
    console.error('加载发布状态失败', error)
  }
}

// 加载浏览量 TOP
const loadTopViews = async () => {
  try {
    const data = await get<any>('/api/admin/statistics/top-views?limit=10')
    if (data.success) {
      topViews.value = data.data
    }
  } catch (error) {
    console.error('加载浏览量排行失败', error)
  }
}

// 加载销量 TOP
const loadTopSales = async () => {
  try {
    const data = await get<any>('/api/admin/statistics/top-sales?limit=10')
    if (data.success) {
      topSales.value = data.data
    }
  } catch (error) {
    console.error('加载销量排行失败', error)
  }
}

// 计算百分比
const getPercentage = (value: number) => {
  const total = overview.totalProducts
  if (!total || total === 0) return 0
  return Math.round((value / total) * 100)
}

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

onMounted(() => {
  loadOverview()
  loadPublishStatus()
  loadTopViews()
  loadTopSales()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

/* 统一卡片高度 */
.equal-height-card :deep(.el-card__body) {
  min-height: 350px;
}

.overview-cards {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
  height: 100px;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.order-stats-row {
  padding: 10px 0;
}

.order-stat-item {
  text-align: center;
  padding: 25px 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  transition: all 0.3s;
}

.order-stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.order-stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1;
}

.order-stat-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.ai-stats {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ai-stat-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.ai-stat-icon {
  font-size: 32px;
}

.ai-stat-content {
  flex: 1;
}

.ai-stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.ai-stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.4;
  text-align: center;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.status-chart {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 350px;
  justify-content: center;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.status-label {
  width: 60px;
  color: #606266;
}

.status-value {
  width: 40px;
  text-align: right;
  font-weight: bold;
  color: #303133;
}

.top-list {
  max-height: 500px;
  overflow-y: auto;
  min-height: 350px;
}

.top-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.top-item:last-child {
  border-bottom: none;
}

.rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
  color: white;
  background: #909399;
}

.rank-1 { background: #FFD700; }
.rank-2 { background: #C0C0C0; }
.rank-3 { background: #CD7F32; }

.top-image {
  width: 50px;
  height: 50px;
  border-radius: 4px;
}

.top-info {
  flex: 1;
  min-width: 0;
}

.top-name {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 5px;
}

.top-stats {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #909399;
}

.stat {
  display: flex;
  align-items: center;
  gap: 3px;
}
</style>
