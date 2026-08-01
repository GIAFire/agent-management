<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  DataLine,
  Delete,
  Edit,
  Grid,
  Menu,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  Stopwatch,
  User
} from '@element-plus/icons-vue'
import {
  createSubagent,
  deleteSubagent,
  getSubagentMetrics,
  listLocalAgentOptions,
  listRecentSubagentTasks,
  pageSubagents,
  updateSubagent
} from '@/axios/subagent'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建子智能体')
const viewMode = ref('grid')
const subagents = ref([])
const tasks = ref([])
const exceptionTaskRows = ref([])
const metricData = ref(null)
const localAgentOptions = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(6)

const queryParams = reactive({
  keyword: '',
  type: '',
  status: '',
  sourceAvailable: ''
})

const form = reactive({
  id: null,
  subagentCode: '',
  subagentName: '',
  description: '',
  sourceType: 1,
  localAgentId: '',
  remoteUrl: '',
  protocolType: 1,
  enabled: 1,
  remark: '',
  headers: []
})

const subagentRows = computed(() => {
  return subagents.value.map((row, index) => normalizeSubagent(row, index))
})

const taskRows = computed(() => {
  return tasks.value.map((row, index) => normalizeTask(row, index))
})

const typeOptions = computed(() => {
  return [
    { label: '本地子智能体', value: 1 },
    { label: '远程子智能体', value: 2 }
  ]
})

const filteredRows = computed(() => {
  return subagentRows.value
})

const pagedRows = computed(() => {
  return filteredRows.value
})

const recentTasks = computed(() => taskRows.value)

const exceptionTasks = computed(() => exceptionTaskRows.value.map((row, index) => normalizeTask(row, index)))

const metrics = computed(() => {
  const data = metricData.value || {}
  const change = data.delegationChangePercent

  return [
    {
      label: '子智能体总数',
      value: data.total ?? '--',
      sub: `${data.enabled ?? 0} 个已启用`,
      icon: User,
      tone: 'blue'
    },
    {
      label: '今日委派',
      value: data.todayDelegations == null ? '--' : formatNumber(data.todayDelegations),
      sub: change == null ? '昨日同期无委派' : `较昨日同期 ${change >= 0 ? '+' : ''}${change}%`,
      icon: DataLine,
      tone: 'cyan',
      positive: true
    },
    {
      label: '执行成功率',
      value: data.successRate == null ? '--' : `${Number(data.successRate).toFixed(1)}%`,
      sub: `异常 ${data.unsuccessfulTasks ?? 0} 次`,
      icon: CircleCheck,
      tone: 'indigo'
    },
    {
      label: '平均执行时长',
      value: data.averageDurationMs == null ? '--' : formatDuration(data.averageDurationMs),
      sub: '今日已结束任务',
      icon: Stopwatch,
      tone: 'green',
      positive: true
    }
  ]
})

watch(
  () => [queryParams.type, queryParams.status, queryParams.sourceAvailable],
  () => {
    currentPage.value = 1
    loadSubagentPage()
  }
)

watch(
  () => [currentPage.value, pageSize.value],
  () => {
    loadSubagentPage()
  }
)

function normalizeSubagent(row, index) {
  const enabled = Number(row.enabled ?? 1) === 1
  const runtimeType = Number(row.sourceType) === 1 ? '本地' : '远程'
  const statusState = !row.sourceAvailable ? 'unavailable' : enabled ? 'available' : 'disabled'

  return {
    ...row,
    id: row.id || index + 1,
    subagentName: row.subagentName || row.name || `子智能体 ${index + 1}`,
    subagentKey: row.subagentCode || row.subagentKey || row.key || `subagent_${index + 1}`,
    description: row.description || '暂无描述',
    runtimeType,
    status: Number(row.enabled ?? 1),
    statusState,
    statusText: statusLabel(statusState),
    todayDispatch: Number(row.todayDelegations ?? 0),
    parentAgents: Number(row.parentAgents ?? 0),
    successRate: row.successRate == null ? null : Number(row.successRate),
    sourceName: row.sourceName || '-',
    headers: row.headers || []
  }
}

