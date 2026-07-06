<template>
  <div class="image-upload-wrapper">
    <!-- 上传区域 -->
    <el-upload
      ref="uploadRef"
      :action="uploadUrl"
      :headers="headers"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-remove="handleRemove"
      :file-list="fileList"
      :limit="limit"
      :multiple="multiple"
      list-type="picture-card"
      accept="image/*"
      :class="{ 'hide-upload': fileList.length >= limit }"
    >
      <el-icon><Plus /></el-icon>
      <div class="upload-text">{{ uploadText }}</div>
    </el-upload>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" :title="previewTitle" width="800px">
      <img :src="previewImage" class="preview-image" />
    </el-dialog>

    <!-- 提示信息 -->
    <div class="upload-tip" v-if="tip">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ tip }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadProps, UploadUserFile, UploadInstance } from 'element-plus'

// 管理端直接从 localStorage 获取 token
const getToken = () => localStorage.getItem('adminToken') || ''

interface Props {
  modelValue?: string | string[]
  limit?: number
  multiple?: boolean
  uploadText?: string
  tip?: string
  maxSize?: number // MB
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  limit: 1,
  multiple: false,
  uploadText: '上传图片',
  tip: '',
  maxSize: 5
})

const emit = defineEmits<{
  'update:modelValue': [value: string | string[]]
  'change': [urls: string | string[]]
}>()

// 直接使用 localStorage，不需要 store
const uploadRef = ref<UploadInstance>()
const previewVisible = ref(false)
const previewImage = ref('')
const previewTitle = ref('')

// 上传接口地址
const uploadUrl = '/api/upload/image'

// 请求头（携带 Token）
const headers = computed(() => ({
  Authorization: getToken()
}))

// 文件列表
const fileList = ref<UploadUserFile[]>([])

// 初始化文件列表
watch(() => props.modelValue, (val) => {
  if (Array.isArray(val)) {
    fileList.value = val.map((url, index) => ({
      name: `image_${index + 1}`,
      url: url
    }))
  } else if (val) {
    fileList.value = [{
      name: 'image_1',
      url: val
    }]
  } else {
    fileList.value = []
  }
}, { immediate: true })

// 上传前校验
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  if (!isLtMaxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB！`)
    return false
  }
  return true
}

// 上传成功
const handleSuccess: UploadProps['onSuccess'] = (response: any) => {
  if (response.success) {
    const url = response.data.url
    ElMessage.success('上传成功')
    
    // 更新值
    if (props.multiple) {
      const urls = fileList.value.map(f => f.url).filter(Boolean) as string[]
      urls.push(url)
      emit('update:modelValue', urls)
      emit('change', urls)
    } else {
      emit('update:modelValue', url)
      emit('change', url)
    }
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 上传失败
const handleError = () => {
  ElMessage.error('上传失败，请重试')
}

// 移除图片
const handleRemove = (file: UploadUserFile) => {
  const url = file.url
  if (props.multiple) {
    const urls = (props.modelValue as string[]).filter(u => u !== url)
    emit('update:modelValue', urls)
    emit('change', urls)
  } else {
    emit('update:modelValue', '')
    emit('change', '')
  }
}

// 暴露方法
defineExpose({
  clearFiles: () => {
    uploadRef.value?.clearFiles()
    fileList.value = []
    emit('update:modelValue', props.multiple ? [] : '')
  }
})
</script>

<style scoped>
.image-upload-wrapper {
  display: inline-block;
}

.upload-text {
  margin-top: 8px;
  color: #8c939d;
  font-size: 12px;
}

.hide-upload :deep(.el-upload--picture-card) {
  display: none;
}

.preview-image {
  width: 100%;
  max-height: 600px;
  object-fit: contain;
}

.upload-tip {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>
