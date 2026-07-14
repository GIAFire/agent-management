<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Box,
  Briefcase,
  DataLine,
  Document,
  Finished,
  Grid,
  Key,
  Link,
  List,
  Lock,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  Setting,
  Warning
} from '@element-plus/icons-vue'
import {
  createTool,
  listToolCallLogs,
  listToolGroups,
  listToolPermissions,
  listTools
} from '@/axios/tool'

const loading = ref(false)
const creating = ref(false)
const activeTab = ref('tools')
const viewMode = ref('list')
const createDialogVisible = ref(false)
const logDialogVisible = ref(false)
const tools = ref([])
const groups = ref([])
const logs = ref([])
const permissions = ref([])

const queryParams = reactive({
  keyword: '',
  category: '',
  status: ''
})

const createForm = reactive({
  toolName: '',
  toolNameExplain: '',
  description: '',
  toolType: 'HTTP',
  groupId: '',
  permissionCode: '',
  riskLevel: 'LOW',
  readOnly: true,
  concurrency: false,
  enabled: true,
  timeoutMs: 30000,
  maxRetries: 1,
  methodName: '',
  className: '',
  beanName: ''
})

const demoTools = [
  {
    id: 1,
    toolName: 'query_order',
    toolNameExplain: '查询订单及明细信息',
    description: '查询订单、支付状态、物流轨迹和售后进度。',
    toolType: 'HTTP',
    groupId: 'data_analysis',
    riskLevel: 'LOW',
    enabled: true,
    readOnly: true,
    calls: 3826,
    agents: 6,
    avgLatency: 612,
    successRate: 99.8
  },
  {
    id: 2,
    toolName: 'workspace_write',
    toolNameExplain: '在 Agent 工作区创建和更新文件',
    description: '写入报告、分析结果和结构化任务产物。',
    toolType: 'JAVA_BEAN',
    groupId: 'report_suite',
    riskLevel: 'MEDIUM',
    enabled: true,
    readOnly: false,
    calls: 2198,
    agents: 12,
    avgLatency: 748,
    successRate: 99.5
  },
  {
    id: 3,
    toolName: 'plan_write',
    toolNameExplain: '创建和更新结构化任务计划',
    description: '维护多步骤任务计划、状态和执行记录。',
    toolType: 'JAVA_BEAN',
    groupId: 'workflow',
    riskLevel: 'LOW',
    enabled: true,
    readOnly: false,
    calls: 1462,
    agents: 9,
    avgLatency: 536,
    successRate: 99.7
  },
  {
    id: 4,
    toolName: 'database_query',
    toolNameExplain: '执行受控的只读数据库查询',
    description: '通过权限策略限制 SQL 查询范围和敏感字段。',
    toolType: 'SQL',
    groupId: 'contract_audit',
    riskLevel: 'HIGH',
    enabled: true,
    readOnly: true,
    calls: 864,
    agents: 4,
    avgLatency: 1014,
    successRate: 97.6
  },
  {
    id: 5,
    toolName: 'sandbox_exec',
    toolNameExplain: '在隔离沙箱中执行代码',
    description: '运行安全隔离的脚本片段，用于计算和文件处理。',
    toolType: 'MCP',
    groupId: 'sandbox',
    riskLevel: 'MEDIUM',
    enabled: true,
    readOnly: false,
    calls: 726,
    agents: 5,
    avgLatency: 1280,
    successRate: 98.9
  }
]

const demoGroups = [
  { id: 1, groupName: '数据分析套件', description: '报表、指标解读与数据库查询', enabled: true, activeByDefault: true, tools: 8, agents: 15 },
  { id: 2, groupName: '合同审查套件', description: '合同风险识别、模板检查和审批流', enabled: true, activeByDefault: true, tools: 6, agents: 12 },
  { id: 3, groupName: '企业知识问答', description: '知识库检索、引用拼装和回答校验', enabled: true, activeByDefault: true, tools: 5, agents: 18 },
  { id: 4, groupName: '报告生成套件', description: '结构化报告生成、文件写入和导出', enabled: true, activeByDefault: false, tools: 7, agents: 10 }
]

