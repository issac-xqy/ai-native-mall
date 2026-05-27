<template>
  <div class="image-upload">
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      @change="handleFileChange"
      style="display: none"
    />

    <!-- 已选择图片预览 -->
    <div v-if="imageUrl" class="preview-wrapper">
      <img :src="getImageUrl(imageUrl)" class="preview-image" />
      <div class="preview-actions">
        <el-button size="small" @click="handleReplace">重新选择</el-button>
        <el-button size="small" type="danger" @click="handleRemove">删除</el-button>
      </div>
    </div>

    <!-- 上传区域 -->
    <div v-else class="upload-area" @click="triggerUpload" @dragover.prevent @drop="handleDrop">
      <el-icon :size="40" color="#c0c4cc"><Plus /></el-icon>
      <p class="upload-text">{{ placeholder || '点击或拖拽上传图片' }}</p>
      <p class="upload-tip">{{ tip || '支持 JPG、PNG、GIF、WebP 格式，不超过 10MB' }}</p>
    </div>

    <!-- 上传进度 -->
    <el-progress v-if="uploading" :percentage="progress" :stroke-width="4" class="upload-progress" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

interface Props {
  modelValue?: string
  placeholder?: string
  tip?: string
  compress?: boolean
  maxSize?: number
  endpoint?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '点击或拖拽上传图片',
  tip: '支持 JPG、PNG、GIF、WebP 格式，不超过 10MB',
  compress: true,
  maxSize: 10,
  endpoint: '/api/upload/product'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  success: [data: any]
}>()

const fileInput = ref<HTMLInputElement>()
const imageUrl = ref(props.modelValue)
const uploading = ref(false)
const progress = ref(0)

// 监听外部值变化
watch(() => props.modelValue, (val) => {
  imageUrl.value = val
})

// 触发文件选择
const triggerUpload = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    uploadFile(input.files[0])
  }
}

// 处理拖拽上传
const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  if (event.dataTransfer?.files && event.dataTransfer.files[0]) {
    uploadFile(event.dataTransfer.files[0])
  }
}

// 上传图片
const uploadFile = async (file: File) => {
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  // 验证文件大小
  const maxSizeBytes = props.maxSize * 1024 * 1024
  if (file.size > maxSizeBytes) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return
  }

  uploading.value = true
  progress.value = 0

  try {
    // 压缩图片
    let uploadFile: File = file
    if (props.compress && file.type.startsWith('image/')) {
      uploadFile = await compressImage(file)
    }

    // 模拟进度
    const progressInterval = setInterval(() => {
      progress.value = Math.min(progress.value + 10, 90)
    }, 200)

    // 上传文件
    const formData = new FormData()
    formData.append('file', uploadFile)

    const response = await fetch(props.endpoint, {
      method: 'POST',
      body: formData
    })

    clearInterval(progressInterval)
    progress.value = 100

    const data = await response.json()
    
    if (data.success) {
      imageUrl.value = data.data.url
      emit('update:modelValue', data.data.url)
      emit('success', data.data)
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(data.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传失败', error)
    ElMessage.error('上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
    progress.value = 0
  }
}

// 压缩图片
const compressImage = (file: File, maxWidth = 1200, quality = 0.8): Promise<File> => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target?.result as string
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height

        // 缩放图片
        if (width > maxWidth) {
          height = (height * maxWidth) / width
          width = maxWidth
        }

        canvas.width = width
        canvas.height = height

        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)

        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve(new File([blob], file.name, { type: 'image/jpeg' }))
            } else {
              resolve(file)
            }
          },
          'image/jpeg',
          quality
        )
      }
    }
    reader.onerror = () => resolve(file)
  })
}

// 获取完整图片 URL
const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url
}

// 重新选择
const handleReplace = () => {
  imageUrl.value = ''
  emit('update:modelValue', '')
  triggerUpload()
}

// 删除图片
const handleRemove = () => {
  imageUrl.value = ''
  emit('update:modelValue', '')
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 暴露方法
defineExpose({
  triggerUpload
})
</script>

<style scoped>
.image-upload {
  width: 100%;
}

.preview-wrapper {
  position: relative;
  display: inline-block;
  border-radius: 8px;
  overflow: hidden;
}

.preview-image {
  width: 100%;
  max-width: 300px;
  height: auto;
  display: block;
  border-radius: 8px;
}

.preview-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  padding: 10px;
  display: flex;
  justify-content: center;
  gap: 10px;
  opacity: 0;
  transition: opacity 0.3s;
}

.preview-wrapper:hover .preview-actions {
  opacity: 1;
}

.upload-area {
  width: 100%;
  max-width: 300px;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}

.upload-area:hover {
  border-color: #409EFF;
  background: #ecf5ff;
}

.upload-text {
  margin: 10px 0 5px 0;
  color: #606266;
  font-size: 14px;
}

.upload-tip {
  margin: 0;
  color: #909399;
  font-size: 12px;
}

.upload-progress {
  margin-top: 15px;
}
</style>