function normalizeTask(row, index) {
  const status = String(row.status || row.successStatus || 'COMPLETED').toUpperCase()
  const succeeded = ['SUCCESS', 'SUCCEEDED', 'COMPLETED', 'DONE'].includes(status)
  const finished = !['RUNNING', 'PENDING'].includes(status)

  return {
    ...row,
    id: row.id || index + 1,
    parentAgentName: row.parentAgentName || row.agentName || `主智能体 #${row.parentAgentId || index + 1}`,
    subagentName: row.subagentName || row.subagentKey || `子智能体 #${row.subagentId || index + 1}`,
    taskInput: row.taskInput || row.errorMessage || row.taskResult || '暂无任务描述',
    startedAt: row.startedAt || row.createTime || '-',
    durationMs: Number(row.durationMs || 0),
    errorMessage: row.errorMessage || row.reason || '',
    status,
    succeeded,
    finished
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    subagentCode: '',
    subagentName: '',
    description: '',
    sourceType: 1,
    localAgentId: '',
    remoteUrl: '',
    protocolType: 1,
    enabled: 1,
    remark: '',
    headers: []
  })
}

async function openCreateDialog() {
  resetForm()
  localAgentOptions.value = await listLocalAgentOptions()
  dialogTitle.value = '新建子智能体'
  dialogVisible.value = true
}

function openEditDialog(row) {
  resetForm()
  if (Number(row.sourceType) === 1) {
    localAgentOptions.value = [{
      id: row.localAgentId,
      agentName: row.sourceName,
      description: row.description
    }]
  }
  Object.assign(form, {
    id: row.id,
    subagentCode: row.subagentCode || row.subagentKey,
    subagentName: row.subagentName,
    description: row.description,
    sourceType: Number(row.sourceType),
    localAgentId: row.localAgentId || '',
    remoteUrl: row.remoteUrl || '',
    protocolType: Number(row.protocolType || 1),
    enabled: Number(row.enabled ?? row.status ?? 1),
    remark: row.remark || '',
    headers: (row.headers || []).map((header) => ({
      id: header.id,
      headerName: header.headerName,
      headerValue: '',
      hasValue: Boolean(header.hasValue),
      remove: false
    }))
  })
  dialogTitle.value = '编辑子智能体'
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.subagentName.trim() || !form.subagentCode.trim() || !form.description.trim()) {
    ElMessage.warning('请填写子智能体名称、唯一编码和描述')
    return
  }
  if (Number(form.sourceType) === 1 && !form.localAgentId) {
    ElMessage.warning('请选择本地智能体')
    return
  }
  if (Number(form.sourceType) === 2 && !form.remoteUrl.trim()) {
    ElMessage.warning('请填写远程服务 URL')
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      await updateSubagent(payload)
      ElMessage.success('子智能体已更新')
    } else {
      await createSubagent(payload)
      ElMessage.success('子智能体已创建')
    }
    dialogVisible.value = false
    await loadDashboard()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除子智能体「${row.subagentName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteSubagent(row.id)
  ElMessage.success('子智能体已删除')
  await loadDashboard()
}

function buildPayload() {
  return {
    id: normalizeId(form.id),
    subagentCode: form.subagentCode.trim(),
    subagentName: form.subagentName.trim(),
    description: form.description.trim(),
    sourceType: Number(form.sourceType),
    localAgentId: Number(form.sourceType) === 1 ? normalizeId(form.localAgentId) : null,
    remoteUrl: Number(form.sourceType) === 2 ? form.remoteUrl.trim() : null,
    protocolType: Number(form.sourceType) === 2 ? 1 : null,
    enabled: Number(form.enabled),
    remark: form.remark,
    headers: Number(form.sourceType) === 2
      ? form.headers.map((header) => ({
          id: normalizeId(header.id),
          headerName: header.headerName.trim(),
          headerValue: header.headerValue,
          remove: Boolean(header.remove)
        }))
      : []
  }
}

async function loadSubagentPage() {
  const result = await pageSubagents({
    current: currentPage.value,
    size: pageSize.value,
    keyword: queryParams.keyword || undefined,
    sourceType: queryParams.type || undefined,
    enabled: queryParams.status === '' ? undefined : Number(queryParams.status),
    sourceAvailable: queryParams.sourceAvailable === '' ? undefined : queryParams.sourceAvailable
  })
  subagents.value = result?.records || []
  total.value = Number(result?.total || 0)
}

