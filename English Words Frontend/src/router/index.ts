import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('@/views/AppLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'books', name: 'books', component: () => import('@/views/BooksView.vue') },
        { path: 'words', name: 'words', component: () => import('@/views/WordsView.vue') },
        { path: 'dictionary', name: 'dictionary', component: () => import('@/views/DictionaryView.vue') },
        { path: 'quiz', name: 'quiz', component: () => import('@/views/QuizView.vue') },
        { path: 'review', name: 'review', component: () => import('@/views/ReviewView.vue') },
        { path: 'progress', name: 'progress', component: () => import('@/views/ProgressView.vue') },
        { path: 'history', name: 'history', component: () => import('@/views/HistoryView.vue') }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