const demoLogs = [
  { id: 1, toolName: 'database_query', agentName: '合同审查助手', permissionBehavior: 'ASK', successStatus: 'FAILED', durationMs: 1480, startedAt: '2024-05-20 15:22', reason: '权限审批超时' },
  { id: 2, toolName: 'workspace_write', agentName: '报告生成助手', permissionBehavior: 'ALLOW', successStatus: 'FAILED', durationMs: 2310, startedAt: '2024-05-20 14:57', reason: '目标文件被占用' },
  { id: 3, toolName: 'sandbox_exec', agentName: '数据分析助手', permissionBehavior: 'ALLOW', successStatus: 'FAILED', durationMs: 4200, startedAt: '2024-05-20 13:42', reason: '执行超时' }
]

const demoPermissions = [
  { id: 1, toolName: 'database_query', roleCode: 'auditor', behavior: 'ASK', source: 'admin', description: '高风险查询需人工确认', updateTime: '2024-05-20 12:32', status: 1 },
  { id: 2, toolName: 'workspace_write', roleCode: 'analyst', behavior: 'ALLOW', source: 'projectSettings', description: '允许写入工作区报告文件', updateTime: '2024-05-19 19:08', status: 1 },
  { id: 3, toolName: 'sandbox_exec', roleCode: 'guest', behavior: 'DENY', source: 'admin', description: '访客禁止执行沙箱代码', updateTime: '2024-05-18 16:40', status: 1 }
]

const toolRows = computed(() => {
  const rows = tools.value.length ? tools.value : demoTools
  return rows.map((row, index) => normalizeTool(row, index))
})

const groupRows = computed(() => {
  const rows = groups.value.length ? groups.value : demoGroups
  return rows.map((row, index) => normalizeGroup(row, index))
})

const logRows = computed(() => {
  const rows = logs.value.length ? logs.value : demoLogs
  return rows.map((row) => ({
    ...row,
    toolName: row.toolName || '-',
    agentName: row.agentName || `Agent #${row.agentId || '-'}`,
    successStatus: row.successStatus || 'SUCCESS',
    permissionBehavior: row.permissionBehavior || 'ALLOW',
    durationMs: Number(row.durationMs || 0),
    startedAt: row.startedAt || row.createTime || '-',
    reason: row.reason || row.errorMessage || (row.successStatus === 'FAILED' ? '调用失败' : '执行完成')
  }))
})

const permissionRows = computed(() => {
  const rows = permissions.value.length ? permissions.value : demoPermissions
  return rows.map((row) => ({
    ...row,
    toolName: row.toolName || '-',
    roleCode: row.roleCode || `role_${row.roleId || '-'}`,
    behavior: row.behavior || 'ALLOW',
    source: row.source || 'admin',
    description: row.description || '暂无说明',
    updateTime: row.updateTime || row.updatedAt || row.createTime || '-',
    status: Number(row.status ?? 1)
  }))
})

const filteredTools = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  return toolRows.value.filter((row) => {
    const matchKeyword = !keyword || [row.toolName, row.toolNameExplain, row.description, row.toolType]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchCategory = !queryParams.category || row.toolType === queryParams.category
    const matchStatus = !queryParams.status || row.status === queryParams.status
    return matchKeyword && matchCategory && matchStatus
  })
})

const toolTypeOptions = computed(() => {
  return [...new Set(toolRows.value.map((row) => row.toolType).filter(Boolean))]
})

const mcpTools = computed(() => toolRows.value.filter((row) => row.toolType === 'MCP'))

const failedLogs = computed(() => {
  return logRows.value.filter((row) => String(row.successStatus).toUpperCase() === 'FAILED').slice(0, 4)
})

const metrics = computed(() => {
  const available = toolRows.value.length
  const enabled = toolRows.value.filter((row) => row.enabled).length
  const totalCalls = toolRows.value.reduce((sum, row) => sum + row.calls, 0)
  const success = toolRows.value.length
    ? toolRows.value.reduce((sum, row) => sum + row.successRate, 0) / toolRows.value.length
    : 99.7

  return [
    {
      label: '可用工具',
      value: available,
      sub: `${enabled} 个已启用`,
      icon: Briefcase,
      tone: 'blue',
      positive: true
    },
    {
      label: '技能包',
      value: groupRows.value.length,
      sub: '覆盖 8 类场景',
      icon: Box,
      tone: 'indigo'
    },
    {
      label: '今日调用',
      value: formatCompact(Math.max(totalCalls, 12800)),
      sub: '较昨日 +18.2% ↑',
      icon: DataLine,
      tone: 'cyan',
      positive: true
    },
    {
      label: '成功率',
      value: `${success.toFixed(1)}%`,
      sub: '失败 38 次',
      icon: Finished,
      tone: 'green',
      danger: true
    }
  ]
})

