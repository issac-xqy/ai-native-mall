<template>
  <div class="product-detail">
    <!-- 骨架屏 Loading -->
    <el-card v-if="loading">
      <el-row :gutter="40">
        <el-col :span="10">
          <el-skeleton :rows="8" animated />
        </el-col>
        <el-col :span="14">
          <el-skeleton :rows="6" animated />
          <div style="margin-top: 30px;">
            <el-skeleton :rows="2" animated />
          </div>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 商品详情 -->
    <el-card v-else-if="product">
      <el-row :gutter="40">
        <el-col :span="10">
          <!-- 主轮播图 -->
          <el-carousel 
            ref="carouselRef"
            :interval="4000" 
            type="card" 
            height="400px" 
            class="product-carousel"
            @change="handleCarouselChange"
          >
            <el-carousel-item v-for="(img, index) in productImages" :key="index">
              <img :src="img" class="carousel-image" loading="lazy" />
            </el-carousel-item>
          </el-carousel>
          
          <!-- 缩略图列表 -->
          <div v-if="productImages.length > 1" class="thumbnail-list">
            <div 
              v-for="(img, index) in productImages" 
              :key="index"
              class="thumbnail-item"
              :class="{ active: currentImageIndex === index }"
              @click="switchToImage(index)"
            >
              <img :src="img" :alt="`商品图片${index + 1}`" loading="lazy" />
            </div>
          </div>
        </el-col>
        <el-col :span="14">
          <h1 class="product-title">{{ product.name }}</h1>
          <div class="price-section">
            <span class="current-price">¥{{ product.price }}</span>
            <span class="original-price" v-if="product.originalPrice">¥{{ product.originalPrice }}</span>
            <el-tag v-if="getDiscount(product) > 0" type="danger" effect="dark" class="discount-tag">
              省 ¥{{ getDiscount(product).toFixed(0) }}
            </el-tag>
          </div>
          
          <div class="product-meta">
            <div class="meta-item">
              <span class="label">销量</span>
              <span class="value">{{ formatSales(product.sales) }}</span>
            </div>
            <div class="meta-item">
              <span class="label">库存</span>
              <span class="value" :class="getStockClass(product.stock)">{{ getStockText(product.stock) }}</span>
            </div>
            <div class="meta-item">
              <span class="label">评分</span>
              <span class="value rating">⭐ {{ getRating(product.sentimentScore) }}</span>
            </div>
          </div>
          
          <div class="quantity">
            <span class="label">数量</span>
            <el-input-number v-model="quantity" :min="1" :max="Math.min(product.stock, 99)" size="large" />
          </div>

          <div class="actions">
            <el-button type="primary" size="large" @click="addToCart" :icon="ShoppingCart">
              加入购物车
            </el-button>
            <el-button type="danger" size="large" @click="buyNow" :icon="CreditCard">
              立即购买
            </el-button>
          </div>
          
          <!-- 售后保障 -->
          <div class="guarantee">
            <el-row :gutter="20">
              <el-col :span="8" class="guarantee-item">
                <el-icon :size="20" color="#67c23a"><CircleCheck /></el-icon>
                <span>7天无理由退换</span>
              </el-col>
              <el-col :span="8" class="guarantee-item">
                <el-icon :size="20" color="#67c23a"><CircleCheck /></el-icon>
                <span>正品保障</span>
              </el-col>
              <el-col :span="8" class="guarantee-item">
                <el-icon :size="20" color="#67c23a"><CircleCheck /></el-icon>
                <span>极速退款</span>
              </el-col>
            </el-row>
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <!-- 商品描述 -->
      <div class="description">
        <h3>📝 商品描述</h3>
        <p>{{ product.description || product.aiDescription || '暂无描述' }}</p>
      </div>
      
      <el-divider />
      
      <!-- 用户评价 -->
      <div class="reviews">
        <div class="reviews-header">
          <h3>💬 用户评价 ({{ reviewTotal }})</h3>
          <el-button type="primary" size="small" @click="showReviewForm = !showReviewForm">
            {{ showReviewForm ? '取消评价' : '我要评价' }}
          </el-button>
        </div>
        
        <!-- 评论表单 -->
        <el-card v-if="showReviewForm" class="review-form-card">
          <el-form label-width="80px">
            <el-form-item label="评分">
              <el-rate v-model="reviewForm.rating" :max="5" allow-half show-text />
            </el-form-item>
            <el-form-item label="评价内容">
              <el-input
                v-model="reviewForm.content"
                type="textarea"
                :rows="4"
                placeholder="分享你的使用体验..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitReview">提交评价</el-button>
              <el-button @click="showReviewForm = false">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 评论列表 -->
        <el-skeleton v-if="reviewLoading" :rows="3" animated style="margin-top: 20px;" />
        
        <el-empty v-else-if="reviews.length === 0" description="暂无评价，快来抢沙发吧~" />
        
        <div v-else class="review-list">
          <div v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-header">
              <el-avatar :size="32" class="review-avatar">{{ getUserName(review.userId) }}</el-avatar>
              <span class="review-user">{{ getReviewUserName(review) }}</span>
              <el-rate v-model="review.rating" disabled :max="5" size="small" />
              <span class="review-date" :title="formatFullTime(review.createTime)">
                {{ formatRelativeTime(review.createTime) }}
              </span>
              <el-button
                v-if="canDeleteReview(review)"
                type="danger"
                size="small"
                link
                @click="handleDeleteReview(review.id)"
                class="delete-btn"
              >
                删除
              </el-button>
            </div>
            <p class="review-content">{{ review.content }}</p>
            <div class="review-tags" v-if="review.sentiment">
              <el-tag :type="review.sentiment === 'positive' ? 'success' : review.sentiment === 'negative' ? 'danger' : 'info'" size="small">
                {{ review.sentiment === 'positive' ? '👍 好评' : review.sentiment === 'negative' ? '👎 差评' : '😐 中评' }}
              </el-tag>
            </div>
          </div>
          
          <!-- 分页器 -->
          <el-pagination
            v-model:current-page="reviewPageNum"
            :page-size="reviewPageSize"
            :total="reviewTotal"
            layout="total, prev, pager, next"
            style="margin-top: 20px; justify-content: center"
            @current-change="handleReviewPageChange"
          />
        </div>
      </div>
    </el-card>
    
    <!-- 商品不存在 -->
    <el-empty v-else description="商品不存在或已下架">
      <el-button type="primary" @click="$router.push('/products')">返回商品列表</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart, CreditCard, CircleCheck } from '@element-plus/icons-vue'
