<template>
  <div class="ai-carousel-wrapper">
    <!-- AI推荐头部 -->
    <div class="ai-header" v-if="recommendations.length > 0">
      <div class="ai-text">
        <span class="ai-title">AI智能推荐</span>
        <span class="ai-subtitle">{{ getRecommendMessage() }}</span>
      </div>
      <el-button 
        text 
        size="small" 
        class="refresh-btn"
        @click="refreshRecommendations"
        :loading="loading"
      >
        换一批
      </el-button>
    </div>

    <!-- 轮播容器 -->
    <div class="carousel-container">
      <div 
        class="carousel-track" 
        :style="{ transform: `translateX(-${currentIndex * (cardWidth + gap)}px)`, transition: transitioning ? 'transform 0.5s ease' : 'none' }"
        @mouseenter="pauseAutoPlay"
        @mouseleave="resumeAutoPlay"
      >
        <div 
          v-for="(item, index) in displayProducts" 
          :key="item.id + '-' + index"
          class="carousel-card"
          :style="{ width: cardWidth + 'px', marginRight: gap + 'px' }"
          @click="handleCardClick(item)"
        >
          <div class="card-image-wrapper">
            <img 
              :src="getFullImageUrl(item.image)" 
              :alt="item.name"
              class="card-image"
              loading="lazy"
              @error="handleImageError"
            />
            <!-- AI推荐标签 -->
            <div class="ai-badge">
              <span class="badge-text">{{ item.recommend_reason || 'AI推荐' }}</span>
            </div>
            <!-- 置信度指示器 -->
            <div class="confidence-bar" v-if="item.confidence">
              <div 
                class="confidence-fill" 
                :style="{ width: (item.confidence * 100) + '%' }"
              ></div>
            </div>
          </div>
          
          <div class="card-content">
            <h3 class="card-title">{{ item.name }}</h3>
            <div class="card-price">
              <span class="price-current">¥{{ item.price }}</span>
              <span class="price-original" v-if="item.originalPrice && item.originalPrice > item.price">
                ¥{{ item.originalPrice }}
              </span>
            </div>
            <div class="card-meta">
              <span class="meta-sales">已售 {{ item.sales }}</span>
              <span class="meta-rating">{{ item.rating || '4.5' }}</span>
            </div>
            <div class="card-action">
              <el-button type="primary" size="small" @click.stop="addToCart(item)">
                加入购物车
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 导航按钮 -->
    <button 
      class="nav-btn nav-prev" 
      @click="prevSlide"
      :disabled="currentIndex === 0"
    >
      <svg viewBox="0 0 24 24" width="24" height="24">
        <path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
      </svg>
    </button>
    <button 
      class="nav-btn nav-next" 
      @click="nextSlide"
      :disabled="currentIndex >= maxIndex"
    >
      <svg viewBox="0 0 24 24" width="24" height="24">
        <path fill="currentColor" d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
      </svg>
    </button>

    <!-- 指示器 -->
    <div class="carousel-indicators" v-if="maxIndex > 0">
      <span 
        v-for="i in maxIndex + 1" 
        :key="i"
        class="indicator"
        :class="{ active: i - 1 === currentIndex }"
        @click="goToSlide(i - 1)"
      ></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '../stores/cart'
import { useUserStore } from '../stores/user'

interface RecommendProduct {
  id: number
  name: string
  price: number
  originalPrice?: number
  image: string
  sales: number
  rating?: number
  recommend_reason?: string
  confidence?: number
  [key: string]: any
}

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const recommendations = ref<RecommendProduct[]>([])
const loading = ref(false)
const currentIndex = ref(0)
const autoPlayTimer = ref<number | null>(null)
const cardWidth = 280
const gap = 20
const transitioning = ref(false)

// 最大索引
const maxIndex = computed(() => {
  return Math.max(0, recommendations.value.length - 3)
})

// 显示的商品列表（循环展示）
const displayProducts = computed(() => {
  return recommendations.value
})

// 获取推荐消息
const getRecommendMessage = () => {
  if (userStore.isLoggedIn) {
    return '根据您的浏览偏好智能推荐'
  }
  return '今日热门推荐'
}

