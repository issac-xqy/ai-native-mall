import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
    { path: '/', name: 'Home', component: () => import('@/views/Home.vue'), meta: { requiresAuth: true } },
    { path: '/products', name: 'Products', component: () => import('@/views/Products.vue') },
    { path: '/product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetail.vue') },
    { path: '/cart', name: 'Cart', component: () => import('@/views/Cart.vue') },
    { path: '/checkout', name: 'Checkout', component: () => import('@/views/Checkout.vue'), meta: { requiresAuth: true } },
    { path: '/payment', name: 'Payment', component: () => import('@/views/Payment.vue'), meta: { requiresAuth: true } },
    { path: '/orders', name: 'Orders', component: () => import('@/views/Orders.vue'), meta: { requiresAuth: true } },
    { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
    { path: '/upload-demo', name: 'UploadDemo', component: () => import('@/views/UploadDemo.vue') },
    { path: '/ai-chat', name: 'AIChat', component: () => import('@/views/AIChat.vue') },
    { path: '/wallet', name: 'Wallet', component: () => import('@/views/Wallet.vue'), meta: { requiresAuth: true } },
    { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') },
  ],
  scrollBehavior() { return { top: 0 } },
})

const ACCESS_TOKEN_EXPIRE = 15 * 60 * 1000

function hasValidToken(): boolean {
  const tk = localStorage.getItem('accessToken') || localStorage.getItem('token')
  if (!tk || tk === 'undefined' || tk === 'null' || tk.length < 10) return false
  const loginTime = Number(localStorage.getItem('loginTime'))
  return Date.now() - loginTime <= ACCESS_TOKEN_EXPIRE
}

// 路由守卫：需要登录的页面自动跳转
router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth) {
    if (!hasValidToken()) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('loginTime')
      localStorage.removeItem('token')
      return next('/login?redirect=' + encodeURIComponent(to.path))
    }
  }
  if (to.path === '/login') {
    if (hasValidToken()) {
      return next('/')
    }
  }
  next()
})

export default router
