<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <div class="auth-header">
        <h1>🛒 AI智能商城</h1>
        <p class="subtitle">智能购物，AI驱动</p>
      </div>

      <!-- 登录表单 -->
      <div v-if="mode === 'login'">
        <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" class="auth-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名/手机号"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>

          <div class="auth-links">
            <el-link type="primary" @click="mode = 'register'">注册账号</el-link>
            <el-link type="info">忘记密码？</el-link>
          </div>
        </el-form>
      </div>

      <!-- 注册表单 -->
      <div v-else>
        <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" class="auth-form">
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              prefix-icon="Phone"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱（选填）"
              prefix-icon="Message"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（6-20位）"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >
              注册
            </el-button>
          </el-form-item>

          <div class="auth-links">
            <el-link type="primary" @click="mode = 'login'">返回登录</el-link>
          </div>
        </el-form>
      </div>

      <!-- 第三方登录 -->
      <el-divider>
        <span class="divider-text">其他登录方式</span>
      </el-divider>

      <div class="social-login">
        <el-button circle size="large" class="social-btn">
          <span>微信</span>
        </el-button>
        <el-button circle size="large" class="social-btn">
          <span>QQ</span>
        </el-button>
        <el-button circle size="large" class="social-btn">
          <span>邮箱</span>
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)

const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const registerForm = reactive({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: any, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await fetch('/api/user/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: loginForm.username,
          password: loginForm.password
        })
      })

      const data = await response.json()
      if (data.success) {
        // 同步更新 Store 和 localStorage
        userStore.token = data.token
        userStore.userInfo = data.userInfo
        userStore.loginTime = data.loginTime || Date.now()
        
        localStorage.setItem('token', data.token)
        localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
        localStorage.setItem('loginTime', String(userStore.loginTime))
        
        ElMessage.success('登录成功')
        
        // 跳转到首页或之前的页面
        const redirect = router.currentRoute.value.query.redirect as string
        router.push(redirect || '/')
      } else {
        ElMessage.error(data.message || '登录失败')
      }
    } catch (error) {
      ElMessage.error('登录失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await fetch('/api/user/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: registerForm.username,
          phone: registerForm.phone,
          email: registerForm.email,
          password: registerForm.password
        })
      })

      const data = await response.json()
      if (data.success) {
        ElMessage.success('注册成功，请登录')
        mode.value = 'login'
        loginForm.username = registerForm.username
      } else {
        ElMessage.error(data.message || '注册失败')
      }
    } catch (error) {
      ElMessage.error('注册失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 450px;
  padding: 40px;
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
}

.auth-header h1 {
  color: #409EFF;
  font-size: 28px;
  margin: 0 0 10px 0;
}

.subtitle {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.auth-form {
  margin-top: 20px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.divider-text {
  color: #909399;
  font-size: 14px;
}

.social-login {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}

.social-btn {
  width: 50px;
  height: 50px;
  font-size: 24px;
}
</style>
