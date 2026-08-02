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
  disableToolPermission,
  getToolMetrics,
  listToolGroups,
  listTools,
  pageToolCallLogs,
  pageToolPermissions,
  saveToolPermission
} from '@/axios/tool'
import { listRole } from '@/axios/role'

const loading = ref(false)
const creating = ref(false)
const logDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const tools = ref([])
const groups = ref([])
const logs = ref([])
const permissions = ref([])
const metricData = ref(null)
const roles = ref([])
const activeTool = ref(null)
const logFilterStatus = ref('')
const logTotal = ref(0)
const permissionTotal = ref(0)
const permissionEditorVisible = ref(false)

const queryParams = reactive({
  keyword: '',
  category: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 8
})

const permissionPagination = reactive({
  currentPage: 1,
  pageSize: 10
})

const logPagination = reactive({
  currentPage: 1,
  pageSize: 10
})

const permissionForm = reactive({
  id: '',
  toolId: '',
  roleId: '',
  behavior: 'ASK',
  description: '',
  ruleContent: '',
  status: 1
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

const toolRows = computed(() => {
  return tools.value.map((row, index) => normalizeTool(row, index))
})

const groupRows = computed(() => {
  return groups.value.map((row, index) => normalizeGroup(row, index))
})

const logRows = computed(() => {
  return logs.value.map((row) => ({
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
  return permissions.value.map((row) => ({
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

watch(
  () => [permissionPagination.currentPage, permissionPagination.pageSize],
  () => {
    if (permissionDialogVisible.value) loadPermissionPage()
  }
)

watch(
  () => [logPagination.currentPage, logPagination.pageSize],
  () => {
    if (logDialogVisible.value) loadLogPage()
  }
)

const toolTypeOptions = computed(() => {
  return [...new Set(toolRows.value.map((row) => row.toolType).filter(Boolean))]
})

const failedLogs = computed(() => {
  return logRows.value.filter((row) => String(row.successStatus).toUpperCase() === 'FAILED').slice(0, 4)
})

const metrics = computed(() => {
  const data = metricData.value || {}
  const callChange = data.callChangePercent
  const successRate = data.successRate

  return [
    {
      label: '可用工具',
      value: data.availableTools ?? '--',
      sub: `${data.enabledTools ?? 0} 个已启用`,
      icon: Briefcase,
      tone: 'blue',
      positive: true
    },
    {
      label: '工具分组',
      value: data.enabledGroups ?? '--',
      sub: '已启用分组',
      icon: Box,
      tone: 'indigo'
    },
    {
      label: '今日调用',
      value: data.todayCalls == null ? '--' : formatCompact(data.todayCalls),
      sub: callChange == null ? '昨日同期无调用' : `较昨日同期 ${callChange >= 0 ? '+' : ''}${callChange}%`,
      icon: DataLine,
      tone: 'cyan',
      positive: true
    },
    {
      label: '成功率',
      value: successRate == null ? '--' : `${Number(successRate).toFixed(1)}%`,
      sub: `失败 ${data.failedCalls ?? 0} 次`,
      icon: Finished,
      tone: 'green',
      danger: true
    }
  ]
})

function normalizeTool(row, index) {
  const calls = Number(row.calls ?? row.callCount ?? row.invokeCount ?? 0)
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
    calls,
    agents: Number(row.agents ?? row.agentCount ?? 0),
    avgLatency: Number(row.avgLatency ?? row.durationMs ?? 0),
    successRate: row.successRate == null ? null : Number(row.successRate),
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
    tools: Number(row.tools ?? row.toolCount ?? relatedTools.length),
    agents: Number(row.agents ?? row.agentCount ?? 0)
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



const loadPermissionPage = async () => {
  const result = await pageToolPermissions({
    current: permissionPagination.currentPage,
    size: permissionPagination.pageSize,
    toolId: activeTool.value?.id || undefined
  })
  permissions.value = result?.records || []
  permissionTotal.value = Number(result?.total || 0)
}

const openPermissionDialog = async () => {
  activeTool.value = null
  permissionPagination.currentPage = 1
  permissionDialogVisible.value = true
  await loadPermissionPage()
}

const resetPermissionForm = (tool, permission = null) => {
  Object.assign(permissionForm, {
    id: permission?.id || '',
    toolId: tool.id,
    roleId: permission?.roleId || '',
    behavior: permission?.behavior || 'ASK',
    description: permission?.description || '',
    ruleContent: permission?.ruleContent || '',
    status: Number(permission?.status ?? 1)
  })
}

const configureTool = async (tool, permission = null) => {
  activeTool.value = tool
  resetPermissionForm(tool, permission)
  if (!roles.value.length) {
    roles.value = await listRole()
  }
  permissionEditorVisible.value = true
}

const submitPermission = async () => {
  await saveToolPermission({ ...permissionForm })
  ElMessage.success('权限规则已保存')
  permissionEditorVisible.value = false
  const returnToPermissionList = permissionDialogVisible.value
  activeTool.value = null
  await loadDashboard()
  if (returnToPermissionList) {
    await loadPermissionPage()
  }
}

const disablePermission = async (permission) => {
  await disableToolPermission(permission.id)
  ElMessage.success('权限规则已停用')
  await loadPermissionPage()
}

const loadLogPage = async () => {
  const result = await pageToolCallLogs({
    current: logPagination.currentPage,
    size: logPagination.pageSize,
    toolId: activeTool.value?.id || undefined,
    successStatus: logFilterStatus.value || undefined
  })
  logs.value = result?.records || []
  logTotal.value = Number(result?.total || 0)
}

const openToolCalls = async (tool = null, status = '') => {
  activeTool.value = tool
  logFilterStatus.value = status
  logPagination.currentPage = 1
  logDialogVisible.value = true
  await loadLogPage()
}

const loadDashboard = async () => {
  loading.value = true
  try {
    const [toolResult, groupResult, metricResult, logResult, permissionResult] = await Promise.all([
      listTools(),
      listToolGroups(),
      getToolMetrics(),
      pageToolCallLogs({ current: 1, size: 4, successStatus: 'FAILED' }),
      pageToolPermissions({ current: 1, size: 4 })
    ])
    tools.value = Array.isArray(toolResult) ? toolResult : []
    groups.value = Array.isArray(groupResult) ? groupResult : []
    metricData.value = metricResult || null
    logs.value = logResult?.records || []
    permissions.value = permissionResult?.records || []
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
  <section v-loading="loading" class="tool-console management-page">
    <div class="tool-metrics management-metrics">
      <article v-for="item in metrics" :key="item.label" class="tool-metric management-metric-card">
        <div class="metric-icon management-metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <small>{{ item.label }}</small>
          <strong>{{ item.value }}</strong>
          <p :class="{ positive: item.positive, danger: item.danger }">{{ item.sub }}</p>
        </div>
      </article>
    </div>

    <div class="tool-dashboard management-content-grid">
      <section class="tool-library-panel management-panel">
        <div class="panel-head management-panel-title">
          <div>
            <h3>工具列表</h3>
            <p>共 {{ filteredTools.length }} 个工具</p>
          </div>
          <div class="tool-filter-bar management-filter-bar">
            <el-input
              v-model="queryParams.keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索工具名称或编码..."
            />
            <el-select v-model="queryParams.category" clearable placeholder="全部分类">
              <el-option v-for="type in toolTypeOptions" :key="type" :label="type" :value="type" />
            </el-select>
            <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
          </div>
        </div>

        <div v-if="pagedTools.length" class="tool-list">
          <article v-for="tool in pagedTools" :key="tool.id" class="tool-row management-data-card">
            <el-dropdown class="management-card-menu" trigger="click">
              <button class="management-card-menu-button" type="button" aria-label="工具操作">
                <el-icon class="management-card-menu-icon"><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="configureTool(tool)">权限配置</el-dropdown-item>
                  <el-dropdown-item @click="openToolCalls(tool)">查看调用</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <header class="tool-card-head">
              <span class="tool-mark" :class="riskClass(tool.riskLevel)">
                <el-icon><component :is="tool.readOnly ? Search : Setting" /></el-icon>
              </span>
              <div class="tool-main">
                <div class="tool-title-line">
                  <h4>{{ tool.toolName }}</h4>
                </div>
                <p>{{ tool.description }}</p>
              </div>
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
          </article>
        </div>
        <el-empty v-else description="暂无符合条件的工具" />

        <div class="table-footer">
          <span>共 {{ currentTabTotal }} 项</span>
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            class="management-pagination"
            background
            layout="prev, pager, next, sizes"
            :page-sizes="[8, 16, 32]"
            :total="currentTabTotal"
          />
        </div>
      </section>

      <aside class="tool-side management-side-column">
        <section class="side-panel management-side-card">
          <div class="side-head">
            <h3>最近更新的权限规则</h3>
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

        <section class="side-panel management-side-card">
          <div class="side-head">
            <h3>最近失败调用</h3>
            <el-button link type="primary" @click="openToolCalls(null, 'FAILED')">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
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
      <div class="dialog-pagination">
        <el-pagination
          v-model:current-page="logPagination.currentPage"
          v-model:page-size="logPagination.pageSize"
          background
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          :total="logTotal"
        />
      </div>
    </el-dialog>

    <el-dialog
      v-model="permissionDialogVisible"
      title="权限规则"
      width="900px"
      destroy-on-close
      class="tool-dialog"
    >
      <el-table :data="permissionRows" stripe>
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
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="configureTool({ id: row.toolId, toolName: row.toolName }, row)">编辑</el-button>
            <el-button v-if="row.status === 1" link type="danger" @click="disablePermission(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-pagination">
        <el-pagination
          v-model:current-page="permissionPagination.currentPage"
          v-model:page-size="permissionPagination.pageSize"
          background
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          :total="permissionTotal"
        />
      </div>
    </el-dialog>

    <el-dialog
      v-model="permissionEditorVisible"
      :title="`${permissionForm.id ? '编辑' : '新增'}权限规则 · ${activeTool?.toolName || ''}`"
      width="620px"
      destroy-on-close
      class="tool-dialog"
    >
      <el-form label-width="100px">
        <el-form-item label="角色" required>
          <el-select v-model="permissionForm.roleId" :disabled="Boolean(permissionForm.id)" style="width: 100%">
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="`${role.roleName} (${role.roleCode})`"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="行为" required>
          <el-select v-model="permissionForm.behavior" style="width: 100%">
            <el-option v-for="behavior in ['ALLOW', 'DENY', 'ASK', 'PASSTHROUGH']" :key="behavior" :value="behavior" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="permissionForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="permissionForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionEditorVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!permissionForm.roleId" @click="submitPermission">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
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
  padding-right: 36px;
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

}

@media (max-width: 980px) {
  .tool-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .permission-change-list > div {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
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

<style scoped src="../../assets/management-page.css"></style>
