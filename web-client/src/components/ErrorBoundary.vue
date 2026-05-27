<template>
  <div v-if="error" class="error-boundary">
    <el-result
      icon="error"
      title="页面出错了"
      :sub-title="errorMessage"
    >
      <template #extra>
        <el-button type="primary" @click="handleRetry">刷新重试</el-button>
        <el-button @click="handleGoHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const error = ref<Error | null>(null)
const errorMessage = ref('未知错误，请稍后重试')

// 捕获子组件错误
onErrorCaptured((err, _instance, _info) => {
  console.error('组件错误:', err)
  error.value = err
  errorMessage.value = err.message || '组件加载失败'
  
  // 阻止错误继续传播
  return false
})

const handleRetry = () => {
  error.value = null
  location.reload()
}

const handleGoHome = () => {
  error.value = null
  router.push('/')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px;
}
</style>
