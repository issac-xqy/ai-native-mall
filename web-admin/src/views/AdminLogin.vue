<template>
  <div class="admin-login">
    <el-card class="login-card">
      <template #header><h2>智能商城管理后台</h2></template>
      <el-form @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="管理员用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" show-password prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const username = ref('admin')
const password = ref('')
const loading = ref(false)

const handleLogin = async () => {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await fetch('/api/user/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value })
    })
    const data = await res.json()
    if (data.success && data.data?.accessToken) {
      localStorage.setItem('adminToken', data.data.accessToken)
      localStorage.setItem('adminRefreshToken', data.data.refreshToken)
      localStorage.setItem('adminUser', JSON.stringify(data.data.userInfo))
      ElMessage.success('登录成功')
      location.reload()
    } else {
      ElMessage.error(data.message || '登录失败')
    }
  } catch {
    ElMessage.error('登录失败，请检查网络')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
}
.login-card {
  width: 400px;
}
</style>