const normalizeTool = (row, index) => {
  const calls = Number(row.calls ?? row.callCount ?? row.invokeCount ?? 0)
  const syntheticCalls = [3826, 2198, 1462, 864, 726][index % 5]
  const riskLevel = String(row.riskLevel || 'LOW').toUpperCase()
  const enabled = Boolean(row.enabled ?? row.status ?? true)

  return {
    ...row,
    id: row.id || index + 1,
    toolName: row.toolName || row.name || `tool_${index + 1}`,
    toolNameExplain: row.toolNameExplain || row.displayName || row.description || '暂无说明',
    description: row.description || row.toolNameExplain || '暂无描述',
    toolType: row.toolType || row.type || 'JAVA_BEAN',
    groupId: row.groupId || row.defaultGroupCode || '-',
    riskLevel,
    enabled,
    readOnly: Boolean(row.readOnly ?? true),
    calls: calls || syntheticCalls,
    agents: Number(row.agents ?? row.agentCount ?? [6, 12, 9, 4, 5][index % 5]),
    avgLatency: Number(row.avgLatency ?? row.durationMs ?? [612, 748, 536, 1014, 1280][index % 5]),
    successRate: Number(row.successRate ?? [99.8, 99.5, 99.7, 97.6, 98.9][index % 5]),
    status: enabled ? riskLevel === 'HIGH' ? 'limited' : 'normal' : 'disabled'
  }
}

const normalizeGroup = (row, index) => {
  const relatedTools = toolRows.value.filter((tool) => String(tool.groupId) === String(row.id) || tool.groupId === row.groupName)
  return {
    ...row,
    id: row.id || index + 1,
    groupName: row.groupName || row.name || `技能包 ${index + 1}`,
    description: row.description || '暂无描述',
    enabled: Boolean(row.enabled ?? true),
    activeByDefault: Boolean(row.activeByDefault ?? false),
    tools: Number(row.tools ?? row.toolCount ?? relatedTools.length ?? [8, 6, 5, 7][index % 4]),
    agents: Number(row.agents ?? row.agentCount ?? [15, 12, 18, 10][index % 4])
  }
}

