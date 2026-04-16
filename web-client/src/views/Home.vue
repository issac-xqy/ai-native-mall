<template>
  <div class="home">
    <el-carousel height="400px" class="banner">
      <el-carousel-item v-for="i in 3" :key="i">
        <div class="banner-item" :style="{ background: `linear-gradient(135deg, #667eea 0%, #764ba2 100%)` }">
          <h2>AI智能推荐</h2>
          <p>根据您的喜好智能推荐商品</p>
        </div>
      </el-carousel-item>
    </el-carousel>

    <div class="section">
      <h2>🔥 热门商品</h2>
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
              <!-- 商品标签 -->
              <div class="product-tags">
                <!-- 未上架显示无货 -->
                <el-tag v-if="product.publishStatus !== 1" type="info" size="small" effect="dark">🚫 无货</el-tag>
                <!-- 已上架显示其他标签 -->
                <template v-else>
                  <el-tag v-if="getRating(product.sentimentScore) >= 4.5" type="danger" size="small" effect="dark">好评如潮</el-tag>
                  <el-tag v-else-if="getRating(product.sentimentScore) >= 4.0" type="success" size="small">好评率高</el-tag>
                  <el-tag v-if="product.sales > 1000" type="warning" size="small" effect="dark">🔥 热销</el-tag>
                </template>
              </div>
            </div>
            <div class="product-info">
              <h3>{{ product.name }}</h3>
              <div class="price-row">
                <span class="price">¥{{ product.price }}</span>
                <span class="original-price" v-if="product.originalPrice && product.originalPrice > product.price">¥{{ product.originalPrice }}</span>
              </div>
              <div class="meta-row">
                <span class="sales">已售 {{ formatSales(product.sales) }}</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

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

const loadProducts = async () => {
  try {
    // 使用专门的销量Top10接口
    const response = await fetch('/api/product/top-sales?limit=10')
    const data = await response.json()
    console.log('📥 首页热销商品接口返回:', data)
    
    if (data.success && data.data) {
      hotProducts.value = data.data
      console.log('✅ 加载热销商品数量:', hotProducts.value.length)
    } else {
      console.error('❌ 商品接口返回失败:', data.message)
    }
  } catch (error) {
    console.error('加载商品失败', error)
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

// 格式化销量
const formatSales = (sales: number) => {
  if (!sales) return '0'
  if (sales >= 10000) return (sales / 10000).toFixed(1) + '万'
  if (sales >= 1000) return (sales / 1000).toFixed(1) + 'k'
  return sales.toString()
}

// 获取评分（5分制）
const getRating = (score: any) => {
  if (!score) return '4.5'
  const num = typeof score === 'string' ? parseFloat(score) : score
  return num > 5 ? (num / 5 * 5).toFixed(1) : num.toFixed(1)
}

// 图片加载失败处理
const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.home {
  max-width: 1400px;
  margin: 0 auto;
}

.banner {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 30px;
}

.banner-item {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
}

.banner-item h2 {
  font-size: 48px;
  margin: 0 0 20px 0;
}

.banner-item p {
  font-size: 24px;
  opacity: 0.9;
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
</style>
