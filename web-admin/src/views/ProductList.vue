<template>
  <div class="admin-product-list">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="商品名称" clearable />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="searchForm.publishStatus" placeholder="全部" clearable>
            <el-option label="全部" value="" />
            <el-option label="草稿" :value="0" />
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 批量操作 -->
      <div class="batch-actions" v-if="selectedRows.length > 0">
        <span>已选 {{ selectedRows.length }} 项</span>
        <el-button type="success" size="small" @click="handleBatchPublish">批量上架</el-button>
        <el-button type="warning" size="small" @click="handleBatchUnpublish">批量下架</el-button>
      </div>

      <!-- 商品表格 -->
      <el-table
        :data="products"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        style="margin-top: 20px"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.image"
              style="width: 60px; height: 60px"
              fit="cover"
            />
          </template>
        </el-table-column>

        <el-table-column prop="name" label="商品名称" min-width="200" />

        <el-table-column label="价格" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="stock" label="库存" width="80" />

        <el-table-column label="数据统计" width="200">
          <template #default="{ row }">
            <div class="stats">
              <el-tooltip content="浏览量">
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ row.viewCount || 0 }}
                </span>
              </el-tooltip>
              <el-tooltip content="点击量">
                <span class="stat-item">
                  <el-icon><Pointer /></el-icon>
                  {{ row.clickCount || 0 }}
                </span>
              </el-tooltip>
              <el-tooltip content="销量">
                <span class="stat-item">
                  <el-icon><ShoppingCart /></el-icon>
                  {{ row.sales || 0 }}
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发布状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.publishStatus === 1" type="success">已上架</el-tag>
            <el-tag v-else-if="row.publishStatus === 2" type="info">已下架</el-tag>
            <el-tag v-else type="warning">草稿</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.publishStatus !== 1"
              type="success"
              size="small"
              @click="handlePublish(row)"
            >
              上架
            </el-button>
            <el-button
              v-if="row.publishStatus === 1"
              type="warning"
              size="small"
              @click="handleUnpublish(row)"
            >
              下架
            </el-button>
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Pointer, ShoppingCart } from '@element-plus/icons-vue'
import { get, put, del } from '../utils/request'

const router = useRouter()

const loading = ref(false)
const products = ref<any[]>([])
const selectedRows = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive<{ keyword: string; publishStatus: number | null }>({
  keyword: '',
  publishStatus: null
})

// 监听分页变化
watch([pageNum, pageSize], () => {
  loadProducts()
})

// 加载商品列表
const loadProducts = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      pageNum: pageNum.value.toString(),
      pageSize: pageSize.value.toString()
    })
    
    if (searchForm.keyword) {
      params.append('keyword', searchForm.keyword)
    }
    if (searchForm.publishStatus != null) {
      params.append('publishStatus', searchForm.publishStatus.toString())
    }

    const data = await get<any>(`/api/admin/product/list?${params}`)
    if (data.success) {
      products.value = data.data?.data || data.data?.records || []
      total.value = data.data?.total || 0
    }
  } catch (error) {
    console.error('加载商品列表失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadProducts()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.publishStatus = null
  pageNum.value = 1
  loadProducts()
}

// 选择变化
const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
}

// 上架商品
const handlePublish = async (product: any) => {
  try {
    await ElMessageBox.confirm(`确定要上架商品"${product.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })

    const data = await put<any>(`/api/admin/product/${product.id}/publish`)
    if (data.success) {
      ElMessage.success('上架成功')
      loadProducts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('上架失败')
    }
  }
}

// 下架商品
const handleUnpublish = async (product: any) => {
  try {
    await ElMessageBox.confirm(`确定要下架商品"${product.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const data = await put<any>(`/api/admin/product/${product.id}/unpublish`)
    if (data.success) {
      ElMessage.success('下架成功')
      loadProducts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('下架失败')
    }
  }
}

// 批量上架
const handleBatchPublish = async () => {
  try {
    await ElMessageBox.confirm(`确定要上架选中的 ${selectedRows.value.length} 个商品吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })

    const ids = selectedRows.value.map(p => p.id)
    const data = await put<any>('/api/admin/product/batch/publish', { ids })
    if (data.success) {
      ElMessage.success('批量上架成功')
      loadProducts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('批量上架失败')
    }
  }
}

// 批量下架
const handleBatchUnpublish = async () => {
  try {
    await ElMessageBox.confirm(`确定要下架选中的 ${selectedRows.value.length} 个商品吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const ids = selectedRows.value.map(p => p.id)
    const data = await put<any>('/api/admin/product/batch/unpublish', { ids })
    if (data.success) {
      ElMessage.success('批量下架成功')
      loadProducts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('批量下架失败')
    }
  }
}

// 编辑商品
const handleEdit = (product: any) => {
  router.push(`/product/edit/${product.id}`)
}

// 删除商品
const handleDelete = async (product: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除商品"${product.name}"吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })

    const data = await del<any>(`/api/admin/product/${product.id}`)
    if (data.success) {
      ElMessage.success('删除成功')
      loadProducts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.admin-product-list {
  padding: 20px;
}

.search-form {
  margin-bottom: 0;
}

.batch-actions {
  margin-top: 15px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.stats {
  display: flex;
  gap: 15px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 13px;
}

.stat-item .el-icon {
  color: #409EFF;
}
</style>