async function loadDashboard() {
  loading.value = true
  try {
    const [pageResult, metricsResult, recentResult, exceptionResult] = await Promise.all([
      pageSubagents({
        current: currentPage.value,
        size: pageSize.value,
        keyword: queryParams.keyword || undefined,
        sourceType: queryParams.type || undefined,
        enabled: queryParams.status === '' ? undefined : Number(queryParams.status),
        sourceAvailable: queryParams.sourceAvailable === '' ? undefined : queryParams.sourceAvailable
      }),
      getSubagentMetrics(),
      listRecentSubagentTasks({ limit: 6 }),
      listRecentSubagentTasks({ limit: 4, exceptionsOnly: true })
    ])
    subagents.value = pageResult?.records || []
    total.value = Number(pageResult?.total || 0)
    metricData.value = metricsResult || null
    tasks.value = Array.isArray(recentResult) ? recentResult : []
    exceptionTaskRows.value = Array.isArray(exceptionResult) ? exceptionResult : []
  } finally {
    loading.value = false
  }
}

function searchSubagents() {
  currentPage.value = 1
  loadSubagentPage()
}

function selectLocalAgent(agentId) {
  const agent = localAgentOptions.value.find((item) => String(item.id) === String(agentId))
  if (!agent) return
  form.subagentName = agent.agentName || ''
  form.description = agent.description || agent.agentName || ''
}

function addHeader() {
  form.headers.push({
    id: null,
    headerName: '',
    headerValue: '',
    hasValue: false,
    remove: false
  })
}

function removeHeader(header, index) {
  if (header.id) {
    header.remove = true
    return
  }
  form.headers.splice(index, 1)
}

function statusClass(status) {
  return {
    running: status === 'running',
    available: status === 'available',
    disabled: status === 'disabled',
    unavailable: status === 'unavailable'
  }
}

function statusLabel(status) {
  const map = {
    running: '运行中',
    available: '可用',
    disabled: '已停用',
    unavailable: '来源不可用'
  }
  return map[status] || status || '-'
}

function taskStatusType(status) {
  if (['SUCCESS', 'SUCCEEDED', 'COMPLETED', 'DONE'].includes(status)) {
    return 'success'
  }
  if (status === 'TIMEOUT') {
    return 'warning'
  }
  if (['FAILED', 'CANCELLED'].includes(status)) {
    return 'danger'
  }
  return 'info'
}

function taskStatusLabel(status) {
  const map = {
    COMPLETED: '成功',
    SUCCESS: '成功',
    SUCCEEDED: '成功',
    FAILED: '失败',
    TIMEOUT: '异常',
    CANCELLED: '取消',
    RUNNING: '运行中'
  }
  return map[status] || status
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('en-US')
}

function formatRate(value) {
  if (!Number.isFinite(value)) {
    return '--'
  }
  return `${Number(value).toFixed(1)}%`
}

function formatDuration(value) {
  const ms = Number(value || 0)
  if (!ms) {
    return '--'
  }
  return `${(ms / 1000).toFixed(1)}s`
}

