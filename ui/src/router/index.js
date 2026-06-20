import { createRouter, createWebHistory } from 'vue-router'
import { adminRoutes } from './modules/admin'

const routes = [
  ...adminRoutes,
  {
    path: '/:pathMatch(.*)*',
    redirect: '/agent/manage'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
