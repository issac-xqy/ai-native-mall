import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { title: '数据统计' }
    },
    {
      path: '/products',
      name: 'ProductList',
      component: () => import('@/views/ProductList.vue'),
      meta: { title: '商品管理' }
    },
    {
      path: '/categories',
      name: 'CategoryList',
      component: () => import('@/views/CategoryList.vue'),
      meta: { title: '分类管理' }
    },
    {
      path: '/product/create',
      name: 'ProductCreate',
      component: () => import('@/views/ProductForm.vue'),
      meta: { title: '新增商品' }
    },
    {
      path: '/product/edit/:id',
      name: 'ProductEdit',
      component: () => import('@/views/ProductForm.vue'),
      meta: { title: '编辑商品' }
    },
    {
      path: '/product-ai',
      name: 'ProductAI',
      component: () => import('@/views/ProductAI.vue'),
      meta: { title: 'AI 商品助手' }
    },
    {
      path: '/comment',
      name: 'Comment',
      component: () => import('@/views/CommentAnalysis.vue'),
      meta: { title: '评论分析' }
    },
    {
      path: '/ai-monitor',
      name: 'AiMonitor',
      component: () => import('@/views/AiMonitor.vue'),
      meta: { title: 'AI监控看板' }
    },
    {
      path: '/knowledge',
      name: 'KnowledgeBase',
      component: () => import('@/views/KnowledgeBase.vue'),
      meta: { title: '知识库管理' }
    },
    {
      path: '/orders',
      name: 'OrderList',
      component: () => import('@/views/OrderList.vue'),
      meta: { title: '订单管理' }
    }
  ]
})

export default router
