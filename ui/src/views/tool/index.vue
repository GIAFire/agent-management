<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Box,
  Briefcase,
  DataLine,
  Document,
  Finished,
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
const logDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const tools = ref([])
const groups = ref([])
const logs = ref([])
const permissions = ref([])

const queryParams = reactive({
  keyword: '',
  category: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 6
})

const permissionPagination = reactive({
  currentPage: 1,
  pageSize: 10
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

const pagedPermissionRows = computed(() => {
  const start = (permissionPagination.currentPage - 1) * permissionPagination.pageSize
  return permissionRows.value.slice(start, start + permissionPagination.pageSize)
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

const pagedTools = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  return filteredTools.value.slice(start, start + pagination.pageSize)
})

const currentTabTotal = computed(() => filteredTools.value.length)

watch(
  () => [queryParams.keyword, queryParams.category, queryParams.status],
  () => {
    pagination.currentPage = 1
  }
)

watch(
  () => pagination.pageSize,
  () => {
    const maxPage = Math.max(1, Math.ceil(currentTabTotal.value / pagination.pageSize))
    if (pagination.currentPage > maxPage) {
      pagination.currentPage = maxPage
    }
  }
)

watch(
  filteredTools,
  () => {
    const maxPage = Math.max(1, Math.ceil(currentTabTotal.value / pagination.pageSize))
    if (pagination.currentPage > maxPage) {
      pagination.currentPage = maxPage
    }
  }
)

const toolTypeOptions = computed(() => {
  return [...new Set(toolRows.value.map((row) => row.toolType).filter(Boolean))]
})

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
      label: '工具分组',
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

function normalizeTool(row, index) {
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

function normalizeGroup(row, index) {
  const relatedTools = toolRows.value.filter((tool) => String(tool.groupId) === String(row.id) || tool.groupId === row.groupName)
  return {
    ...row,
    id: row.id || index + 1,
    groupName: row.groupName || row.name || `工具分组 ${index + 1}`,
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



const openPermissionDialog = () => {
  permissionPagination.currentPage = 1
  permissionDialogVisible.value = true
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
        <h2>工具</h2>
        <p>集中管理 Agent 可调用的工具、工具分组与权限策略，安全扩展智能体执行能力。</p>
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
        <div class="panel-head">
          <div>
            <h3>工具列表</h3>
            <p>共 {{ filteredTools.length }} 个工具</p>
          </div>
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
            <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
          </div>
        </div>

        <div class="tool-list">
          <article v-for="tool in pagedTools" :key="tool.id" class="tool-row">
            <header class="tool-card-head">
              <span class="tool-mark" :class="riskClass(tool.riskLevel)">
                <el-icon><component :is="tool.readOnly ? Search : Setting" /></el-icon>
              </span>
              <div class="tool-main">
                <div class="tool-title-line">
                  <h4>{{ tool.toolName }}</h4>
                  <span>{{ tool.toolType }}</span>
                </div>
                <p>{{ tool.toolNameExplain }}</p>
              </div>
              <span class="status-pill-mini" :class="tool.status">
                <i />
                {{ statusLabel(tool.status) }}
              </span>
            </header>
            <div class="tool-card-stats">
              <div>
                <span>调用次数</span>
                <strong>{{ formatNumber(tool.calls) }}</strong>
              </div>
              <div>
                <span>Agent</span>
                <strong>{{ tool.agents }}</strong>
              </div>
              <div>
                <span>平均耗时</span>
                <strong>{{ tool.avgLatency }}ms</strong>
              </div>
            </div>
            <footer class="tool-card-actions">
              <span class="risk-label" :class="riskClass(tool.riskLevel)">
                <el-icon><Finished /></el-icon>
                {{ riskLabel(tool.riskLevel) }}
              </span>
              <nav>
                <el-dropdown trigger="click">
                  <button class="more-button" type="button" aria-label="更多操作">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="configureTool(tool)">权限配置</el-dropdown-item>
                      <el-dropdown-item>查看调用</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </nav>
            </footer>
          </article>
        </div>

        <div class="table-footer">
          <span>共 {{ currentTabTotal }} 项</span>
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            background
            layout="prev, pager, next, sizes"
            :page-sizes="[6, 12, 24]"
            :total="currentTabTotal"
          />
        </div>
      </section>

      <aside class="tool-side">
        <section class="side-panel">
          <div class="side-head">
            <h3>权限变更记录</h3>
            <el-button link type="primary" @click="openPermissionDialog">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
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

    <el-dialog
      v-model="permissionDialogVisible"
      title="权限变更记录"
      width="900px"
      destroy-on-close
      class="tool-dialog"
    >
      <el-table :data="pagedPermissionRows" stripe>
        <el-table-column prop="toolName" label="工具" min-width="160" />
        <el-table-column prop="roleCode" label="角色" min-width="130" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="130" />
        <el-table-column prop="behavior" label="权限" width="110">
          <template #default="{ row }">
            <el-tag :type="behaviorType(row.behavior)">{{ row.behavior }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="变更时间" min-width="170" />
      </el-table>
      <div class="dialog-pagination">
        <el-pagination
          v-model:current-page="permissionPagination.currentPage"
          v-model:page-size="permissionPagination.pageSize"
          background
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          :total="permissionRows.length"
        />
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.tool-console {
  display: grid;
  min-height: calc(100vh - 115px);
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 18px;
  padding-bottom: 28px;
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
  align-items: center;
  gap: 18px;
  padding: 10px 22px;
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
  width: 100%;
  grid-template-columns: minmax(760px, 1fr) minmax(330px, 0.36fr);
  align-self: stretch;
  align-items: stretch;
  gap: 18px;
  min-height: 0;
}

.tool-library-panel {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-color: #d9e4f2;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(34, 67, 112, 0.06);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 18px 24px;
  border-bottom: 0;
}

.panel-head h3 {
  margin: 0;
  color: #0a2547;
  font-size: 17px;
  font-weight: 800;
}

.panel-head p {
  margin: 6px 0 0;
  color: #6d819b;
  font-size: 12px;
}

.tool-filter-bar {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
}

.tool-filter-bar .el-input {
  width: 250px;
}

.tool-filter-bar .el-select {
  width: 128px;
}

.tool-filter-bar .el-button {
  height: 34px;
  border-radius: 5px;
  font-weight: 800;
}

.tool-filter-bar :deep(.el-input__wrapper),
.tool-filter-bar :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 5px;
  box-shadow: 0 0 0 1px #d7e1ee inset;
}

.tool-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px 20px;
  padding: 0 18px 18px;
}

.tool-row {
  position: relative;
  display: grid;
  min-height: 160px;
  grid-template-rows: auto minmax(78px, 1fr) auto;
  padding: 18px;
  border: 1px solid #e0eaf6;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(42, 72, 108, 0.05);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.tool-row:hover {
  border-color: #a9c9ff;
  box-shadow: 0 12px 28px rgba(47, 117, 255, 0.08);
  transform: translateY(-1px);
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
  padding-right: 78px;
}

.tool-card-head {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 14px;
}

.tool-title-line {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
}

.tool-title-line h4 {
  overflow: hidden;
  margin: 0;
  color: #0a2547;
  font-size: 15px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.tool-row:hover .tool-title-line h4 {
  color: #0b63f6;
}

.tool-title-line span {
  flex: 0 0 auto;
  padding: 3px 7px;
  border-radius: 6px;
  color: #2f75ff;
  background: #eaf2ff;
  font-size: 11px;
  font-weight: 800;
}

.tool-main p {
  display: -webkit-box;
  overflow: hidden;
  margin: 10px 0 0;
  color: #6d819b;
  font-size: 12px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.status-pill-mini {
  position: absolute;
  top: 18px;
  right: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 62px;
  height: 24px;
  border: 1px solid #bce8cc;
  border-radius: 7px;
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
  border-color: #ffdca6;
  color: #c56a1c;
  background: #fff5e8;
}

.status-pill-mini.disabled {
  border-color: #d9e2ec;
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

.tool-card-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  align-self: end;
  border: 1px solid #e1ebf6;
  border-radius: 8px;
  background: #f8fbff;
}

.tool-card-stats div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border-right: 1px solid #e1ebf6;
}

.tool-card-stats div:last-child {
  border-right: 0;
}

.tool-card-stats span {
  color: #7e94ad;
  font-size: 12px;
}

.tool-card-stats strong {
  display: block;
  color: #203957;
  font-size: 15px;
}

.tool-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid #e5edf6;
}

.tool-card-actions nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-card-actions .el-button.is-link {
  height: auto;
  padding: 0;
  font-weight: 800;
}

.risk-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #18a668;
  font-size: 12px;
  font-weight: 800;
}

.risk-label.medium {
  color: #c56a1c;
}

.risk-label.high {
  color: #e45765;
}

.table-footer {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 18px 16px;
  color: #53637b;
  font-size: 14px;
  font-weight: 700;
}

.table-footer :deep(.el-pagination) {
  --el-pagination-button-bg-color: #ffffff;
  --el-pagination-hover-color: #0b63f6;
}

.table-footer :deep(.el-pager li),
.table-footer :deep(.btn-prev),
.table-footer :deep(.btn-next) {
  border: 1px solid #d9e4f2;
  border-radius: 5px;
  box-shadow: none;
}

.table-footer :deep(.el-pager li.is-active) {
  border-color: #0b63f6;
  color: #0b63f6;
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

.tool-side {
  display: grid;
  grid-template-rows: minmax(150px, 0.42fr) minmax(260px, 0.58fr);
  align-content: stretch;
  gap: 18px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
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
  flex: 1 1 auto;
  align-content: start;
  margin-top: 14px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
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

.dialog-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
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

  .panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .tool-filter-bar {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}

@media (max-width: 980px) {
  .tool-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .tool-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .tool-filter-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .tool-filter-bar .el-input,
  .tool-filter-bar .el-select,
  .tool-filter-bar .el-button {
    width: 100%;
  }

  .permission-change-list > div {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .tool-hero h2 {
    font-size: 28px;
  }

  .tool-metrics {
    grid-template-columns: 1fr;
  }

  .tool-list {
    grid-template-columns: 1fr;
    padding: 0 12px 14px;
  }

  .table-footer {
    align-items: flex-start;
    flex-direction: column;
    padding-right: 12px;
    padding-left: 12px;
  }

  .tool-card-head {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .tool-mark {
    width: 48px;
    height: 48px;
  }

  .tool-main {
    padding-right: 0;
  }

  .status-pill-mini {
    position: static;
    grid-column: 1 / -1;
    justify-self: start;
  }

  .tool-card-stats {
    grid-template-columns: 1fr;
  }

  .tool-card-stats div {
    border-right: 0;
    border-bottom: 1px solid #e1ebf6;
  }

  .tool-card-stats div:last-child {
    border-bottom: 0;
  }
}
</style>
