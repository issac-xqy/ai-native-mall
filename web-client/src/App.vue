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
          <el-menu-item index="/">{{ $t('nav.home') }}</el-menu-item>
          <el-menu-item index="/products">{{ $t('nav.products') }}</el-menu-item>
          <el-menu-item index="/cart">
            {{ $t('nav.cart') }}<el-badge v-if="cartStore.totalCount > 0" :value="cartStore.totalCount" class="cart-badge"/>
          </el-menu-item>
          <el-menu-item index="/orders">{{ $t('nav.orders') }}</el-menu-item>
          <el-menu-item index="/wallet">{{ $t('nav.wallet') }}</el-menu-item>
          <el-menu-item index="/ai-chat">{{ $t('nav.aiChat') }}</el-menu-item>
          <el-menu-item index="/profile">{{ $t('nav.profile') }}</el-menu-item>
        </el-menu>
        <div class="nav-right">
          <el-input v-model="searchKeyword" :placeholder="$t('nav.search')" :prefix-icon="Search" clearable class="nav-search"
            @keyup.enter="router.push({ path: '/products', query: { keyword: searchKeyword.trim() } })"/>
          <el-button text circle class="lang-btn" @click="toggleLang">
            {{ locale === 'zh-CN' ? 'EN' : '中' }}
          </el-button>
          <el-dropdown v-if="userStore.isLoggedIn">
            <span class="user-badge">
              <el-avatar :size="34" :style="{background:'var(--brand-gradient)'}">{{ userStore.userInfo?.nickname?.[0] || 'U' }}</el-avatar>
              <span class="user-name">{{ userStore.userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">{{ $t('logout.profile') }}</el-dropdown-item>
                <el-dropdown-item @click="router.push('/orders')">{{ $t('logout.orders') }}</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">{{ $t('logout.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-else type="primary" round @click="router.push('/login')">{{ $t('nav.login') }}</el-button>
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
        <div class="footer-col"><h4>AI Mall</h4><p>{{ $t('footer.about') }}</p></div>
        <div class="footer-col"><h4>{{ $t('footer.service') }}</h4><p>{{ $t('footer.aiService') }}</p><p>{{ $t('footer.aiRecommend') }}</p></div>
        <div class="footer-col"><h4>{{ $t('footer.links') }}</h4><p>GitHub</p><p>关于</p></div>
      </div>
      <p class="footer-copy">{{ $t('footer.copy') }}</p>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCartStore } from './stores/cart'
import { useUserStore } from './stores/user'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import SkeletonLoader from './components/SkeletonLoader.vue'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const searchKeyword = ref('')
const activeIndex = computed(() => route.path)
const isLoginPage = computed(() => route.path === '/login')

const toggleLang = () => {
  locale.value = locale.value === 'zh-CN' ? 'en' : 'zh-CN'
  localStorage.setItem('lang', locale.value)
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
  ElMessage.success(t('logout.success'))
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
.lang-btn { color: #b8c7e0; font-size: 12px; font-weight: 700; width: 34px; height: 34px; }
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
