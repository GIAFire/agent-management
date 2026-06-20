import {createRouter, createWebHistory} from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/views/index/index.vue')
  },
  {
    path: '/index',
    component: () => import('@/views/index/index.vue')
  },
  {
    path: '/login',
    component: () => import('@/views/login.vue')
  },
]

const router = createRouter({
  history: createWebHistory('/agentScope/'),
  routes: routes,
})

export default router