import { get, post, del } from '../utils/request'
import { formatRelativeTime, formatFullTime } from '../utils/timeFormatter'

// 商品数据类型定义
interface Product {
  id: number
  name: string
  price: number
  originalPrice: number
  image: string
  images: string
  description: string
  aiDescription: string
  sales: number
  stock: number
  sentimentScore: number | string
  publishStatus: number
  [key: string]: any
}

// 评论数据类型定义
interface Review {
  id: number
  productId: number
  userId: number
  content: string
  rating: number
  sentiment?: string
  createTime: string
  username?: string
  nickname?: string
  [key: string]: any
}

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

// 初始化默认值，避免类型检查报错
const product = ref<Product>({
  id: 0,
  name: '',
  price: 0,
  originalPrice: 0,
  image: '',
  images: '',
  description: '',
  aiDescription: '',
  sales: 0,
  stock: 0,
  sentimentScore: 0,
  publishStatus: 1
})
const quantity = ref(1)
const loading = ref(true)
const reviews = ref<Review[]>([])
const reviewLoading = ref(false)
const showReviewForm = ref(false)
const currentImageIndex = ref(0)
const carouselRef = ref<any>(null)
const reviewPageNum = ref(1)
const reviewPageSize = ref(10)
const reviewTotal = ref(0)
const reviewForm = ref({
  rating: 5,
  content: ''
})

// 切换图片
const switchToImage = (index: number) => {
  if (carouselRef.value) {
    carouselRef.value.setActiveItem(index)
  }
}

// 轮播图切换事件
const handleCarouselChange = (index: number) => {
  currentImageIndex.value = index
}

// 获取完整图片URL
const getFullImageUrl = (path: string) => {
  if (!path) return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
  if (path.startsWith('/')) return path
  if (path.startsWith('http')) return path
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
}

// 商品图片列表
const productImages = computed(() => {
  if (!product.value) return []
  
  const images: string[] = []
  
  // 解析 images 字段（JSON 数组字符串）
  if (product.value.images) {
    try {
      const parsed = JSON.parse(product.value.images)
      if (Array.isArray(parsed)) {
        images.push(...parsed.map((img: string) => getFullImageUrl(img)))
      }
    } catch (e) {
      console.warn('解析商品多图失败', e)
    }
  }
  
  // 如果没有多图，使用主图
  if (images.length === 0 && product.value.image) {
    images.push(getFullImageUrl(product.value.image))
  }
  
  // 至少返回一张图片
  if (images.length === 0) {
    images.push('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=')
  }
  
  return images
})

// 格式化销量
const formatSales = (sales: number) => {
  if (!sales) return '0'
  if (sales >= 10000) return (sales / 10000).toFixed(1) + '万'
  if (sales >= 1000) return (sales / 1000).toFixed(1) + 'k'
  return sales.toString()
}

