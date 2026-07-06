<template>
  <div class="comment-analysis">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🤖 智能商品情感分析</span>
          <el-tag type="success" effect="dark">AI自动分析</el-tag>
        </div>
      </template>

      <!-- 智能分析模式 -->
      <el-row :gutter="20">
        <!-- 输入区域 -->
        <el-col :span="8">
          <el-form label-width="100px">
            <el-form-item label="商品名称">
              <el-input
                v-model="productName"
                placeholder="输入商品名称，自动分析评论"
                clearable
                @keyup.enter="smartAnalyze"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="smartAnalyze"
                :loading="smartLoading"
                size="large"
                style="width: 100%"
              >
                <el-icon style="margin-right: 8px"><MagicStick /></el-icon>
                智能分析
              </el-button>
            </el-form-item>
            
            <el-divider>或使用传统模式</el-divider>
            
            <el-form-item label="评论文本">
              <el-input
                v-model="commentText"
                type="textarea"
                :rows="4"
                placeholder="手动输入单条评论进行分析"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                @click="analyzeComment"
                :loading="loading"
                size="small"
              >
                分析单条评论
              </el-button>
            </el-form-item>
          </el-form>
        </el-col>

        <!-- 智能分析结果 -->
        <el-col :span="16">
          <!-- 商品信息卡片 -->
          <el-card v-if="smartResult?.product" class="product-info-card" shadow="never">
            <template #header>
              <div class="product-header">
                <el-avatar :size="50" :src="smartResult?.product?.image" shape="square">
                  <el-icon :size="30"><ShoppingBag /></el-icon>
                </el-avatar>
                <div class="product-meta">
                  <h3>{{ smartResult?.product?.name }}</h3>
                  <p>价格：¥{{ smartResult?.product?.price }} | 评论数：{{ smartResult?.total_comments }}</p>
                </div>
              </div>
            </template>
            
            <!-- 分析报告 -->
            <div v-if="smartResult?.satisfaction_rate !== undefined" class="report-content">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-statistic title="好评率" :value="smartResult?.satisfaction_rate || 0" suffix="%" />
                </el-col>
                <el-col :span="8">
                  <el-statistic title="总评论数" :value="smartResult?.total_comments" />
                </el-col>
                <el-col :span="8">
                  <el-statistic title="分析状态" :value="smartResult?.error ? '异常' : '完成'" />
                </el-col>
              </el-row>
              
              <el-alert v-if="smartResult?.error" :title="'分析异常'" :description="smartResult.error" type="error" :closable="false" show-icon style="margin-bottom: 20px" />
              
              <el-divider>好评标签 TOP 5</el-divider>
              <div class="tags">
                <el-tag
                  v-for="tag in (smartResult?.positive_tags || [])"
                  :key="tag"
                  type="success"
                  effect="light"
                  class="tag-item"
                >
                  👍 {{ tag }}
                </el-tag>
                <el-empty v-if="!smartResult?.positive_tags?.length" description="暂无好评标签" :image-size="60" />
              </div>
              
              <el-divider>差评标签 TOP 5</el-divider>
              <div class="tags">
                <el-tag
                  v-for="tag in (smartResult?.negative_tags || [])"
                  :key="tag"
                  type="danger"
                  effect="light"
                  class="tag-item"
                >
                  👎 {{ tag }}
                </el-tag>
                <el-empty v-if="!smartResult?.negative_tags?.length" description="暂无差评标签" :image-size="60" />
              </div>
              
              <el-divider>改进建议</el-divider>
              <el-alert
                v-for="(suggestion, index) in (smartResult?.improvement_suggestions || [])"
                :key="index"
                :title="`建议 ${index + 1}`"
                :description="suggestion"
                type="warning"
                :closable="false"
                show-icon
                style="margin-bottom: 10px"
              />
              <el-empty v-if="!smartResult?.improvement_suggestions?.length" description="暂无改进建议" :image-size="60" />
            </div>
            
            <!-- 无评论提示 -->
            <el-empty v-else-if="smartResult?.total_comments === 0" :description="smartResult?.message || '暂无评论'" :image-size="100">
              <template #image>
                <el-icon :size="80" color="#409EFF"><ChatDotRound /></el-icon>
              </template>
            </el-empty>
          </el-card>
          
          <!-- 单条评论分析结果 -->
          <div v-else-if="result" class="result-container">
            <el-alert
              :title="`情感倾向: ${sentimentText}`"
              :type="sentimentType"
              :closable="false"
              show-icon
            >
              <div class="result-content">
                <p><strong>分析总结：</strong>{{ result.summary }}</p>
              </div>
            </el-alert>

            <el-divider>提取标签</el-divider>

            <div class="tags">
              <el-tag
                v-for="tag in result.tags"
                :key="tag"
                :type="getTagType(tag)"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>

          <el-empty v-else description="输入商品名称后点击智能分析" :image-size="120">
            <template #image>
              <el-icon :size="100" color="#909399"><DataAnalysis /></el-icon>
            </template>
          </el-empty>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, MagicStick, ShoppingBag, ChatDotRound, DataAnalysis } from '@element-plus/icons-vue'