const resetCreateForm = () => {
  Object.assign(createForm, {
    toolName: '',
    toolNameExplain: '',
    description: '',
    toolType: 'HTTP',
    groupId: '',
    permissionCode: '',
    riskLevel: 'LOW',
    readOnly: true,
    concurrency: false,
    enabled: true,
    timeoutMs: 30000,
    maxRetries: 1,
    methodName: '',
    className: '',
    beanName: ''
  })
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const handleCreateTool = async () => {
  if (!createForm.toolName.trim()) {
    ElMessage.warning('请输入工具名称')
    return
  }
  creating.value = true
  try {
    await createTool({ ...createForm })
    ElMessage.success('工具创建成功')
    createDialogVisible.value = false
    await loadDashboard()
  } finally {
    creating.value = false
  }
}

const openLogDialog = () => {
  logDialogVisible.value = true
}

const configureTool = (tool) => {
  ElMessage.info(`准备配置 ${tool.toolName}`)
}

const loadDashboard = async () => {
  loading.value = true
  try {
    const [toolResult, groupResult, logResult, permissionResult] = await Promise.allSettled([
      listTools(),
      listToolGroups(),
      listToolCallLogs(),
      listToolPermissions()
    ])
    tools.value = toolResult.status === 'fulfilled' && Array.isArray(toolResult.value) ? toolResult.value : []
    groups.value = groupResult.status === 'fulfilled' && Array.isArray(groupResult.value) ? groupResult.value : []
    logs.value = logResult.status === 'fulfilled' && Array.isArray(logResult.value) ? logResult.value : []
    permissions.value = permissionResult.status === 'fulfilled' && Array.isArray(permissionResult.value) ? permissionResult.value : []
  } finally {
    loading.value = false
  }
}

const riskClass = (riskLevel) => {
  const value = String(riskLevel || '').toUpperCase()
  if (value === 'HIGH') {
    return 'high'
  }
  if (value === 'MEDIUM') {
    return 'medium'
  }
  return 'low'
}

const riskLabel = (riskLevel) => {
  const map = {
    LOW: '低风险',
    MEDIUM: '中风险',
    HIGH: '高风险'
  }
  return map[String(riskLevel || '').toUpperCase()] || riskLevel || '-'
}

const statusLabel = (status) => {
  const map = {
    normal: '正常',
    limited: '受限',
    disabled: '停用'
  }
  return map[status] || status
}

const behaviorType = (behavior) => {
  const value = String(behavior || '').toUpperCase()
  if (value === 'DENY') {
    return 'danger'
  }
  if (value === 'ASK') {
    return 'warning'
  }
  return 'success'
}

const formatNumber = (value) => Number(value || 0).toLocaleString('en-US')

const formatCompact = (value) => {
  const number = Number(value || 0)
  if (number >= 1000000) {
    return `${(number / 1000000).toFixed(1)}M`
  }
  if (number >= 1000) {
    return `${(number / 1000).toFixed(number >= 10000 ? 1 : 0)}K`
  }
  return String(number)
}

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="tool-console">
    <div class="tool-hero">
      <div>
        <h2>工具与技能</h2>
        <p>集中管理 Agent 可调用的工具、技能包与权限策略，安全扩展智能体执行能力。</p>
      </div>
    </div>

    <div class="tool-metrics">
      <article v-for="item in metrics" :key="item.label" class="tool-metric">
        <span class="metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small :class="{ positive: item.positive, danger: item.danger }">{{ item.sub }}</small>
        </div>
      </article>
    </div>

    <div class="tool-dashboard">
      <section class="tool-library-panel">
        <el-button class="tabs-create-tool" type="primary" :icon="Plus" @click="openCreateDialog">新建工具</el-button>
        <el-tabs v-model="activeTab" class="tool-tabs">
          <el-tab-pane label="工具库" name="tools">
            <div class="tool-filter-bar">
              <el-input
                v-model="queryParams.keyword"
                clearable
                :prefix-icon="Search"
                placeholder="搜索工具名称或编码..."
              />
              <el-select v-model="queryParams.category" clearable placeholder="全部分类">
                <el-option v-for="type in toolTypeOptions" :key="type" :label="type" :value="type" />
              </el-select>
              <el-select v-model="queryParams.status" clearable placeholder="全部状态">
                <el-option label="正常" value="normal" />
                <el-option label="受限" value="limited" />
                <el-option label="停用" value="disabled" />
              </el-select>
              <div class="view-switch">
                <el-tooltip content="列表视图" placement="top">
                  <button :class="{ active: viewMode === 'list' }" type="button" @click="viewMode = 'list'">
                    <el-icon><List /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="卡片视图" placement="top">
                  <button :class="{ active: viewMode === 'grid' }" type="button" @click="viewMode = 'grid'">
                    <el-icon><Grid /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </div>

            <div v-if="viewMode === 'list'" class="tool-list">
              <article v-for="tool in filteredTools" :key="tool.id" class="tool-row">
                <span class="tool-mark" :class="riskClass(tool.riskLevel)">
                  <el-icon><component :is="tool.readOnly ? Search : Setting" /></el-icon>
                </span>
                <div class="tool-main">
                  <h4>{{ tool.toolName }}</h4>
                  <p>{{ tool.toolNameExplain }}</p>
                </div>
                <div class="tool-call">
                  <span>调用</span>
                  <strong>{{ formatNumber(tool.calls) }}</strong>
                </div>
                <div class="tool-agent">{{ tool.agents }} 个 Agent</div>
                <span class="status-pill-mini" :class="tool.status">
                  <i />
                  {{ statusLabel(tool.status) }}
                </span>
                <el-tooltip :content="riskLabel(tool.riskLevel)" placement="top">
                  <span class="risk-shield" :class="riskClass(tool.riskLevel)">
                    <el-icon><Finished /></el-icon>
                  </span>
                </el-tooltip>
                <el-button link type="primary" @click="configureTool(tool)">配置</el-button>
                <el-dropdown trigger="click">
                  <button class="more-button" type="button" aria-label="更多操作">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="configureTool(tool)">配置策略</el-dropdown-item>
                      <el-dropdown-item>查看调用</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </article>
            </div>

            <div v-else class="tool-card-grid">
              <article v-for="tool in filteredTools" :key="tool.id" class="tool-card">
                <div class="tool-card-head">
                  <span class="tool-mark" :class="riskClass(tool.riskLevel)">
                    <el-icon><component :is="tool.readOnly ? Search : Setting" /></el-icon>
                  </span>
                  <span class="status-pill-mini" :class="tool.status">{{ statusLabel(tool.status) }}</span>
                </div>
                <h4>{{ tool.toolName }}</h4>
                <p>{{ tool.toolNameExplain }}</p>
                <div class="tool-card-stats">
                  <div><span>调用</span><strong>{{ formatNumber(tool.calls) }}</strong></div>
                  <div><span>Agent</span><strong>{{ tool.agents }}</strong></div>
                  <div><span>耗时</span><strong>{{ tool.avgLatency }}ms</strong></div>
                </div>
              </article>
            </div>

            <div class="table-footer">
              <span>共 {{ filteredTools.length }} 项</span>
              <div class="pager-mock">
                <button disabled type="button">&lt;</button>
                <strong>1</strong>
                <button disabled type="button">&gt;</button>
                <span>10 条/页</span>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="技能包" name="skills">
            <div class="skill-grid">
              <article v-for="group in groupRows" :key="group.id" class="skill-card">
                <span class="skill-icon"><el-icon><Box /></el-icon></span>
                <div>
                  <h4>{{ group.groupName }}</h4>
                  <p>{{ group.description }}</p>
                  <small>工具 {{ group.tools }} 个 · Agent {{ group.agents }} 个</small>
                </div>
                <el-button :icon="ArrowRight" />
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane label="MCP 工具" name="mcp">
            <div class="mcp-list">
              <article v-for="tool in mcpTools" :key="tool.id" class="mcp-row">
                <span><el-icon><Link /></el-icon></span>
                <div>
                  <h4>{{ tool.toolName }}</h4>
                  <p>{{ tool.description }}</p>
                </div>
                <el-tag type="success">在线</el-tag>
                <el-button text type="primary">配置</el-button>
              </article>
              <el-empty v-if="!mcpTools.length" description="暂无 MCP 工具" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="权限策略" name="permissions">
            <div class="permission-list">
              <article v-for="item in permissionRows" :key="item.id" class="permission-row">
                <span class="permission-icon"><el-icon><Key /></el-icon></span>
                <div>
                  <h4>{{ item.toolName }}</h4>
                  <p>{{ item.description }}</p>
                </div>
                <span>{{ item.roleCode }}</span>
                <el-tag :type="behaviorType(item.behavior)">{{ item.behavior }}</el-tag>
              </article>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>

      <aside class="tool-side">
        <section class="side-panel">
          <div class="side-head">
            <h3>权限变更记录</h3>
            <el-button link type="primary">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
          </div>
          <div class="permission-change-list">
            <div v-for="item in permissionRows" :key="item.id">
              <span class="permission-icon"><el-icon><Lock /></el-icon></span>
              <div>
                <strong>{{ item.toolName }}</strong>
                <small>{{ item.roleCode }} · {{ item.updateTime }}</small>
              </div>
              <el-tag :type="behaviorType(item.behavior)">{{ item.behavior }}</el-tag>
            </div>
          </div>
        </section>

        <section class="side-panel">
          <div class="side-head">
            <h3>最近失败调用</h3>
            <el-button link type="primary">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
          </div>
          <div class="failure-list">
            <div v-for="item in failedLogs" :key="item.id" class="failure-row">
              <span><el-icon><Warning /></el-icon></span>
              <div>
                <strong>{{ item.toolName }}</strong>
                <small>{{ item.agentName }} · {{ item.reason }}</small>
              </div>
              <em>{{ item.durationMs }}ms</em>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      title="新建工具"
      width="760px"
      destroy-on-close
      class="tool-dialog"
    >
      <el-form label-width="112px" class="tool-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工具名称" required>
              <el-input v-model="createForm.toolName" placeholder="如：query_order" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示说明">
              <el-input v-model="createForm.toolNameExplain" placeholder="如：查询订单及明细信息" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工具类型">
              <el-select v-model="createForm.toolType">
                <el-option label="HTTP" value="HTTP" />
                <el-option label="JAVA_BEAN" value="JAVA_BEAN" />
                <el-option label="MCP" value="MCP" />
                <el-option label="SQL" value="SQL" />
                <el-option label="RPC" value="RPC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技能包">
              <el-select v-model="createForm.groupId" clearable placeholder="选择技能包">
                <el-option v-for="group in groupRows" :key="group.id" :label="group.groupName" :value="group.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限码">
              <el-input v-model="createForm.permissionCode" placeholder="如：tool:order:query" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级">
              <el-select v-model="createForm.riskLevel">
                <el-option label="低风险" value="LOW" />
                <el-option label="中风险" value="MEDIUM" />
                <el-option label="高风险" value="HIGH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时毫秒">
              <el-input-number v-model="createForm.timeoutMs" :min="1000" :step="1000" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大重试">
              <el-input-number v-model="createForm.maxRetries" :min="0" :max="5" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="调用特性">
              <div class="switch-row">
                <el-checkbox v-model="createForm.readOnly">只读工具</el-checkbox>
                <el-checkbox v-model="createForm.concurrency">允许并发</el-checkbox>
                <el-checkbox v-model="createForm.enabled">启用</el-checkbox>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="描述工具能力、参数约束和适用场景" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateTool">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="logDialogVisible"
      title="调用日志"
      width="900px"
      destroy-on-close
      class="tool-dialog"
    >
      <el-table :data="logRows" stripe>
        <el-table-column prop="toolName" label="工具" min-width="150" />
        <el-table-column prop="agentName" label="Agent" min-width="150" />
        <el-table-column prop="permissionBehavior" label="权限" width="100">
          <template #default="{ row }">
            <el-tag :type="behaviorType(row.permissionBehavior)">{{ row.permissionBehavior }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="successStatus" label="状态" width="100" />
        <el-table-column prop="durationMs" label="耗时" width="100">
          <template #default="{ row }">{{ row.durationMs }}ms</template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" min-width="170" />
      </el-table>
    </el-dialog>
  </section>
</template>

<style scoped>
.tool-console {
  display: grid;
  min-height: calc(100vh - 135px);
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 18px;
}

.tool-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.tool-hero h2 {
  margin: 14px 0 8px;
  color: #071f40;
  font-size: 34px;
  font-weight: 850;
  letter-spacing: 0;
}

.tool-hero p {
  margin: 0;
  color: #526b87;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

.tool-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.tool-metric,
.tool-library-panel,
.side-panel {
  border: 1px solid #d7e5f8;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 38px rgba(48, 94, 151, 0.08);
}

.tool-metric {
  display: flex;
  min-height: 126px;
  align-items: center;
  gap: 18px;
  padding: 20px 22px;
}

.metric-icon {
  display: grid;
  width: 58px;
  height: 58px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 13px;
  color: #2f75ff;
  background: #ecf4ff;
  font-size: 28px;
}

.metric-icon.indigo {
  color: #5c6cff;
  background: #eef1ff;
}

.metric-icon.cyan {
  color: #0b95d8;
  background: #e9f8ff;
}

.metric-icon.green {
  color: #168354;
  background: #eaf8ef;
}

.tool-metric > div > span {
  display: block;
  color: #667d99;
  font-size: 13px;
}

.tool-metric strong {
  display: block;
  margin-top: 8px;
  color: #0a2547;
  font-size: 30px;
  font-weight: 850;
  line-height: 1;
}

.tool-metric small {
  display: block;
  margin-top: 12px;
  color: #6d819b;
  font-size: 12px;
  font-weight: 750;
}

.tool-metric small.positive {
  color: #22a86b;
}

.tool-metric small.danger {
  color: #ef6673;
}

.tool-dashboard {
  display: grid;
  grid-template-columns: minmax(620px, 1fr) minmax(330px, 0.6fr);
  align-items: stretch;
  gap: 18px;
  min-height: 0;
}

.tool-library-panel {
  position: relative;
  min-width: 0;
  overflow: hidden;
}

.tool-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 150px 0 22px;
  border-bottom: 1px solid #dce8f5;
}

.tabs-create-tool {
  position: absolute;
  z-index: 2;
  top: 10px;
  right: 22px;
}

.tool-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.tool-tabs :deep(.el-tabs__item) {
  height: 56px;
  color: #667d99;
  font-size: 15px;
  font-weight: 800;
}

.tool-tabs :deep(.el-tabs__item.is-active) {
  color: #2f75ff;
}

.tool-filter-bar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 160px 160px auto;
  gap: 12px;
  padding: 22px;
}

.view-switch {
  display: inline-flex;
  overflow: hidden;
  height: 40px;
  border: 1px solid #d3e2f6;
  border-radius: 10px;
  background: #ffffff;
}

.view-switch button {
  display: grid;
  width: 44px;
  height: 38px;
  place-items: center;
  border: 0;
  color: #6d819b;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
}

.view-switch button.active {
  color: #2f75ff;
  background: #edf4ff;
}

.tool-list {
  display: grid;
  gap: 10px;
  padding: 0 22px 18px;
}

.tool-row {
  display: grid;
  grid-template-columns: 54px minmax(180px, 1fr) 86px 90px 74px 36px 56px 34px;
  align-items: center;
  gap: 14px;
  min-height: 88px;
  padding: 14px 16px;
  border: 1px solid #e0eaf6;
  border-radius: 12px;
  background: #ffffff;
}

.tool-row:hover,
.tool-card:hover,
.skill-card:hover {
  border-color: #a9c9ff;
  box-shadow: 0 12px 28px rgba(47, 117, 255, 0.08);
}

.tool-mark {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 12px;
  color: #2f75ff;
  background: #edf4ff;
  font-size: 24px;
}

.tool-mark.low {
  color: #2f75ff;
  background: #edf4ff;
}

.tool-mark.medium {
  color: #c56a1c;
  background: #fff5e8;
}

.tool-mark.high {
  color: #e45765;
  background: #fff0f2;
}

.tool-main {
  min-width: 0;
}

.tool-main h4,
.tool-card h4,
.skill-card h4,
.mcp-row h4,
.permission-row h4 {
  overflow: hidden;
  margin: 0;
  color: #0a2547;
  font-size: 15px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-main p,
.tool-card p,
.skill-card p,
.mcp-row p,
.permission-row p {
  overflow: hidden;
  margin: 7px 0 0;
  color: #6d819b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-call span {
  display: block;
  color: #7e94ad;
  font-size: 12px;
}

.tool-call strong {
  display: block;
  margin-top: 4px;
  color: #203957;
  font-size: 17px;
}

.tool-agent {
  color: #405874;
  font-weight: 750;
}

.status-pill-mini {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 62px;
  height: 32px;
  border-radius: 8px;
  color: #168354;
  background: #eaf8ef;
  font-size: 12px;
  font-weight: 800;
}

.status-pill-mini i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.status-pill-mini.limited {
  color: #c56a1c;
  background: #fff5e8;
}

.status-pill-mini.disabled {
  color: #6d819b;
  background: #edf3f8;
}

.risk-shield {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #18a668;
  font-size: 24px;
}

.risk-shield.medium {
  color: #e18a12;
}

.risk-shield.high {
  color: #e45765;
}

.more-button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 0;
  border-radius: 8px;
  color: #405874;
  background: transparent;
  cursor: pointer;
}

.more-button:hover {
  background: #edf4ff;
  color: #2f75ff;
}

.tool-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 0 22px 18px;
}