// 获取评分
const getRating = (score: any) => {
  if (!score) return '4.5'
  const num = typeof score === 'string' ? parseFloat(score) : score
  return num > 5 ? (num / 5 * 5).toFixed(1) : num.toFixed(1)
}

// 计算折扣
const getDiscount = (product: any) => {
  if (!product.originalPrice || product.originalPrice <= product.price) return 0
  return product.originalPrice - product.price
}

// 库存文案
const getStockText = (stock: number) => {
  if (stock > 100) return '充足'
  if (stock > 10) return `仅剩 ${stock} 件`
  if (stock > 0) return `库存紧张，仅剩 ${stock} 件！`
  return '暂时缺货'
}

// 库存样式类
const getStockClass = (stock: number) => {
  if (stock > 100) return 'stock-normal'
  if (stock > 10) return 'stock-warning'
  if (stock > 0) return 'stock-danger'
  return 'stock-out'
}

const loadProduct = async () => {
  loading.value = true
  try {
    const data = await get<any>(`/api/product/${route.params.id}`)
    product.value = data.data
    loadReviews()
  } catch (error: any) {
    console.error('加载商品详情失败', error)
    if (error.status === 404) {
      ElMessage.error('商品不存在或已下架')
    }
  } finally {
    loading.value = false
  }
}

// 加载评论
const loadReviews = async () => {
  reviewLoading.value = true
  try {
    console.log('📥 开始加载评论，商品ID:', route.params.id, '页码:', reviewPageNum.value)
    const data = await get<any>(`/api/comment/product/${route.params.id}?pageNum=${reviewPageNum.value}&pageSize=${reviewPageSize.value}`)
    console.log('📥 评论接口返回:', data)
    if (data.success) {
      reviews.value = data.data || []
      reviewTotal.value = data.total || 0
      console.log('✅ 评论加载完成，数量:', reviews.value.length, '总数:', reviewTotal.value)
      // 调试：打印第一条评论的userId
      if (reviews.value.length > 0) {
        console.log('🔍 第一条评论数据:', reviews.value[0])
        console.log('🔍 userId类型:', typeof reviews.value[0].userId, '值:', reviews.value[0].userId)
      }
      // 调试：打印当前登录用户信息
      console.log('🔍 当前用户信息:', userStore.userInfo)
      if (userStore.userInfo) {
        console.log('🔍 当前用户ID类型:', typeof userStore.userInfo.id, '值:', userStore.userInfo.id)
      }
    } else {
      console.warn('❌ 评论接口返回 success=false:', data.message)
    }
  } catch (error) {
    console.error('❌ 加载评论异常:', error)
  } finally {
    reviewLoading.value = false
  }
}

// 评论分页切换
const handleReviewPageChange = (page: number) => {
  reviewPageNum.value = page
  loadReviews()
}

// 判断当前用户是否可以删除该评论
const canDeleteReview = (review: any) => {
  // 严格检查登录状态
  if (!userStore.isLoggedIn) {
    console.log('🔒 未登录，不可删除')
    return false
  }
  
  if (!userStore.userInfo || !userStore.userInfo.id) {
    console.log('🔒 用户信息不完整，不可删除')
    return false
  }
  
  if (!review || review.userId === undefined || review.userId === null) {
    console.log('🔒 评论userId不存在，不可删除')
    return false
  }
  
  // 严格类型转换和比较
  const reviewUserId = Number(review.userId)
  const currentUserId = Number(userStore.userInfo.id)
  
  // 检查转换是否成功
  if (isNaN(reviewUserId) || isNaN(currentUserId)) {
    console.log('🔒 userId类型转换失败 - reviewUserId:', reviewUserId, 'currentUserId:', currentUserId)
    return false
  }
  
  const canDelete = reviewUserId === currentUserId
  
  console.log('🔍 权限检查详情:')
  console.log('  - 评论userId (原始):', review.userId, '类型:', typeof review.userId)
  console.log('  - 评论userId (转换后):', reviewUserId)
  console.log('  - 当前用户信息:', userStore.userInfo)
  console.log('  - 当前用户ID (原始):', userStore.userInfo.id, '类型:', typeof userStore.userInfo.id)
  console.log('  - 当前用户ID (转换后):', currentUserId)
  console.log('  - 是否匹配:', canDelete)
  
  return canDelete
}

// 删除评论
const handleDeleteReview = async (reviewId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 检查登录状态
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    
    console.log('🗑️ 开始删除评论，ID:', reviewId)
    console.log('🔑 当前Token:', userStore.token ? '存在' : '不存在')
    
    const data = await del<any>(`/api/comment/${reviewId}`)
    console.log('📥 删除接口返回:', data)
    
    if (data.success) {
      ElMessage.success('删除成功')
      // 如果当前页只有一条评论且不是第一页，则回到上一页
      if (reviews.value.length === 1 && reviewPageNum.value > 1) {
        reviewPageNum.value--
      }
      loadReviews()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('❌ 删除评论失败', error)
      if (error.status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        router.push('/login')
      } else {
        ElMessage.error('删除失败，请稍后重试')
      }
    }
  }
}

