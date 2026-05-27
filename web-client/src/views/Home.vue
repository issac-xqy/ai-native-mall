<template>
  <div class="home">
    <!-- AI智能推荐轮播图 -->
    <AICarousel />

    <div class="section">
      <h2>热门商品</h2>
      <el-row :gutter="20">
        <el-col :span="6" v-for="product in hotProducts" :key="product.id">
          <el-card shadow="hover" class="product-card" @click="goToProduct(product.id)">
            <div class="product-image-wrapper">
              <img 
                :src="getFullImageUrl(product.image)" 
                class="product-image" 
                loading="lazy"
                @error="handleImageError"
              />
            </div>
            <div class="product-info">
              <h3>{{ product.name }}</h3>
              <div class="price-row">
                <span class="price">¥{{ product.price }}</span>
                <span class="original-price" v-if="product.originalPrice && product.originalPrice > product.price">¥{{ product.originalPrice }}</span>
              </div>
              <div class="meta-row">
                <span class="sales">已售 {{ product.sales }}</span>
                <span class="rating">⭐ {{ getRating(product.sentimentScore) }}</span>
              </div>
              <el-button type="primary" size="small" @click.stop="addToCart(product)">
                加入购物车
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 排行榜区域 -->
    <el-row :gutter="20" class="ranking-section">
      <!-- 销量排行榜 -->
      <el-col :span="12">
        <el-card class="ranking-card">
          <template #header>
            <span class="ranking-title">销量排行榜 TOP 10</span>
          </template>
          <div class="ranking-list">
            <div 
              v-for="(product, index) in topSalesProducts" 
              :key="product.id" 
              class="ranking-item"
              @click="goToProduct(product.id)"
            >
              <div class="rank-badge" :class="getRankClass(index)">{{ index + 1 }}</div>
              <img :src="getFullImageUrl(product.image)" class="ranking-image" loading="lazy" @error="handleImageError" />
              <div class="ranking-info">
                <div class="ranking-name">{{ product.name }}</div>
                <div class="ranking-price">¥{{ product.price }}</div>
              </div>
              <div class="ranking-sales">已售 {{ product.sales }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 好评排行榜 -->
      <el-col :span="12">
        <el-card class="ranking-card">
          <template #header>
            <span class="ranking-title">好评排行榜 TOP 10</span>
          </template>
          <div class="ranking-list">
            <div 
              v-for="(product, index) in topRatedProducts" 
              :key="product.id" 
              class="ranking-item"
              @click="goToProduct(product.id)"
            >
              <div class="rank-badge" :class="getRankClass(index)">{{ index + 1 }}</div>
              <img :src="getFullImageUrl(product.image)" class="ranking-image" loading="lazy" @error="handleImageError" />
              <div class="ranking-info">
                <div class="ranking-name">{{ product.name }}</div>
                <div class="ranking-price">¥{{ product.price }}</div>
              </div>
              <div class="ranking-rating">⭐ {{ getRating(product.sentimentScore) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import AICarousel from '../components/AICarousel.vue'

// 商品数据类型定义
interface Product {
  id: number
  name: string
  price: number
  originalPrice: number
  image: string
  images?: string
  description?: string
  sales: number
  stock?: number
  sentimentScore: number | string
  publishStatus?: number
  [key: string]: any
}

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const hotProducts = ref<Product[]>([])
const topSalesProducts = ref<Product[]>([])
const topRatedProducts = ref<Product[]>([])

const loadProducts = async () => {
  try {
    // 加载热销商品
    const response = await fetch('/api/product/top-sales?limit=10')
    const data = await response.json()
    if (data.success && data.data) {
      hotProducts.value = data.data
    }
  } catch (error) {
    console.error('加载商品失败', error)
  }
}

const loadRankings = async () => {
  try {
    // 加载销量排行榜
    const salesRes = await fetch('/api/product/top-sales?limit=10')
    const salesData = await salesRes.json()
    if (salesData.success && salesData.data) {
      topSalesProducts.value = salesData.data
    }
    
    // 加载好评排行榜
    const ratedRes = await fetch('/api/product/top-rated?limit=10')
    const ratedData = await ratedRes.json()
    if (ratedData.success && ratedData.data) {
      topRatedProducts.value = ratedData.data
    }
  } catch (error) {
    console.error('加载排行榜失败', error)
  }
}

// 获取完整图片URL
const getFullImageUrl = (path: string) => {
  if (!path) return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
  if (path.startsWith('/')) return path
  if (path.startsWith('http')) return path
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
}

// 添加到购物车
const addToCart = (product: Product) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login?redirect=/')
    return
  }
  cartStore.addToCart(product, 1)
  ElMessage.success('已加入购物车')
}

const goToProduct = (id: number | string) => {
  console.log('🔗 点击商品，ID:', id, '类型:', typeof id)
  if (!id || String(id) === 'undefined' || id === undefined) {
    console.error('❌ 商品ID无效:', id)
    return
  }
  console.log('✅ 跳转到商品详情页:', `/product/${id}`)
  router.push(`/product/${id}`)
}

// 获取评分（5分制）
const getRating = (score: any) => {
  if (!score) return '4.5'
  const num = typeof score === 'string' ? parseFloat(score) : score
  return num > 5 ? (num / 5 * 5).toFixed(1) : num.toFixed(1)
}

// 获取排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return 'rank-normal'
}

// 图片加载失败处理
const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
}

onMounted(() => {
  loadProducts()
  loadRankings()
})
</script>

<style scoped>
.home {
  max-width: 1400px;
  margin: 0 auto;
}

.section {
  margin-bottom: 40px;
}

.section h2 {
  margin-bottom: 20px;
  color: #303133;
}

.product-card {
  cursor: pointer;
  transition: transform 0.3s;
  margin-bottom: 20px;
}

.product-card:hover {
  transform: translateY(-5px);
}

.product-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.product-info {
  padding: 10px 0;
}

.product-info h3 {
  font-size: 16px;
  margin: 10px 0;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-image-wrapper {
  position: relative;
  overflow: hidden;
}

.product-tags {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 8px 0;
}

.original-price {
  color: #909399;
  font-size: 13px;
  text-decoration: line-through;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 5px;
}

.rating {
  color: #f59e0b;
  font-size: 13px;
  font-weight: 500;
}

/* 排行榜样式 */
.ranking-section {
  margin-bottom: 40px;
}

.ranking-card {
  height: 100%;
}

.ranking-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.ranking-list {
  max-height: 500px;
  overflow-y: auto;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.3s;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-item:hover {
  background-color: #f5f7fa;
}

.rank-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin-right: 12px;
  flex-shrink: 0;
}

.rank-gold {
  background: linear-gradient(135deg, #ffd700, #ffaa00);
}

.rank-silver {
  background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
}

.rank-bronze {
  background: linear-gradient(135deg, #cd7f32, #b87333);
}

.rank-normal {
  background-color: #909399;
}

.ranking-image {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  object-fit: cover;
  margin-right: 12px;
  flex-shrink: 0;
}

.ranking-info {
  flex: 1;
  min-width: 0;
}

.ranking-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-price {
  font-size: 16px;
  color: #f56c6c;
  font-weight: 600;
}

.ranking-sales,
.ranking-rating {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
  margin-left: 12px;
}

.ranking-rating {
  color: #f59e0b;
}
</style>