// 加载推荐数据
const loadRecommendations = async () => {
  loading.value = true
  try {
    const userId = userStore.userInfo?.id
    const params = new URLSearchParams({
      scene: 'home',
      limit: '8'
    })
    
    if (userId) {
      params.append('userId', String(userId))
    }

    const response = await fetch(`/api/ai/recommend/carousel?${params}`)
    const data = await response.json()
    
    if (data.success && data.data) {
      recommendations.value = data.data
      currentIndex.value = 0
    }
  } catch (error) {
    console.error('加载AI推荐失败', error)
    ElMessage.error('加载推荐失败')
  } finally {
    loading.value = false
  }
}

// 刷新推荐
const refreshRecommendations = () => {
  loadRecommendations()
}

// 上一张
const prevSlide = () => {
  if (currentIndex.value > 0) {
    transitioning.value = true
    currentIndex.value--
    setTimeout(() => { transitioning.value = false }, 500)
  }
}

// 下一张
const nextSlide = () => {
  if (currentIndex.value < maxIndex.value) {
    transitioning.value = true
    currentIndex.value++
    setTimeout(() => { transitioning.value = false }, 500)
  }
}

// 跳转到指定索引
const goToSlide = (index: number) => {
  transitioning.value = true
  currentIndex.value = index
  setTimeout(() => { transitioning.value = false }, 500)
}

// 自动播放
const startAutoPlay = () => {
  autoPlayTimer.value = window.setInterval(() => {
    if (currentIndex.value >= maxIndex.value) {
      currentIndex.value = 0
    } else {
      nextSlide()
    }
  }, 5000)
}

const pauseAutoPlay = () => {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value)
    autoPlayTimer.value = null
  }
}

const resumeAutoPlay = () => {
  startAutoPlay()
}

// 图片URL处理
const getFullImageUrl = (path: string) => {
  if (!path) return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
  if (path.startsWith('/')) return path
  if (path.startsWith('http')) return path
  return path
}

// 图片加载失败
const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
}

// 卡片点击
const handleCardClick = (item: RecommendProduct) => {
  router.push(`/product/${item.id}`)
}

// 加入购物车
const addToCart = (product: RecommendProduct) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login?redirect=/')
    return
  }
  cartStore.addToCart(product, 1)
  ElMessage.success('已加入购物车')
}

onMounted(() => {
  loadRecommendations()
  startAutoPlay()
})

onUnmounted(() => {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value)
  }
})
</script>

<style scoped>
.ai-carousel-wrapper {
  position: relative;
  margin-bottom: 30px;
}

/* AI推荐头部 */
.ai-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 0 4px;
}

.ai-icon {
  font-size: 28px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.ai-text {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ai-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.ai-subtitle {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.refresh-btn {
  color: #409EFF;
}

/* 轮播容器 */
.carousel-container {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding: 20px 0;
}

.carousel-track {
  display: flex;
  padding: 0 20px;
}

/* 卡片样式 */
.carousel-card {
  flex-shrink: 0;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.carousel-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.card-image-wrapper {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* AI推荐标签 */
.ai-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(64, 158, 255, 0.9);
  border-radius: 20px;
  color: white;
  font-size: 12px;
  backdrop-filter: blur(4px);
}

.badge-icon {
  font-size: 14px;
}

/* 置信度指示器 */
.confidence-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(0, 0, 0, 0.1);
}

.confidence-fill {
  height: 100%;
  background: linear-gradient(90deg, #67C23A, #409EFF);
  transition: width 0.5s ease;
}

/* 卡片内容 */
.card-content {
  padding: 16px;
}

.card-title {
  font-size: 15px;
  color: #303133;
  margin: 0 0 10px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.price-current {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}

.price-original {
  font-size: 13px;
  color: #909399;
  text-decoration: line-through;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 12px;
  color: #909399;
}

.meta-rating {
  color: #f59e0b;
}

.card-action {
  text-align: center;
}

/* 导航按钮 */
.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.3s;
  z-index: 10;
}

.nav-btn:hover:not(:disabled) {
  background: #409EFF;
  color: white;
}

.nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.nav-prev {
  left: 10px;
}

.nav-next {
  right: 10px;
}

/* 指示器 */
.carousel-indicators {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dcdfe6;
  cursor: pointer;
  transition: all 0.3s;
}

.indicator.active {
  width: 24px;
  border-radius: 4px;
  background: #409EFF;
}
</style>