// 提交评论
const submitReview = async () => {
  if (!reviewForm.value.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  try {
    await post('/api/comment', {
      productId: product.value.id,
      content: reviewForm.value.content,
      rating: reviewForm.value.rating,
      sentiment: reviewForm.value.rating >= 4 ? 'positive' : reviewForm.value.rating >= 3 ? 'neutral' : 'negative'
    })
    
    ElMessage.success('评论成功')
    reviewForm.value.content = ''
    reviewForm.value.rating = 5
    showReviewForm.value = false
    // 重置到第一页并刷新评论列表
    reviewPageNum.value = 1
    loadReviews()
  } catch (error: any) {
    console.error('提交评论失败', error)
    ElMessage.error('评论失败，请稍后重试')
  }
}

// 获取用户名首字母
const getUserName = (_userId: number) => {
  return 'U'
}

// 获取评论用户的显示名称
const getReviewUserName = (review: Review) => {
  // 如果评论中包含用户信息，优先使用
  if (review.username) {
    return review.username
  }
  if (review.nickname) {
    return review.nickname
  }
  // 否则显示为用户ID
  return `用户${review.userId}`
}

const addToCart = () => {
  if (!product.value || product.value.stock <= 0) {
    ElMessage.warning('商品库存不足')
    return
  }
  cartStore.addToCart(product.value, quantity.value)
  ElMessage.success('已加入购物车')
}

const buyNow = () => {
  if (!product.value || product.value.stock <= 0) {
    ElMessage.warning('商品库存不足')
    return
  }
  router.push({
    path: '/checkout',
    query: {
      fromCart: 'false',
      productId: String(product.value.id),
      productName: product.value.name,
      price: String(product.value.price),
      quantity: String(quantity.value)
    }
  })
}

onMounted(() => {
  loadProduct()
})
</script>

<style scoped>
.product-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.product-carousel {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.thumbnail-list {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  justify-content: center;
}

.thumbnail-item {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
  overflow: hidden;
}

.thumbnail-item:hover {
  transform: scale(1.05);
  border-color: #409EFF;
}

.thumbnail-item.active {
  border-color: #409EFF;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.5);
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.product-image {
  width: 100%;
  border-radius: 8px;
}

.product-title {
  font-size: 28px;
  margin-bottom: 20px;
  color: #303133;
}

.price-section {
  display: flex;
  align-items: baseline;
  gap: 15px;
  margin-bottom: 25px;
}

.current-price {
  color: #f56c6c;
  font-size: 36px;
  font-weight: bold;
}

.original-price {
  color: #909399;
  text-decoration: line-through;
  font-size: 18px;
}

.discount-tag {
  margin-left: 10px;
}

.product-meta {
  display: flex;
  gap: 30px;
  padding: 20px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-item .label {
  color: #909399;
  font-size: 14px;
}

.meta-item .value {
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.meta-item .value.rating {
  color: #f59e0b;
}

.meta-item .value.stock-warning {
  color: #e6a23c;
  font-weight: bold;
}

.meta-item .value.stock-danger {
  color: #f56c6c;
  font-weight: bold;
  animation: blink 1s infinite;
}

.meta-item .value.stock-out {
  color: #909399;
  text-decoration: line-through;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.5; }
}

.quantity {
  margin: 20px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.quantity .label {
  font-size: 14px;
  color: #606266;
}

.actions {
  display: flex;
  gap: 20px;
  margin-top: 30px;
}

.actions .el-button {
  padding: 0 40px;
}

.guarantee {
  margin-top: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.guarantee-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.description {
  margin-top: 20px;
}

.description h3 {
  margin-bottom: 10px;
}

.reviews {
  margin-top: 20px;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.reviews-header h3 {
  margin: 0;
}

.review-form-card {
  margin-bottom: 20px;
}

.review-list {
  margin-top: 20px;
}

.review-item {
  padding: 20px 0;
  border-bottom: 1px solid #ebeef5;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.review-avatar {
  background-color: #409EFF;
  color: white;
}

.review-user {
  font-weight: 500;
  color: #303133;
}

.review-date {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
  cursor: help;
  transition: color 0.3s;
  white-space: nowrap;
}

.review-date:hover {
  color: #409EFF;
}

.delete-btn {
  margin-left: 10px;
  font-size: 12px;
}

.review-content {
  color: #606266;
  line-height: 1.6;
  margin: 0 0 10px 0;
}

.review-tags {
  display: flex;
  gap: 10px;
}
</style>