.tool-card {
  border: 1px solid #e0eaf6;
  border-radius: 12px;
  background: #ffffff;
  padding: 16px;
}

.tool-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.tool-card-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 16px;
}

.tool-card-stats div {
  padding: 10px;
  border-radius: 9px;
  background: #f7fbff;
}

.tool-card-stats span {
  color: #7890aa;
  font-size: 11px;
}

.tool-card-stats strong {
  display: block;
  margin-top: 5px;
  color: #0a2547;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 22px 18px;
  color: #6d819b;
}

.pager-mock {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager-mock button,
.pager-mock strong,
.skill-card .el-button {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid #d3e2f6;
  border-radius: 8px;
  color: #2f75ff;
  background: #ffffff;
}

.pager-mock strong {
  border-color: #2f75ff;
  color: #ffffff;
  background: #2f75ff;
}

.pager-mock span {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #d3e2f6;
  border-radius: 8px;
  line-height: 32px;
}

.skill-grid,
.mcp-list,
.permission-list {
  display: grid;
  gap: 12px;
  padding: 22px;
}

.skill-card,
.mcp-row,
.permission-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 82px;
  padding: 14px 16px;
  border: 1px solid #e0eaf6;
  border-radius: 12px;
  background: #ffffff;
}

.skill-icon,
.mcp-row > span,
.permission-icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 12px;
  color: #5c6cff;
  background: #eef1ff;
  font-size: 22px;
}

