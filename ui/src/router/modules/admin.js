import Layout from '@/layout/index.vue'

export const adminRoutes = [
  {
    path: '/',
    component: Layout,
    redirect: '/agent/manage',
    children: [
      {
        path: 'agent',
        name: 'Agent',
        redirect: '/agent/manage',
        meta: { title: '智能体', icon: 'Cpu' },
        children: [
          {
            path: 'manage',
            name: 'AgentManage',
            component: () => import('@/views/agent/index.vue'),
            meta: { title: '智能体管理', icon: 'Cpu' }
          },
          {
            path: 'model',
            name: 'ModelManage',
            component: () => import('@/views/model/index.vue'),
            meta: { title: '模型管理', icon: 'Connection' }
          },
          {
            path: 'knowledge',
            name: 'KnowledgeManage',
            component: () => import('@/views/knowledge/index.vue'),
            meta: { title: '知识库管理', icon: 'Collection' }
          },
          {
            path: 'tool',
            name: 'ToolManage',
            component: () => import('@/views/tool/index.vue'),
            meta: { title: '工具管理', icon: 'Tools' }
          },
          {
            path: 'skill-package',
            name: 'SkillPackageManage',
            component: () => import('@/views/skillPackage/index.vue'),
            meta: { title: '技能包管理', icon: 'Box' }
          },
          {
            path: 'mcp',
            name: 'McpManage',
            component: () => import('@/views/mcp/index.vue'),
            meta: { title: 'MCP管理', icon: 'Share' }
          },
          {
            path: 'hook',
            name: 'HookManage',
            component: () => import('@/views/hook/index.vue'),
            meta: { title: '钩子管理', icon: 'Link' }
          },
          {
            path: 'prompt-template',
            name: 'PromptTemplateManage',
            component: () => import('@/views/promptTemplate/index.vue'),
            meta: { title: '系统提示词模板', icon: 'Document' }
          },
          {
            path: 'sensitive-word',
            name: 'SensitiveWordManage',
            component: () => import('@/views/sensitiveWord/index.vue'),
            meta: { title: '敏感词管理', icon: 'Warning' }
          }
        ]
      },
      {
        path: 'user',
        name: 'UserCenter',
        redirect: '/user/manage',
        meta: { title: '用户管理', icon: 'User' },
        children: [
          {
            path: 'tenant',
            name: 'TenantManage',
            component: () => import('@/views/tenant/index.vue'),
            meta: { title: '租户管理', icon: 'OfficeBuilding' }
          },
          {
            path: 'manage',
            name: 'UserManage',
            component: () => import('@/views/user/index.vue'),
            meta: { title: '用户管理', icon: 'User' }
          }
        ]
      }
    ]
  }
]