import { get, post } from '../utils/request'

const commentText = ref('')
const loading = ref(false)
const result = ref<any>(null)

const batchComments = ref('')
const reportLoading = ref(false)
const reportResult = ref('')

// 智能分析相关
const productName = ref('')
const smartLoading = ref(false)
const smartResult = ref<any>(null)

const sentimentText = computed(() => {
  if (!result.value) return ''
  const sentiment = result.value.sentiment
  return sentiment === 'positive' ? '正面' : sentiment === 'negative' ? '负面' : '中性'
})

const sentimentType = computed(() => {
  if (!result.value) return 'info'
  const sentiment = result.value.sentiment
  return sentiment === 'positive' ? 'success' : sentiment === 'negative' ? 'error' : 'warning'
})

const analyzeComment = async () => {
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论文本')
    return
  }

  loading.value = true
  try {
    const data = await post<any>('/api/ai/comment/analyze', { commentText: commentText.value })
    if (data.success) {
      result.value = data.analysis
      ElMessage.success('分析完成')
    }
  } catch (error) {
    ElMessage.error('分析失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 智能商品情感分析
const smartAnalyze = async () => {
  if (!productName.value.trim()) {
    ElMessage.warning('请输入商品名称')
    return
  }

  smartLoading.value = true
  smartResult.value = null
  result.value = null
  
  try {
    const data = await get<any>(`/api/product/analyze-by-name?productName=${encodeURIComponent(productName.value)}`)
    
    if (data.success) {
      smartResult.value = data.data
      if (smartResult.value.total_comments === 0) {
        ElMessage.info(smartResult.value.message || '该商品暂无评论')
      } else {
        ElMessage.success('分析完成')
      }
    } else {
      ElMessage.error(data.message || '分析失败')
    }
  } catch (error) {
    ElMessage.error('分析失败，请稍后重试')
  } finally {
    smartLoading.value = false
  }
}

// 删除旧的正则提取函数，后端已直接返回结构化数据

const getTagType = (tag: string) => {
  const positive = ['好', '快', '高', '精美', '满意', '喜欢', '棒', '不错', '优秀', '赞']
  const negative = ['差', '慢', '低', '失望', '不满', '糟糕', '坑', '烂', '故障', '问题']
  
  // 根据关键词判断标签类型
  if (positive.some(p => tag.includes(p))) return 'success'
  if (negative.some(n => tag.includes(n))) return 'danger'
  return 'info'
}

</script>

<style scoped>
.comment-analysis {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.product-meta h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 18px;
}

.product-meta p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.report-content {
  margin-top: 20px;
}

.result-container {
  padding: 20px;
}

.result-content {
  margin-top: 10px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.tag-item {
  font-size: 14px;
  padding: 8px 16px;
}

.report-card {
  margin-top: 20px;
}
</style>
