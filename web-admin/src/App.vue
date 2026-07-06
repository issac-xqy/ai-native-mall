<template>
  <AdminLogin v-if="!token" />
  <el-container class="layout-container" :class="{ 'dark-mode': isDark }" v-else>
    <el-aside width="220px" class="sidebar">
      <div class="logo"><h2>智能商城管理后台</h2></div>
      <el-menu :default-active="route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/"><span>数据统计</span></el-menu-item>
        <el-menu-item index="/products"><span>商品管理</span></el-menu-item>
        <el-menu-item index="/categories"><span>分类管理</span></el-menu-item>
        <el-menu-item index="/product-ai"><span>智能运营助手</span></el-menu-item>
        <el-menu-item index="/comment"><span>评论情感分析</span></el-menu-item>
        <el-menu-item index="/ai-monitor"><span>运营监控看板</span></el-menu-item>
        <el-menu-item index="/knowledge"><span>知识库管理</span></el-menu-item>
        <el-menu-item index="/orders"><span>订单管理</span></el-menu-item>
        <el-menu-item index="/logout" style="margin-top:auto" @click="logout"><span>退出登录</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>Smart Mall Admin</span>
        <el-button text @click="isDark = !isDark">{{ isDark ? '☀️' : '🌙' }}</el-button>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import AdminLogin from './views/AdminLogin.vue'

const route = useRoute()
const isDark = ref(localStorage.getItem('darkMode') === '1')
const token = ref(localStorage.getItem('adminToken') || localStorage.getItem('token'))

const logout = () => {
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminUser')
  token.value = ''
  location.reload()
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.sidebar { display: flex; flex-direction: column; background-color: #304156; color: white; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; background-color: #263445; }
.logo h2 { font-size: 16px; color: #fff; }
.header { background-color: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; font-size: 14px; color: #606266; }
.main-content { background-color: #f0f2f5; padding: 20px; }

/* Dark mode */
.dark-mode .main-content { background-color: #1a1a2e; color: #e0e0e0; }
.dark-mode .header { background-color: #16213e; color: #e0e0e0; border-color: #0f3460; }
.dark-mode .sidebar { background-color: #0f3460; }
.dark-mode .logo { background-color: #16213e; }
.dark-mode :deep(.el-card) { background-color: #16213e; border-color: #0f3460; color: #e0e0e0; }
.dark-mode :deep(.el-table) { background-color: #16213e; color: #e0e0e0; }
.dark-mode :deep(.el-table th) { background-color: #1a1a2e; }
.dark-mode :deep(.el-table tr) { background-color: #16213e; }
.dark-mode :deep(.el-input__inner) { background-color: #1a1a2e; color: #e0e0e0; }

/* Responsive */
@media (max-width: 768px) {
  .sidebar { width: 64px !important; }
  .sidebar .logo h2 { display: none; }
  .sidebar :deep(.el-menu-item span) { display: none; }
  .main-content { padding: 10px; }
}
</style>
