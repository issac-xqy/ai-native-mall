<template>
  <div class="ai-monitor">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🤖 AI 智能客服监控看板</span>
          <el-button type="primary" @click="loadStats" :loading="loading">刷新数据</el-button>
        </div>
      </template>

      <!-- 核心指标卡片 -->
      <el-row :gutter="20" class="metrics-row">
        <el-col :span="6">
          <el-card class="metric-card metric-blue">
            <div class="metric-content">
              <el-icon class="metric-icon"><ChatDotRound /></el-icon>
              <div class="metric-info">
                <div class="metric-value">{{ aiStats.todayConversations }}</div>
                <div class="metric-label">今日会话数</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="metric-card metric-green">
            <div class="metric-content">
              <el-icon class="metric-icon"><TrendCharts /></el-icon>
              <div class="metric-info">
                <div class="metric-value">{{ aiStats.avgConversations7Days }}</div>
                <div class="metric-label">近7天平均会话</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="metric-card metric-orange">
            <div class="metric-content">
              <el-icon class="metric-icon"><Timer /></el-icon>
              <div class="metric-info">
                <div class="metric-value">{{ aiStats.avgResponseTime }}ms</div>
                <div class="metric-label">平均响应时间</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="metric-card metric-red">
            <div class="metric-content">
              <el-icon class="metric-icon"><Warning /></el-icon>
              <div class="metric-info">
                <div class="metric-value">{{ aiStats.errorRate }}%</div>
                <div class="metric-label">错误率</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 响应时间分布 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span class="card-title">
                ⏱️ 响应时间分布
                <el-tooltip content="绿色：<10s | 橙色：10-20s | 红色：>20s" placement="top">
                  <el-icon style="margin-left: 5px; color: #909399; cursor: help;"><QuestionFilled /></el-icon>
                </el-tooltip>
              </span>
            </template>
            <div class="response-time-bars">
              <div class="bar-item">
                <div class="bar-label">🟢 优秀（<10s）</div>
                <div class="bar-wrapper">
                  <el-progress 
                    :percentage="getResponseTimePercent('green')" 
                    :color="'#67C23A'" 
                    :stroke-width="20"
                  />
                </div>
                <div class="bar-count">{{ aiStats.responseTimeGreen }} 次</div>
              </div>
              <div class="bar-item">
                <div class="bar-label">🟡 一般（10-20s）</div>
                <div class="bar-wrapper">
                  <el-progress 
                    :percentage="getResponseTimePercent('orange')" 
                    :color="'#E6A23C'" 
                    :stroke-width="20"
                  />
                </div>
                <div class="bar-count">{{ aiStats.responseTimeOrange }} 次</div>
              </div>
              <div class="bar-item">
                <div class="bar-label">缓慢（>20s）</div>
                <div class="bar-wrapper">
                  <el-progress 
                    :percentage="getResponseTimePercent('red')" 
                    :color="'#F56C6C'" 
                    :stroke-width="20"
                  />
                </div>
                <div class="bar-count">{{ aiStats.responseTimeRed }} 次</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card>
            <template #header>
              <span class="card-title">24小时会话趋势</span>
            </template>
            <div ref="trendChartRef" style="height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 错误率与无知识率 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span class="card-title">
                ⚠️ 错误率分析
                <el-tooltip content="近7天AI调用失败的比例" placement="top">
                  <el-icon style="margin-left: 5px; color: #909399; cursor: help;"><QuestionFilled /></el-icon>
                </el-tooltip>
              </span>
            </template>
            <div class="error-rate-display">
              <div class="error-rate-circle" :style="{ borderColor: getErrorRateColor() }">
                <div class="error-rate-value" :style="{ color: getErrorRateColor() }">
                  {{ aiStats.errorRate }}%
                </div>
              </div>
              <div class="error-rate-desc">
                <p>近7天AI服务调用错误率</p>
                <p v-if="aiStats.errorRate < 5" style="color: #67C23A;">✅ 错误率在正常范围内</p>
                <p v-else style="color: #F56C6C;">⚠️ 错误率偏高，请检查AI服务状态</p>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card>
            <template #header>
              <span class="card-title">
                无知识率
                <el-tooltip content="AI检索不到相关知识库内容的比例（需补充知识库）" placement="top">
                  <el-icon style="margin-left: 5px; color: #909399; cursor: help;"><QuestionFilled /></el-icon>
                </el-tooltip>
              </span>
            </template>
            <div class="error-rate-display">
              <div class="error-rate-circle" :style="{ borderColor: getMissingRateColor() }">
                <div class="error-rate-value" :style="{ color: getMissingRateColor() }">
                  {{ aiStats.missingKnowledgeRate }}%
                </div>
              </div>
              <div class="error-rate-desc">
                <p>近7天AI无法回答的问题占比</p>
                <p v-if="aiStats.missingKnowledgeRate < 20" style="color: #67C23A;">✅ 知识库覆盖率良好</p>
                <p v-else style="color: #E6A23C;">建议补充商品知识或售后政策</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ChatDotRound, TrendCharts, Timer, Warning, QuestionFilled } from '@element-plus/icons-vue'
