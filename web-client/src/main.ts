import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'

// 路由守卫 - 登录拦截
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  
  // 需要登录的页面
  const requiresAuth = ['/checkout', '/payment', '/orders', '/profile']
  
  // 先验证并刷新登录状态
  const isValidLogin = userStore.checkAndRefreshAuth()
  
  if (requiresAuth.includes(to.path)) {
    if (!isValidLogin) {
      // 未登录或已过期，跳转到登录页
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }
  }
  
  // 如果已登录且访问登录页，跳转到首页
  if (to.path === '/login' && isValidLogin) {
    next({ path: '/' })
    return
  }
  
  next()
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
