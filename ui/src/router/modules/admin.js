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
            meta: {
              title: '智能体',
              icon: 'Cpu',
              description: '统一配置模型、提示词和可调用能力，查看真实运行表现。'
            }
          },
          {
            path: 'subagent',
            name: 'SubagentManage',
            component: () => import('@/views/subagent/index.vue'),
            meta: {
              title: '子智能体',
              icon: 'Connection',
              description: '集中管理可被智能体委派工作的可复用能力，监控协作状态与执行表现。'
            }
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
            meta: {
              title: '模型管理',
              icon: 'Connection',
              description: '集中维护文本模型连接、生成参数与调用状态。配置变更会应用于后续所有请求。'
            }
          },
          {
            path: 'knowledge',
            name: 'KnowledgeManage',
            component: () => import('@/views/knowledge/index.vue'),
            meta: {
              title: '知识库',
              icon: 'Collection',
              description: '配置独立的 Embedding 模型，管理可供智能体检索的租户知识。'
            }
          },
          {
            path: 'knowledge/:knowledgeBaseId/documents',
            name: 'KnowledgeDocuments',
            component: () => import('@/views/knowledge/documents.vue'),
            meta: { title: '知识库文档', hidden: true, activeMenu: '/agent/knowledge' }
          },
          {
            path: 'tool',
            name: 'ToolManage',
            component: () => import('@/views/tool/index.vue'),
            meta: {
              title: '工具管理',
              icon: 'Tools',
              description: '集中管理智能体可调用的工具、工具分组与权限策略，安全扩展智能体执行能力。'
            }
          },
          {
            path: 'skill',
            name: 'SkillManage',
            component: () => import('@/views/skill/index.vue'),
            meta: {
              title: '技能管理',
              icon: 'MagicStick',
              description: '集中维护技能指令、资源文件、角色范围与智能体绑定后的真实使用情况。'
            }
          },
          {
            path: 'skill-package',
            name: 'SkillPackageManage',
            component: () => import('@/views/skillPackage/index.vue'),
            meta: { title: '技能包管理', icon: 'Box', section: 'TOOLS & SKILLS' }
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
