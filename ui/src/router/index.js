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
    path: '/upload',
    component: () => import('@/views/upload.vue')
  },
  {
    path: '/login',
    component: () => import('@/views/login.vue')
  },
  {
    path: '/kline2',
    component: () => import('@/views/kLine/index.vue')
  },
  {
    path: '/kline',
    component: () => import('@/views/kLine/index2.vue')
  },
  {
    path: '/imLogin',
    component: () => import('@/views/im/Login.vue')
  },
  {
    path: '/imMessages',
    component: () => import('@/views/im/Messages.vue')
  },
]

const router = createRouter({
  history: createWebHistory('/wxCount/'),
  routes: routes,
})

export default router