.skill-card small {
  display: block;
  margin-top: 8px;
  color: #2f75ff;
  font-size: 12px;
  font-weight: 800;
}

.mcp-row {
  grid-template-columns: 48px minmax(0, 1fr) 76px 64px;
}

.permission-row {
  grid-template-columns: 48px minmax(0, 1fr) 110px 88px;
}

.tool-side {
  display: grid;
  grid-template-rows: minmax(150px, 0.42fr) minmax(260px, 0.58fr);
  align-content: stretch;
  gap: 18px;
}

.side-panel {
  min-height: 0;
  padding: 18px;
}

.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.side-head h3 {
  margin: 0;
  color: #0a2547;
  font-size: 17px;
  font-weight: 800;
}

.failure-list,
.permission-change-list {
  display: grid;
  margin-top: 14px;
}

.permission-change-list > div {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 60px;
  border-bottom: 1px solid #e3edf8;
}

.failure-row:last-child,
.permission-change-list > div:last-child {
  border-bottom: 0;
}

.failure-row strong,
.permission-change-list strong {
  overflow: hidden;
  color: #0a2547;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.failure-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 76px;
  align-items: center;
  gap: 12px;
  min-height: 60px;
  border-bottom: 1px solid #e3edf8;
}

.failure-row > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 10px;
  color: #e45765;
  background: #fff0f2;
}

