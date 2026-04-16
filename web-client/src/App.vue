<template>
  <!-- 登录页面全屏显示 -->
  <router-view v-if="isLoginPage" />
  
  <!-- 商城主界面 -->
  <el-container class="layout-container" v-else>
    <el-header class="header">
      <div class="header-content">
        <h1 class="logo" @click="router.push('/')">智能商城</h1>
        <el-menu
          :default-active="activeIndex"
          class="menu"
          mode="horizontal"
          router
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/products">全部商品</el-menu-item>
          <el-menu-item index="/cart">
            购物车
            <el-badge v-if="cartStore.totalCount > 0" :value="cartStore.totalCount" class="cart-badge" />
          </el-menu-item>
          <el-menu-item index="/orders">我的订单</el-menu-item>
          <el-menu-item index="/wallet">我的钱包</el-menu-item>
          <el-menu-item index="/ai-chat">AI客服</el-menu-item>
          <el-menu-item index="/profile">个人中心</el-menu-item>
        </el-menu>
        
        <!-- 搜索框 -->
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品..."
            :prefix-icon="Search"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
        
        <!-- 用户信息 -->
        <div class="user-info">
          <el-dropdown v-if="userStore.isLoggedIn">
            <span class="user-dropdown">
              <el-avatar :size="32" style="margin-right: 8px;">{{ userStore.userInfo?.nickname?.[0] || 'U' }}</el-avatar>
              {{ userStore.userInfo?.username || '用户' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-else type="primary" @click="router.push('/login')">登录</el-button>
        </div>
      </div>
    </el-header>
    
    <el-main class="main-content">
      <ErrorBoundary>
        <router-view />
      </ErrorBoundary>
    </el-main>
    
    <el-footer class="footer">
      <p>© 2026 AI-Native Smart Mall - Powered by xqy</p>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from './stores/cart'
import { useUserStore } from './stores/user'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import ErrorBoundary from './components/ErrorBoundary.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const searchKeyword = ref('')

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/products',
      query: { keyword: searchKeyword.value.trim() }
    })
  }
}

const activeIndex = computed(() => route.path)
const isLoginPage = computed(() => route.path === '/login')

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
  ElMessage.success('已退出登录')
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

.logo {
  font-size: 24px;
  color: #409EFF;
  margin: 0;
  cursor: pointer;
  transition: opacity 0.3s;
}

.logo:hover {
  opacity: 0.8;
}

.menu {
  flex: 1;
  border-bottom: none;
}

.search-box {
  width: 250px;
  margin-right: 15px;
}

.cart-badge {
  margin-left: 5px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 10px;
}

.main-content {
  flex: 1;
  background: #f5f7fa;
  padding: 20px;
}

.footer {
  background: #fff;
  text-align: center;
  padding: 20px;
  color: #909399;
}
</style>
