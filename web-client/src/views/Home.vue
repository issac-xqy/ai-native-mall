<template>
  <div class="home">
    <div class="hero">
      <div class="hero-title">探索好物，发现惊喜</div>
      <div class="hero-sub">AI 智能推荐 · 品质保障 · 极速退款</div>
    </div>
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
import { get } from '../utils/request'
import type { Product } from '../types/api'

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
    const data = await get<{ success: boolean; data: Product[] }>('/api/product/top-sales?limit=10')
    if (data.success && data.data) {
      hotProducts.value = data.data
      topSalesProducts.value = data.data
    }
  } catch (error) {
    console.error('加载商品失败', error)
  }
}

const loadRankings = async () => {
  try {
    const ratedData = await get<{ success: boolean; data: Product[] }>('/api/product/top-rated?limit=10')
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
.home { max-width: 1400px; margin: 0 auto; }

/* Hero banner */
.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px; padding: 48px 40px; margin-bottom: 32px;
  position: relative; overflow: hidden;
}
.hero::after {
  content: ''; position: absolute; right: -40px; top: -40px;
  width: 240px; height: 240px; background: rgba(255,255,255,0.08); border-radius: 50%;
}
.hero-title { font-size: 32px; font-weight: 800; color: #fff; margin-bottom: 8px; position: relative; z-index: 1; }
.hero-sub { font-size: 16px; color: rgba(255,255,255,0.8); position: relative; z-index: 1; }

.section { margin-bottom: 40px; }
.section h2 {
  font-size: 22px; font-weight: 700; margin-bottom: 20px; color: #2D3436;
  padding-left: 16px; border-left: 4px solid var(--brand-primary);
}

/* Product cards */
.product-card {
  cursor: pointer; border-radius: 12px; overflow: hidden;
  transition: all 0.3s ease; margin-bottom: 20px;
  border: 1px solid #f0f0f0; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.product-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 40px rgba(108,92,231,0.12);
  border-color: var(--brand-primary);
}
.product-image { width: 100%; height: 200px; object-fit: cover; transition: transform 0.4s; }
.product-card:hover .product-image { transform: scale(1.05); }
.product-image-wrapper { position: relative; overflow: hidden; }
.product-info { padding: 14px; }
.product-info h3 { font-size: 15px; margin: 0 0 8px; color: #2D3436; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.price-row { display: flex; align-items: baseline; gap: 8px; margin-bottom: 6px; }
.price { font-size: 20px; color: #e74c3c; font-weight: 800; }
.original-price { color: #b2bec3; font-size: 12px; text-decoration: line-through; }
.meta-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 12px; color: #636e72; }

/* Rankings */
.ranking-section { margin-bottom: 40px; }
.ranking-card { border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.ranking-title { font-size: 18px; font-weight: 700; }
.ranking-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #f1f2f6; cursor: pointer; transition: all 0.2s; }
.ranking-item:last-child { border-bottom: none; }
.ranking-item:hover { background: #f8f9ff; padding-left: 8px; border-radius: 8px; }
.rank-badge { width: 30px; height: 30px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; color: #fff; margin-right: 12px; flex-shrink: 0; }
.rank-gold { background: linear-gradient(135deg, #f6d365, #fda085); font-size: 16px; }
.rank-silver { background: linear-gradient(135deg, #a8edea, #fed6e3); color: #2d3436; font-size: 16px; }
.rank-bronze { background: linear-gradient(135deg, #d4a574, #a0522d); font-size: 16px; }
.rank-normal { background: #dfe6e9; color: #636e72; }
.ranking-image { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; margin-right: 12px; flex-shrink: 0; }
.ranking-info { flex: 1; min-width: 0; }
.ranking-name { font-size: 14px; font-weight: 600; color: #2D3436; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ranking-price { font-size: 16px; color: #e74c3c; font-weight: 700; }
.ranking-sales, .ranking-rating { font-size: 12px; color: #b2bec3; white-space: nowrap; margin-left: 12px; }
</style>
