import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import { isLoggedIn } from '@/utils/auth'

export const moduleRoutes = [
  {
    path: '/sensitive-word',
    name: 'SensitiveWord',
    title: '敏感词',
    icon: 'Warning',
    component: () => import('@/views/sensitiveWord/index.vue')
  },
  {
    path: '/agent-config',
    name: 'AgentConfig',
    title: '智能体配置',
    icon: 'SetUp',
    component: () => import('@/views/agentConfig/index.vue')
  },
  {
    path: '/model',
    name: 'Model',
    title: '模型供应商',
    icon: 'Connection',
    component: () => import('@/views/model/index.vue')
  },
  {
    path: '/agent',
    name: 'Agent',
    title: '智能体',
    icon: 'Monitor',
    component: () => import('@/views/agent/index.vue')
  },
  {
    path: '/hook',
    name: 'Hook',
    title: '钩子',
    icon: 'Switch',
    component: () => import('@/views/hook/index.vue')
  },
  {
    path: '/tool',
    name: 'Tool',
    title: '工具',
    icon: 'Tools',
    component: () => import('@/views/tool/index.vue')
  },
  {
    path: '/skill-package',
    name: 'SkillPackage',
    title: '技能包',
    icon: 'Box',
    component: () => import('@/views/skillPackage/index.vue')
  },
  {
    path: '/mcp',
    name: 'Mcp',
    title: 'MCP',
    icon: 'Cloudy',
    component: () => import('@/views/mcp/index.vue')
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    title: '知识库',
    icon: 'Collection',
    component: () => import('@/views/knowledge/index.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue')
    },
    {
      path: '/',
      component: Layout,
      redirect: '/agent',
      children: moduleRoutes.map((route) => ({
        path: route.path.slice(1),
        name: route.name,
        component: route.component,
        meta: {
          title: route.title,
          icon: route.icon
        }
      }))
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/agent'
    }
  ]
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !isLoggedIn()) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }
  if (to.path === '/login' && isLoggedIn()) {
    return to.query.redirect || '/agent'
  }
  return true
})

export default router