import { get } from '../utils/request'
import * as echarts from 'echarts'

const loading = ref(false)
const trendChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null

const aiStats = reactive({
  todayConversations: 0,
  avgConversations7Days: 0,
  avgResponseTime: 0,
  responseTimeGreen: 0,
  responseTimeOrange: 0,
  responseTimeRed: 0,
  errorRate: 0,
  missingKnowledgeRate: 0,
  hourlyTrend: [] as any[]
})

// 加载AI监控数据
const loadStats = async () => {
  loading.value = true
  try {
    const data = await get<any>('/api/admin/statistics/ai-monitor')
    if (data.success) {
      Object.assign(aiStats, data.data)
      await nextTick()
      renderTrendChart()
    }
  } catch (error) {
    console.error('加载AI监控数据失败', error)
  } finally {
    loading.value = false
  }
}

// 计算响应时间百分比
const getResponseTimePercent = (level: string) => {
  const total = aiStats.responseTimeGreen + aiStats.responseTimeOrange + aiStats.responseTimeRed
  if (total === 0) return 0
  
  const count = level === 'green' ? aiStats.responseTimeGreen :
                level === 'orange' ? aiStats.responseTimeOrange :
                aiStats.responseTimeRed
  
  return Math.round((count / total) * 100)
}

// 获取错误率颜色
const getErrorRateColor = () => {
  if (aiStats.errorRate < 5) return '#67C23A'
  if (aiStats.errorRate < 10) return '#E6A23C'
  return '#F56C6C'
}

// 获取无知识率颜色
const getMissingRateColor = () => {
  if (aiStats.missingKnowledgeRate < 20) return '#67C23A'
  if (aiStats.missingKnowledgeRate < 40) return '#E6A23C'
  return '#F56C6C'
}

// 渲染24小时趋势图
const renderTrendChart = () => {
  if (!trendChartRef.value) return
  
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  // 准备数据：确保24小时完整
  const hours = Array.from({ length: 24 }, (_, i) => i)
  const counts = hours.map(h => {
    const item = aiStats.hourlyTrend.find((t: any) => t.hour === h)
    return item ? item.count : 0
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}:00 - {c} 次会话'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hours.map(h => `${h}:00`),
      axisLabel: {
        interval: 2
      }
    },
    yAxis: {
      type: 'value',
      name: '会话数',
      minInterval: 1
    },
    series: [{
      data: counts,
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      },
      lineStyle: {
        width: 3,
        color: '#409EFF'
      },
      itemStyle: {
        color: '#409EFF'
      }
    }]
  }

  trendChart.setOption(option)
}

onMounted(() => {
  loadStats()
  
  // 窗口大小变化时重绘图表
  window.addEventListener('resize', () => {
    trendChart?.resize()
  })
})
</script>

<style scoped>
.ai-monitor {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metrics-row {
  margin-bottom: 20px;
}

.metrics-row :deep(.el-card__body) {
  padding: 20px;
}

.metric-card {
  cursor: pointer;
  transition: all 0.3s;
  min-height: 100px;
  display: flex;
  align-items: center;
}

.metric-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.metric-content {
  display: flex;
  align-items: center;
  gap: 15px;
  width: 100%;
}

.metric-icon {
  font-size: 40px;
  flex-shrink: 0;
}

.metric-blue .metric-icon { color: #409EFF; }
.metric-green .metric-icon { color: #67C23A; }
.metric-orange .metric-icon { color: #E6A23C; }
.metric-red .metric-icon { color: #F56C6C; }

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.4;
}

.response-time-bars {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bar-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.bar-label {
  width: 120px;
  font-size: 14px;
  color: #606266;
  flex-shrink: 0;
}

.bar-wrapper {
  flex: 1;
}

.bar-count {
  width: 80px;
  text-align: right;
  font-size: 14px;
  color: #909399;
  flex-shrink: 0;
}

.error-rate-display {
  display: flex;
  align-items: center;
  gap: 30px;
  padding: 20px 0;
}

.error-rate-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 8px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.error-rate-value {
  font-size: 32px;
  font-weight: bold;
}

.error-rate-desc {
  flex: 1;
}

.error-rate-desc p {
  margin: 8px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
</style>
