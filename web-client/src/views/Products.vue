<template>
  <div class="products">
    <h2>全部商品</h2>
    
    <!-- 分类筛选 -->
    <div class="category-filter">
      <span class="filter-label">分类：</span>
      <el-radio-group v-model="selectedCategoryId" @change="handleCategoryChange">
        <el-radio-button :value="0">全部</el-radio-button>
        <el-radio-button 
          v-for="category in categories" 
          :key="category.id" 
          :value="category.id"
        >
          {{ category.name }}
        </el-radio-button>
      </el-radio-group>
    </div>
    
    <!-- 搜索和筛选 -->
    <div class="filters">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称、描述"
        prefix-icon="Search"
        style="width: 300px"
        @keyup.enter="loadProducts"
        clearable
        @clear="() => { keyword = ''; loadProducts() }"
      />
      <el-button type="primary" @click="loadProducts">搜索</el-button>
      <span class="search-hint" v-if="keyword">搜索 "{{ keyword }}"</span>
      
      <!-- 排序选项 -->
      <el-radio-group v-model="sortBy" @change="loadProducts" class="sort-group">
        <el-radio-button value="default">综合</el-radio-button>
        <el-radio-button value="sales">销量</el-radio-button>
        <el-radio-button value="price-asc">价格 ↑</el-radio-button>
        <el-radio-button value="price-desc">价格 ↓</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 骨架屏 -->
    <el-row :gutter="20" v-if="loading">
      <el-col :span="6" v-for="i in 8" :key="i">
        <el-card shadow="hover" class="product-card">
          <el-skeleton :rows="4" animated />
        </el-card>
      </el-col>
    </el-row>

    <!-- 空状态 -->
    <el-empty v-else-if="products.length === 0" description="未找到相关商品">
      <template #image>
        <el-icon :size="80" color="#c0c4cc"><Search /></el-icon>
      </template>
      <template #description>
        <p class="empty-text">
          <span v-if="keyword">未找到 "{{ keyword }}" 相关商品</span>
          <span v-else>暂无商品</span>
        </p>
        <p class="empty-hint">换个关键词试试？</p>
      </template>
      <el-button type="primary" @click="keyword = ''; loadProducts()">查看全部商品</el-button>
    </el-empty>

    <!-- 商品列表 -->
    <el-row :gutter="20" v-else>
      <el-col :span="6" v-for="product in products" :key="product.id">
        <el-card shadow="hover" class="product-card" @click="goToProduct(product.id)">
          <div class="product-image-wrapper">
            <img :src="getFullImageUrl(product.image)" class="product-image" />
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

    <el-pagination
      v-model:current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; justify-content: center"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { get } from '../utils/request'

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
  categoryId?: number
  [key: string]: any
}

interface Category {
  id: number
  name: string
  parentId: number
}

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const products = ref<Product[]>([])
const categories = ref<Category[]>([])
const selectedCategoryId = ref<number>(0)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(true)
const sortBy = ref('default')  // 排序方式：default/sales/price-asc/price-desc

// 监听路由参数变化（支持从导航栏搜索跳转）
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword) {
    keyword.value = newKeyword as string
    pageNum.value = 1
    loadProducts()
  }
})

// 监听分页变化
watch(pageNum, () => {
  loadProducts()
})

// 获取完整图片URL
const getFullImageUrl = (path: string) => {
  if (!path) return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
  if (path.startsWith('/')) return path
  if (path.startsWith('http')) return path
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
}

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

const loadProducts = async () => {
  loading.value = true
  try {
    let sortField = ''
    let sortOrder = ''
    
    // 解析排序参数
    switch (sortBy.value) {
      case 'sales':
        sortField = 'sales'
        sortOrder = 'desc'
        break
      case 'price-asc':
        sortField = 'price'
        sortOrder = 'asc'
        break
      case 'price-desc':
        sortField = 'price'
        sortOrder = 'desc'
        break
      default:
        sortField = 'create_time'
        sortOrder = 'desc'
    }
    
    const categoryIdParam = selectedCategoryId.value > 0 ? selectedCategoryId.value : ''
    const data = await get<any>(
      `/api/product/list?pageNum=${pageNum.value}&pageSize=${pageSize.value}&categoryId=${categoryIdParam}&keyword=${keyword.value}&sortField=${sortField}&sortOrder=${sortOrder}`
    )
    console.log('📥 商品列表接口返回:', data)
    // 后端返回: {success: true, data: {records: [...], total: xxx}}
    if (data.success && data.data) {
      products.value = data.data.records || []
      total.value = data.data.total || 0
      console.log('✅ 加载商品数量:', products.value.length)
      if (products.value.length > 0) {
        console.log('📥 第一个商品示例:', products.value[0])
      }
    }
  } catch (error: any) {
    console.error('加载商品失败', error)
  } finally {
    loading.value = false
  }
}

const addToCart = (product: any) => {
  cartStore.addToCart(product, 1)
  ElMessage.success('已加入购物车')
}

const goToProduct = (id: number) => {
  router.push(`/product/${id}`)
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const data = await get<any>('/api/category/list')
    if (data.success) {
      // 只显示一级分类（parentId为0的）
      categories.value = data.data.filter((cat: Category) => cat.parentId === 0)
    }
  } catch (error) {
    console.error('加载分类列表失败', error)
  }
}

// 分类切换
const handleCategoryChange = () => {
  pageNum.value = 1
  loadProducts()
}

onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<style scoped>
.products {
  max-width: 1400px;
  margin: 0 auto;
}

.category-filter {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  background: #fff;
  border-radius: 4px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.filters {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.sort-group {
  margin-left: auto;
}

.search-hint {
  color: #909399;
  font-size: 14px;
}

.empty-text {
  color: #606266;
  font-size: 14px;
  margin: 10px 0 5px 0;
}

.empty-hint {
  color: #909399;
  font-size: 12px;
  margin: 0;
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
  margin-bottom: 10px;
}

.rating {
  color: #f59e0b;
  font-size: 13px;
  font-weight: 500;
}

.price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}
</style>