function normalizeId(value) {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="subagent-console management-page">
    <div class="subagent-metrics management-metrics">
      <article v-for="item in metrics" :key="item.label" class="subagent-metric management-metric-card">
        <div class="metric-icon management-metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <small>{{ item.label }}</small>
          <strong>{{ item.value }}</strong>
          <p :class="{ positive: item.positive }">{{ item.sub }}</p>
        </div>
      </article>
    </div>

    <div class="subagent-dashboard management-content-grid">
      <section class="subagent-list-panel management-panel">
        <div class="panel-head management-panel-title">
          <div>
            <h3>子智能体列表</h3>
            <p>共 {{ total }} 个子智能体</p>
          </div>
          <div class="subagent-filter-bar management-filter-bar">
            <el-input
              v-model="queryParams.keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索名称或描述"
              @clear="searchSubagents"
              @keyup.enter="searchSubagents"
            />
            <el-select v-model="queryParams.type" clearable placeholder="全部类型">
              <el-option v-for="type in typeOptions" :key="type.value" :label="type.label" :value="type.value" />
            </el-select>
            <el-select v-model="queryParams.status" clearable placeholder="全部状态">
              <el-option label="已启用" :value="1" />
              <el-option label="已停用" :value="0" />
            </el-select>
            <el-select v-model="queryParams.sourceAvailable" clearable placeholder="全部来源">
              <el-option label="来源可用" :value="true" />
              <el-option label="来源不可用" :value="false" />
            </el-select>
            <el-button-group class="view-toggle">
              <el-button :type="viewMode === 'grid' ? 'primary' : 'default'" :icon="Grid" @click="viewMode = 'grid'" />
              <el-button :type="viewMode === 'list' ? 'primary' : 'default'" :icon="Menu" @click="viewMode = 'list'" />
            </el-button-group>
            <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建子智能体</el-button>
          </div>
        </div>

        <div class="subagent-list" :class="viewMode">
          <article v-for="row in pagedRows" :key="row.id" class="subagent-card management-data-card">
            <el-dropdown class="management-card-menu" trigger="click">
              <button class="management-card-menu-button" type="button" aria-label="子智能体操作">
                <el-icon class="management-card-menu-icon"><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="Edit" @click="openEditDialog(row)">编辑配置</el-dropdown-item>
                  <el-dropdown-item :icon="Delete" divided @click="handleDelete(row)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <header class="subagent-card-head">
              <div class="subagent-main">
                <h4>{{ row.subagentName }}</h4>
                <span>{{ row.subagentKey }}</span>
                <p>{{ row.description }}</p>
              </div>
              <em class="subagent-status" :class="statusClass(row.statusState)">
                <i />
                {{ row.statusText }}
              </em>
            </header>

            <div class="subagent-card-stats">
              <div>
                <span>今日委派</span>
                <strong>{{ formatNumber(row.todayDispatch) }}</strong>
              </div>
              <div>
                <span>主智能体</span>
                <strong>{{ row.parentAgents }}</strong>
              </div>
              <div>
                <span>成功率</span>
                <strong>{{ formatRate(row.successRate) }}</strong>
              </div>
            </div>

            <footer class="subagent-card-actions">
              <span>{{ row.runtimeType }} · {{ row.sourceName }}</span>
            </footer>
          </article>
        </div>

        <div class="subagent-list-footer">
          <span>共 {{ total }} 项</span>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            class="management-pagination"
            background
            layout="prev, pager, next, sizes"
            :page-sizes="[6, 12, 24]"
            :total="total"
          />
        </div>
      </section>

      <aside class="subagent-side management-side-column">
        <section class="side-panel management-side-card">
          <div class="side-head">
            <h3>最近委派记录</h3>
          </div>
          <div class="delegation-timeline">
            <div v-for="task in recentTasks" :key="task.id" class="delegation-row">
              <span />
              <div>
                <strong>{{ task.parentAgentName }} → {{ task.subagentName }}</strong>
                <small>{{ task.taskInput }}</small>
              </div>
              <time>{{ task.startedAt }}</time>
              <el-tag :type="taskStatusType(task.status)">{{ taskStatusLabel(task.status) }}</el-tag>
            </div>
            <el-empty v-if="!recentTasks.length" :image-size="54" description="暂无委派记录" />
          </div>
        </section>

        <section class="side-panel exception-panel management-side-card">
          <div class="side-head">
            <h3>异常执行</h3>
          </div>
          <div class="exception-list">
            <div v-for="task in exceptionTasks" :key="task.id" class="exception-row">
              <div>
                <strong>{{ task.subagentName }}</strong>
                <small>{{ task.errorMessage || task.taskInput }}</small>
              </div>
              <time>{{ task.startedAt }}</time>
              <el-tag :type="taskStatusType(task.status)">{{ taskStatusLabel(task.status) }}</el-tag>
            </div>
            <el-empty v-if="!exceptionTasks.length" :image-size="54" description="暂无异常执行" />
          </div>
        </section>
      </aside>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="760px"
      destroy-on-close
      class="subagent-dialog"
    >
      <el-form label-width="116px" class="subagent-form">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="来源类型" required>
              <el-radio-group v-model="form.sourceType" :disabled="Boolean(form.id)">
                <el-radio :value="1">本地子智能体</el-radio>
                <el-radio :value="2">远程子智能体</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="Number(form.sourceType) === 1" :span="24">
            <el-form-item label="本地智能体" required>
              <el-select
                v-model="form.localAgentId"
                :disabled="Boolean(form.id)"
                filterable
                style="width: 100%"
                placeholder="选择当前租户内尚未注册的智能体"
                @change="selectLocalAgent"
              >
                <el-option
                  v-for="agent in localAgentOptions"
                  :key="agent.id"
                  :label="agent.agentName"
                  :value="agent.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="form.subagentName" placeholder="如：数据检索专家" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="唯一编码" required>
              <el-input v-model="form.subagentCode" placeholder="如：data-retriever" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.enabled">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述" required>
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述子智能体能力、适用任务和交付结果" />
            </el-form-item>
          </el-col>
          <el-col v-if="Number(form.sourceType) === 2" :span="24">
            <el-form-item label="远程 URL" required>
              <el-input v-model="form.remoteUrl" placeholder="https://agent.example.com" />
            </el-form-item>
          </el-col>
          <el-col v-if="Number(form.sourceType) === 2" :span="24">
            <el-form-item label="协议">
              <el-input model-value="Agent Protocol" disabled />
            </el-form-item>
          </el-col>
          <el-col v-if="Number(form.sourceType) === 2" :span="24">
            <el-form-item label="认证 Header">
              <div class="header-editor">
                <div
                  v-for="(header, index) in form.headers"
                  v-show="!header.remove"
                  :key="header.id || index"
                  class="header-row"
                >
                  <el-input v-model="header.headerName" placeholder="Authorization" />
                  <el-input
                    v-model="header.headerValue"
                    type="password"
                    show-password
                    :placeholder="header.hasValue ? '留空则保留原值' : 'Header 值'"
                  />
                  <el-button type="danger" plain :icon="Delete" @click="removeHeader(header, index)" />
                </div>
                <el-button plain :icon="Plus" @click="addHeader">添加 Header</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.subagent-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.subagent-metric,
.subagent-list-panel,
.side-panel {
  border: 1px solid #d7e5f8;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 38px rgba(48, 94, 151, 0.08);
}

.subagent-metric {
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

.metric-icon.cyan {
  color: #0b95d8;
  background: #e9f8ff;
}

.metric-icon.indigo {
  color: #5c6cff;
  background: #eef1ff;
}

.metric-icon.green {
  color: #168354;
  background: #eaf8ef;
}

.subagent-metric span {
  display: block;
  color: #667d99;
  font-size: 13px;
}

.subagent-metric strong {
  display: block;
  margin-top: 8px;
  color: #0a2547;
  font-size: 30px;
  font-weight: 850;
  line-height: 1;
}

.subagent-metric small {
  display: block;
  margin-top: 12px;
  color: #6d819b;
  font-size: 12px;
  font-weight: 750;
}

.subagent-metric small.positive {
  color: #22a86b;
}

.subagent-dashboard {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(760px, 1fr) minmax(330px, 0.52fr);
  align-self: stretch;
  align-items: stretch;
  gap: 18px;
  min-height: 0;
}

.subagent-list-panel {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-color: #d9e4f2;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(34, 67, 112, 0.06);
}

.panel-head,
.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel-head {
  padding: 18px 18px 24px;
  border-bottom: 0;
}

.panel-head h3,
.side-head h3 {
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

.subagent-filter-bar {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.subagent-filter-bar .el-input {
  width: 250px;
}

.subagent-filter-bar .el-select {
  width: 128px;
}

.subagent-filter-bar .el-button {
  height: 34px;
  border-radius: 5px;
  font-weight: 800;
}

.view-toggle {
  flex: 0 0 auto;
}

.subagent-filter-bar :deep(.el-input__wrapper),
.subagent-filter-bar :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 5px;
  box-shadow: 0 0 0 1px #d7e1ee inset;
}

.subagent-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px 16px;
  padding: 0 18px 18px;
}

.subagent-list.list {
  grid-template-columns: 1fr;
}

.subagent-card {
  position: relative;
  display: grid;
  min-height: 210px;
  grid-template-rows: auto minmax(70px, 1fr) auto;
  gap: 14px;
  border: 1px solid #e0eaf6;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(42, 72, 108, 0.05);
}

.subagent-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 52px 0 18px;
}

.subagent-main {
  min-width: 0;
}

.subagent-main h4 {
  overflow: hidden;
  margin: 0;
  color: #0a2547;
  font-size: 18px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subagent-main > span {
  display: block;
  overflow: hidden;
  margin-top: 7px;
  color: #405874;
  font-size: 13px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subagent-main p {
  display: -webkit-box;
  overflow: hidden;
  margin: 7px 0 0;
  color: #6d819b;
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.subagent-status {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  min-width: 70px;
  height: 30px;
  justify-content: center;
  border: 1px solid #bce8cc;
  border-radius: 6px;
  color: #168354;
  background: #eaf8ef;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.subagent-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.subagent-status.running {
  border-color: #bce8cc;
  color: #168354;
  background: #eaf8ef;
}

.subagent-status.available {
  border-color: #d5f0df;
  color: #168354;
  background: #f0fbf4;
}

.subagent-status.disabled {
  border-color: #d9e2ec;
  color: #6d819b;
  background: #f4f7fb;
}

.subagent-status.unavailable {
  border-color: #fed7aa;
  color: #c2410c;
  background: #fff7ed;
}

.subagent-card-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  align-self: end;
  margin: 0 18px;
  border-top: 1px solid #e4edf7;
  border-bottom: 1px solid #e4edf7;
}

.subagent-card-stats div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 11px 10px;
  text-align: center;
  border-right: 1px solid #e4edf7;
}

.subagent-card-stats div:last-child {
  border-right: 0;
}

.subagent-card-stats span {
  color: #7e94ad;
  font-size: 12px;
}

.subagent-card-stats strong {
  color: #203957;
  font-size: 16px;
}

.subagent-card-actions {
  display: flex;
  min-height: 40px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px 12px;
}

.subagent-card-actions > span {
  color: #6d819b;
  font-size: 13px;
  font-weight: 750;
}

.subagent-list-footer {
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

.subagent-list-footer :deep(.el-pagination) {
  --el-pagination-button-bg-color: #ffffff;
  --el-pagination-hover-color: #0b63f6;
}

.subagent-list-footer :deep(.el-pager li),
.subagent-list-footer :deep(.btn-prev),
.subagent-list-footer :deep(.btn-next) {
  border: 1px solid #d9e4f2;
  border-radius: 5px;
  box-shadow: none;
}

.subagent-list-footer :deep(.el-pager li.is-active) {
  border-color: #0b63f6;
  color: #0b63f6;
  background: #ffffff;
}

.subagent-side {
  display: grid;
  grid-template-rows: minmax(360px, 0.58fr) minmax(250px, 0.42fr);
  gap: 18px;
  min-height: 0;
}

.side-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding: 18px;
  border-radius: 8px;
}

.side-head .el-button {
  height: auto;
  padding: 0;
  font-weight: 800;
}

.delegation-timeline,
.exception-list {
  display: grid;
  flex: 1 1 auto;
  align-content: start;
  min-height: 0;
  margin-top: 18px;
  overflow-y: auto;
  padding-right: 4px;
}

.delegation-row {
  position: relative;
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) 72px 54px;
  align-items: center;
  gap: 10px;
  min-height: 76px;
}

.delegation-row::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 5px;
  width: 1px;
  background: #d9e8fb;
  content: '';
}

