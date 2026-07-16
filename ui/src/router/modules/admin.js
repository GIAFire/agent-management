import Layout from '@/layout/index.vue'

export const adminRoutes = [
  {
    path: '/',
    component: Layout,
    redirect: '/overview',
    children: [
      {
        path: 'overview',
        name: 'Overview',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '运行总览', icon: 'Grid', section: 'AGENT MANAGEMENT' }
      },
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
            meta: { title: '智能体', icon: 'Cpu' }
          },
          {
            path: 'subagent',
            name: 'SubagentManage',
            component: () => import('@/views/subagent/index.vue'),
            meta: { title: '子智能体', icon: 'Connection', section: 'SUBAGENTS' }
          },
          {
            path: 'chat/:agentId',
            name: 'AgentChat',
            component: () => import('@/views/agent/chat.vue'),
            meta: { title: '智能体对话', hidden: true, activeMenu: '/agent/manage' }
          },
          {
            path: 'model',
            name: 'ModelManage',
            component: () => import('@/views/model/index.vue'),
            meta: { title: '模型', icon: 'Connection', section: 'MODEL PROVIDERS' }
          },
          {
            path: 'knowledge',
            name: 'KnowledgeManage',
            component: () => import('@/views/knowledge/index.vue'),
            meta: { title: '知识库', icon: 'Collection', section: 'KNOWLEDGE & RAG' }
          },
          {
            path: 'tool',
            name: 'ToolManage',
            component: () => import('@/views/tool/index.vue'),
            meta: { title: '工具管理', icon: 'Tools', section: 'TOOLS & SKILLS' }
          },
          {
            path: 'skill',
            name: 'SkillManage',
            component: () => import('@/views/skill/index.vue'),
            meta: { title: '技能管理', icon: 'MagicStick', section: 'TOOLS & SKILLS' }
          },
          {
            path: 'skill-package',
            name: 'SkillPackageManage',
            component: () => import('@/views/skillPackage/index.vue'),
            meta: { title: '技能包管理', icon: 'Box', section: 'TOOLS & SKILLS' }
          },
          {
            path: 'mcp',
            name: 'McpManage',
            component: () => import('@/views/mcp/index.vue'),
            meta: { title: '沙箱', icon: 'Share', section: 'SANDBOX' }
          },
          {
            path: 'hook',
            name: 'HookManage',
            component: () => import('@/views/hook/index.vue'),
            meta: { title: '钩子管理', icon: 'Link', section: 'SANDBOX' }
          },
          {
            path: 'sensitive-word',
            name: 'SensitiveWordManage',
            component: () => import('@/views/sensitiveWord/index.vue'),
            meta: { title: '敏感词管理', icon: 'Warning', section: 'SANDBOX' }
          }
        ]
      },
      {
        path: 'user',
        name: 'UserCenter',
        redirect: '/user/manage',
        meta: { title: '系统管理', icon: 'User' },
        children: [
          {
            path: 'tenant',
            name: 'TenantManage',
            component: () => import('@/views/tenant/index.vue'),
            meta: { title: '租户管理', icon: 'OfficeBuilding', section: 'SYSTEM MANAGEMENT' }
          },
          {
            path: 'manage',
            name: 'UserManage',
            component: () => import('@/views/user/index.vue'),
            meta: { title: '用户管理', icon: 'User', section: 'SYSTEM MANAGEMENT' }
          },
          {
            path: 'role',
            name: 'RoleManage',
            component: () => import('@/views/role/index.vue'),
            meta: { title: '角色管理', icon: 'UserFilled', section: 'SYSTEM MANAGEMENT' }
          }
        ]
      }
    ]
  }
]
