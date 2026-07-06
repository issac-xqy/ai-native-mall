<template>
  <div class="knowledge-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AI知识库管理</span>
          <el-button type="primary" @click="showUploadDialog = true">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="分类">
          <el-select v-model="selectedCategory" placeholder="全部分类" clearable @change="loadDocuments">
            <el-option label="全部" value="" />
            <el-option label="商品知识" value="product" />
            <el-option label="售后政策" value="policy" />
            <el-option label="常见问题" value="faq" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadDocuments">刷新</el-button>
        </el-form-item>
      </el-form>

      <!-- 文档列表 -->
      <el-table :data="documents" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="docName" label="文档名称" min-width="200" />
        <el-table-column prop="docType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.docType?.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryType(row.category)" size="small">
              {{ getCategoryName(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadUserName" label="上传人" width="120" />
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="primary" 
              @click="handleReVectorize(row.id)"
              :disabled="row.status === 0"
            >
              重新向量化
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="handleDelete(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && documents.length === 0" description="暂无知识库文档" />
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传知识库文档"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="文档标题" required>
          <el-input v-model="uploadForm.title" placeholder="请输入文档标题（可选，默认使用文件名）" />
        </el-form-item>
        <el-form-item label="文档分类" required>
          <el-select v-model="uploadForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="商品知识" value="product" />
            <el-option label="售后政策" value="policy" />
            <el-option label="常见问题" value="faq" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            accept=".pdf,.docx,.doc,.txt,.md"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word、TXT、Markdown 格式，文件大小不超过 10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">
          开始上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import { get, post, del } from '../utils/request'

const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const selectedCategory = ref('')
const documents = ref<any[]>([])
const uploadRef = ref()
const selectedFile = ref<File | null>(null)

const uploadForm = reactive({
  title: '',
  category: 'custom'
})

// 加载文档列表
const loadDocuments = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (selectedCategory.value) {
      params.append('category', selectedCategory.value)
    }
    
    const data = await get<any>(`/api/admin/knowledge/list?${params}`)
    if (data.success) {
      documents.value = data.data || []
    }
  } catch (error) {
    console.error('加载文档列表失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 文件选择
const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

// 上传文档
const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  if (uploadForm.title) {
    formData.append('title', uploadForm.title)
  }
  formData.append('category', uploadForm.category)
  formData.append('userId', '1')
  formData.append('userName', 'admin')

  try {
    const response = await fetch('/api/admin/knowledge/upload', {
      method: 'POST',
      body: formData
    })
    
    const result = await response.json()
    
    if (result.success) {
      ElMessage.success('文档上传成功')
      showUploadDialog.value = false
      resetUploadForm()
      loadDocuments()
    } else {
      ElMessage.error(result.message || '上传失败')
    }
  } catch (error) {
    console.error('上传失败', error)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

// 重新向量化
const handleReVectorize = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要重新向量化该文档吗？', '提示', {
      type: 'warning'
    })
    
    const data = await post<any>(`/api/admin/knowledge/${id}/revectorize`, {})
    if (data.success) {
      ElMessage.success('重新向量化成功')
      loadDocuments()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('重新向量化失败', error)
      ElMessage.error('操作失败')
    }
  }
}

// 删除文档
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该文档吗？删除后将无法恢复！', '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    
    const data = await del<any>(`/api/admin/knowledge/${id}`)
    if (data.success) {
      ElMessage.success('删除成功')
      loadDocuments()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }
}

// 重置上传表单
const resetUploadForm = () => {
  uploadForm.title = ''
  uploadForm.category = 'custom'
  selectedFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 获取分类名称
const getCategoryName = (category: string) => {
  const map: Record<string, string> = {
    product: '商品知识',
    policy: '售后政策',
    faq: '常见问题',
    custom: '自定义'
  }
  return map[category] || category
}

// 获取分类标签类型
const getCategoryType = (category: string) => {
  const map: Record<string, any> = {
    product: 'success',
    policy: 'warning',
    faq: 'info',
    custom: ''
  }
  return map[category] || ''
}

// 获取状态名称
const getStatusName = (status: number) => {
  const map: Record<number, string> = {
    0: '未处理',
    1: '已向量化',
    2: '向量化失败'
  }
  return map[status] || '未知'
}

// 获取状态标签类型
const getStatusType = (status: number) => {
  const map: Record<number, any> = {
    0: 'info',
    1: 'success',
    2: 'danger'
  }
  return map[status] || ''
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.knowledge-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 20px;
}
</style>