.delegation-row > span {
  z-index: 1;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #1f78ff;
  box-shadow: 0 0 0 4px #eaf3ff;
}

.delegation-row strong,
.exception-row strong {
  overflow: hidden;
  color: #0a2547;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delegation-row small,
.exception-row small {
  display: block;
  overflow: hidden;
  margin-top: 7px;
  color: #7890aa;
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delegation-row time,
.exception-row time {
  color: #6d819b;
  font-size: 12px;
  font-weight: 750;
}

.exception-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px 54px;
  align-items: center;
  gap: 10px;
  min-height: 70px;
  border-bottom: 1px solid #e3edf8;
}

.exception-row:last-child {
  border-bottom: 0;
}

.subagent-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 22px;
  border-bottom: 1px solid #dce8f5;
}

.subagent-dialog :deep(.el-dialog__body) {
  padding: 20px 22px;
}

.subagent-form .el-select,
.subagent-form .el-input,
.subagent-form :deep(.el-input-number),
.subagent-form :deep(.el-textarea) {
  width: 100%;
}

.switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
}

.header-editor {
  display: grid;
  width: 100%;
  gap: 10px;
}

.header-row {
  display: grid;
  grid-template-columns: minmax(140px, 0.8fr) minmax(220px, 1.2fr) auto;
  gap: 8px;
}

@media (max-width: 1320px) {
  .subagent-dashboard {
    grid-template-columns: 1fr;
  }

  .subagent-dashboard,
  .subagent-side {
    min-height: 0;
  }

  .subagent-side {
    grid-template-rows: none;
  }

}

@media (max-width: 980px) {
  .subagent-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .subagent-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .delegation-row,
  .exception-row {
    grid-template-columns: 1fr;
  }

  .delegation-row > span,
  .delegation-row::before {
    display: none;
  }
}

@media (max-width: 640px) {
  .subagent-metrics {
    grid-template-columns: 1fr;
  }

  .subagent-list {
    grid-template-columns: 1fr;
    padding: 0 12px 14px;
  }

  .subagent-card-stats {
    grid-template-columns: 1fr;
  }

  .subagent-card-stats div {
    border-right: 0;
    border-bottom: 1px solid #e4edf7;
  }

  .subagent-card-stats div:last-child {
    border-bottom: 0;
  }

  .subagent-card-actions,
  .subagent-list-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

<style scoped src="../../assets/management-page.css"></style>
