import { createRouter, createWebHistory } from 'vue-router'
import { hasRoleAccess, useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '运营看板' } },
        { path: 'lots', name: 'lots', component: () => import('@/views/LotsView.vue'), meta: { title: '停车场管理' } },
        { path: 'lots/:id', name: 'lot-detail', component: () => import('@/views/LotDetailView.vue'), meta: { title: '停车场详情' } },
        { path: 'users', name: 'users', component: () => import('@/views/UsersView.vue'), meta: { title: '用户管理' } },
        { path: 'orders', name: 'orders', component: () => import('@/views/OrdersView.vue'), meta: { title: '订单管理' } },
        { path: 'reservations', name: 'reservations', component: () => import('@/views/ReservationsView.vue'), meta: { title: '预约管理' } },
        { path: 'predict', name: 'predict', component: () => import('@/views/PredictView.vue'), meta: { title: 'AI 占用率预测' } },
        { path: 'knowledge', name: 'knowledge', component: () => import('@/views/KnowledgeView.vue'), meta: { title: 'AI 知识库' } },
        { path: 'ops', name: 'ops', component: () => import('@/views/OpsView.vue'), meta: { title: '运营分析' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.isLoggedIn && to.path === '/login') return '/'
    return true
  }
  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!hasRoleAccess(auth.roles, to.meta.roles as string[] | undefined)) {
    return '/'
  }
  return true
})

export default router
