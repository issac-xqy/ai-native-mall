<template>
  <div class="admin-category-list">
    <el-card>
      <!-- 操作栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增分类
        </el-button>
        <el-button 
          type="danger" 
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button @click="loadCategories">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 分类表格 -->
      <el-table
        :data="categories"
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        @selection-change="handleSelectionChange"
        style="margin-top: 20px"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="name" label="分类名称" min-width="180" />
        
        <el-table-column label="图标" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.icon"
              :src="row.icon"
              style="width: 36px; height: 36px"
              fit="cover"
            />
            <span v-else class="no-icon">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />

        <el-table-column label="操作" min-width="260" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button 
                type="success" 
                size="small" 
                @click="handleAddChild(row)"
              >
                添加子分类
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        
        <el-form-item label="父分类">
          <el-tree-select
            v-model="form.parentId"
            :data="categoryTreeOptions"
            :props="{ label: 'name', value: 'id' }"
            check-strictly
            placeholder="选择父分类（不选则为一级分类）"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="分类图标">
          <el-input v-model="form.icon" placeholder="请输入图标URL" />
          <el-image
            v-if="form.icon"
            :src="form.icon"
            style="width: 100px; height: 100px; margin-top: 10px"
            fit="cover"
          />
        </el-form-item>
        
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete, Refresh } from '@element-plus/icons-vue'
import { get, post, put, del } from '../utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const categories = ref<any[]>([])
const selectedRows = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  parentId: 0,
  icon: '',
  sortOrder: 0,
  status: 1
})

const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ]
})

// 分类树选项（用于选择父分类）
const categoryTreeOptions = ref<any[]>([])

// 加载分类列表
const loadCategories = async () => {
  loading.value = true
  try {
    const data = await get<any>('/api/admin/category/list')
    if (data.success) {
      categories.value = data.data
      // 构建分类树选项
      categoryTreeOptions.value = buildTreeOptions(data.data)
    }
  } catch (error) {
    console.error('加载分类列表失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 构建树形选项（排除当前编辑的分类及其子分类，防止循环引用）
const buildTreeOptions = (list: any[], excludeId?: number): any[] => {
  const tree: any[] = []
  const map = new Map()
  
  list.forEach(item => {
    if (item.id === excludeId) return // 排除当前编辑的分类
    map.set(item.id, { ...item, children: [] })
  })
  
  list.forEach(item => {
    if (item.id === excludeId) return
    const node = map.get(item.id)
    if (item.parentId === 0 || !map.has(item.parentId)) {
      tree.push(node)
    } else {
      const parent = map.get(item.parentId)
      if (parent) {
        parent.children.push(node)
      }
    }
  })
  
  return tree
}

// 新增分类
const handleAdd = () => {
  dialogTitle.value = '新增分类'
  Object.assign(form, {
    id: undefined,
    name: '',
    parentId: 0,
    icon: '',
    sortOrder: 0,
    status: 1
  })
  dialogVisible.value = true
}

// 添加子分类
const handleAddChild = (row: any) => {
  dialogTitle.value = '添加子分类'
  Object.assign(form, {
    id: undefined,
    name: '',
    parentId: row.id,
    icon: '',
    sortOrder: 0,
    status: 1
  })
  dialogVisible.value = true
}

// 编辑分类
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑分类'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    parentId: row.parentId,
    icon: row.icon || '',
    sortOrder: row.sortOrder,
    status: row.status
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      if (form.id) {
        // 更新
        const data = await put<any>(`/api/admin/category/${form.id}`, form)
        if (data.success) {
          ElMessage.success('更新成功')
          dialogVisible.value = false
          loadCategories()
        } else {
          ElMessage.error(data.message || '更新失败')
        }
      } else {
        // 新增
        const data = await post<any>('/api/admin/category', form)
        if (data.success) {
          ElMessage.success('创建成功')
          dialogVisible.value = false
          loadCategories()
        } else {
          ElMessage.error(data.message || '创建失败')
        }
      }
    } catch (error: any) {
      console.error('提交失败', error)
      ElMessage.error(error.response?.data?.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 删除分类
const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定要删除分类"${row.name}"吗？如果该分类下有子分类或商品，将无法删除。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const data = await del<any>(`/api/admin/category/${row.id}`)
      if (data.success) {
        ElMessage.success('删除成功')
        loadCategories()
      } else {
        ElMessage.error(data.message || '删除失败')
      }
    } catch (error: any) {
      console.error('删除失败', error)
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedRows.value.length} 个分类吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const ids = selectedRows.value.map(row => row.id)
      const data = await del<any>('/api/admin/category/batch', { data: ids })
      if (data.success) {
        ElMessage.success('批量删除成功')
        loadCategories()
      } else {
        ElMessage.error(data.message || '批量删除失败')
      }
    } catch (error: any) {
      console.error('批量删除失败', error)
      ElMessage.error(error.response?.data?.message || '批量删除失败')
    }
  }).catch(() => {})
}

// 状态变更
const handleStatusChange = async (row: any) => {
  try {
    const data = await put<any>(`/api/admin/category/${row.id}/status`, {
      status: row.status
    })
    if (data.success) {
      ElMessage.success('状态更新成功')
    } else {
      ElMessage.error(data.message || '状态更新失败')
      // 恢复原状态
      row.status = row.status === 1 ? 0 : 1
    }
  } catch (error: any) {
    console.error('状态更新失败', error)
    ElMessage.error(error.response?.data?.message || '状态更新失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 选择变化
const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.admin-category-list {
  padding: 20px;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
}

.no-icon {
  color: #909399;
  font-size: 14px;
}
</style>
