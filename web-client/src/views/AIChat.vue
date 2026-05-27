<template>
  <div class="chat-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>💬 AI 智能客服</span>
          <el-tag type="success">在线</el-tag>
        </div>
      </template>

      <!-- 对话消息区域 -->
      <div class="messages" ref="messagesRef">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message', msg.role === 'user' ? 'user' : 'ai']"
        >
          <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="content">{{ msg.content }}</div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          v-model="userInput"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题，例如：iPhone 15 Pro 有什么特点？"
          @keydown.enter.ctrl="sendMessage"
        />
        <div class="actions">
          <el-button type="primary" @click="sendMessage" :loading="loading">
            {{ loading ? 'AI 思考中...' : '发送 (Ctrl+Enter)' }}
          </el-button>
          <el-button @click="clearChat">清空对话</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

interface Message {
  role: 'user' | 'ai'
  content: string
}

const userStore = useUserStore()

const messages = ref<Message[]>([
  {
    role: 'ai',
    content: '您好！我是 AI-Native 智能商城的客服助手，请问有什么可以帮助您的？'
  }
])

const userInput = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLDivElement>()

const getUserId = () => {
  if (userStore.userInfo?.username) {
    return userStore.userInfo.username
  }
  return 'guest_user'
}

const sendMessage = async () => {
  if (!userInput.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  const question = userInput.value.trim()
  messages.value.push({ role: 'user', content: question })
  userInput.value = ''
  loading.value = true

  // 添加空的 AI 消息占位
  messages.value.push({ role: 'ai', content: '' })

  const userId = getUserId()
  const url = `/api/ai/customer-service/stream?userId=${encodeURIComponent(userId)}&question=${encodeURIComponent(question)}&apiKey=sk-test`

  try {
    const response = await fetch(url)
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }

      buffer += decoder.decode(value, { stream: true })
      
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        // 处理一行中可能包含多个 SSE 事件的情况
        const events = line.split('data:')
        for (const event of events) {
          const trimmed = event.trim()
          if (!trimmed) continue
          
          if (trimmed === '[DONE]') {
            loading.value = false
            return
          }

          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'ai') {
            lastMsg.content += trimmed
            scrollToBottom()
          }
        }
      }
    }
  } catch (error: any) {
    console.error('Stream error:', error)
    ElMessage.error(`AI 服务连接失败: ${error.message}`)
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.role === 'ai' && !lastMsg.content) {
      messages.value.pop()
    }
  } finally {
    loading.value = false
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const clearChat = () => {
  messages.value = [
    {
      role: 'ai',
      content: '对话已清空。您好！我是 AI-Native 智能商城的客服助手，请问有什么可以帮助您的？'
    }
  ]
}
</script>

<style scoped>
.chat-container {
  max-width: 900px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.messages {
  height: 500px;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 15px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #e6f7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.message.user .avatar {
  background-color: #fff7e6;
}

.content {
  max-width: 60%;
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message.ai .content {
  background-color: white;
  border: 1px solid #e4e7ed;
}

.message.user .content {
  background-color: #409EFF;
  color: white;
}

.input-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
