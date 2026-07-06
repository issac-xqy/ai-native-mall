<template>
  <router-view v-if="isLoginPage" />
  <el-container class="layout-container" v-else>
    <el-header class="nav-header">
      <div class="nav-inner">
        <div class="brand" @click="router.push('/')">
          <span class="brand-icon">✦</span>
          <span class="brand-text">AI Mall</span>
        </div>
        <el-menu :default-active="activeIndex" class="nav-menu" mode="horizontal" router>
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/products">全部商品</el-menu-item>
          <el-menu-item index="/cart">
            购物车<el-badge v-if="cartStore.totalCount > 0" :value="cartStore.totalCount" class="cart-badge"/>
          </el-menu-item>
          <el-menu-item index="/orders">我的订单</el-menu-item>
          <el-menu-item index="/wallet">我的钱包</el-menu-item>
          <el-menu-item index="/ai-chat">AI客服</el-menu-item>
          <el-menu-item index="/profile">个人中心</el-menu-item>
        </el-menu>
        <div class="nav-right">
          <el-input v-model="searchKeyword" placeholder="搜索商品..." :prefix-icon="Search" clearable class="nav-search"
            @keyup.enter="router.push({ path: '/products', query: { keyword: searchKeyword.trim() } })"/>
          <el-dropdown v-if="userStore.isLoggedIn">
            <span class="user-badge">
              <el-avatar :size="34" :style="{background:'var(--brand-gradient)'}">{{ userStore.userInfo?.nickname?.[0] || 'U' }}</el-avatar>
              <span class="user-name">{{ userStore.userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-else type="primary" round @click="router.push('/login')">登录</el-button>
        </div>
      </div>
    </el-header>
    <el-main class="main-content">
      <Suspense>
        <template #default><router-view /></template>
        <template #fallback><SkeletonLoader :count="6" /></template>
      </Suspense>
    </el-main>
    <el-footer class="site-footer">
      <div class="footer-grid">
        <div class="footer-col"><h4>AI Mall</h4><p>智能购物，AI驱动</p></div>
        <div class="footer-col"><h4>服务</h4><p>AI客服</p><p>智能推荐</p></div>
        <div class="footer-col"><h4>链接</h4><p>GitHub</p><p>关于</p></div>
      </div>
      <p class="footer-copy">© 2026 AI-Native Smart Mall · Powered by xqy</p>
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
import SkeletonLoader from './components/SkeletonLoader.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const searchKeyword = ref('')
const activeIndex = computed(() => route.path)
const isLoginPage = computed(() => route.path === '/login')

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
  ElMessage.success('已退出登录')
}
</script>

<style scoped>
.layout-container { min-height: 100vh; display: flex; flex-direction: column; }

/* Header */
.nav-header {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 0 20px; height: 64px; position: sticky; top: 0; z-index: 100;
  box-shadow: 0 2px 20px rgba(0,0,0,0.2);
}
.nav-inner { max-width: 1400px; margin: 0 auto; display: flex; align-items: center; gap: 20px; height: 100%; }
.brand { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.brand-icon { font-size: 24px; color: var(--brand-secondary); animation: pulse 3s infinite; }
.brand-text { font-size: 20px; font-weight: 800; color: #fff; letter-spacing: 1px; }
.nav-menu { --el-menu-bg-color: transparent; --el-menu-text-color: #b8c7e0; --el-menu-active-color: #00CEC9; border-bottom: none !important; flex: 1; }
.nav-menu :deep(.el-menu-item.is-active) { border-bottom: 2px solid #00CEC9 !important; font-weight: 600; }
.nav-right { display: flex; align-items: center; gap: 12px; }
.nav-search { width: 200px; }
.nav-search :deep(.el-input__wrapper) { background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.15); border-radius: 20px; box-shadow: none; }
.nav-search :deep(.el-input__inner) { color: #fff; }
.nav-search :deep(.el-input__inner::placeholder) { color: rgba(255,255,255,0.4); }
.user-badge { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #fff; }
.user-name { font-size: 14px; font-weight: 500; }

/* Main */
.main-content { flex: 1; padding: 24px; max-width: 1400px; margin: 0 auto; width: 100%; }

/* Footer */
.site-footer { background: #1a1a2e; color: #8892b0; padding: 40px 20px 20px; margin-top: auto; }
.footer-grid { max-width: 1200px; margin: 0 auto; display: flex; gap: 60px; margin-bottom: 24px; }
.footer-col h4 { color: #ccd6f6; font-size: 16px; margin-bottom: 12px; }
.footer-col p { color: #8892b0; font-size: 13px; cursor: pointer; margin-bottom: 6px; transition: color 0.2s; }
.footer-col p:hover { color: var(--brand-secondary); }
.footer-copy { text-align: center; font-size: 12px; color: #495670; }
</style>