.failure-row small,
.permission-change-list small {
  display: block;
  overflow: hidden;
  margin-top: 5px;
  color: #7890aa;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.failure-row em {
  color: #e45765;
  font-style: normal;
  font-weight: 800;
}

.permission-change-list .permission-icon {
  width: 36px;
  height: 36px;
  color: #2f75ff;
  background: #edf4ff;
  font-size: 18px;
}

.tool-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 22px;
  border-bottom: 1px solid #dce8f5;
}

.tool-dialog :deep(.el-dialog__body) {
  padding: 20px 22px;
}

.tool-form .el-select,
.tool-form .el-input,
.tool-form :deep(.el-input-number),
.tool-form :deep(.el-textarea) {
  width: 100%;
}

.switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
}

@media (max-width: 1320px) {
  .tool-dashboard {
    grid-template-columns: 1fr;
  }

  .tool-dashboard,
  .tool-side {
    min-height: 0;
  }

  .tool-side {
    grid-template-rows: none;
  }

  .tool-row {
    grid-template-columns: 54px minmax(220px, 1fr) 86px 90px 74px 36px 56px 34px;
  }
}

@media (max-width: 980px) {
  .tool-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .tool-metrics,
  .tool-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .tool-filter-bar {
    grid-template-columns: 1fr;
  }

  .tool-row {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .tool-call,
  .tool-agent,
  .status-pill-mini,
  .risk-shield,
  .tool-row > .el-button,
  .tool-row > .el-dropdown {
    grid-column: 2;
    justify-self: start;
  }

  .permission-change-list > div,
  .mcp-row,
  .permission-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .tool-hero h2 {
    font-size: 28px;
  }

  .tool-metrics,
  .tool-card-grid,
  .tool-card-stats {
    grid-template-columns: 1fr;
  }
}
</style>
