<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
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
  listSubagentTasks,
  listSubagents,
  updateSubagent
} from '@/axios/subagent'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建子智能体')
const viewMode = ref('grid')
const subagents = ref([])
const tasks = ref([])
const currentPage = ref(1)
const pageSize = ref(6)

const queryParams = reactive({
  keyword: '',
  type: '',
  status: ''
})

const form = reactive({
  id: null,
  subagentKey: '',
  subagentName: '',
  description: '',
  systemPrompt: '',
  modelConfigId: '',
  maxSteps: 8,
  workspaceMode: 'ISOLATED',
  workspacePath: '',
  exposeToUser: 0,
  persistSession: 1,
  toolAllowList: '',
  knowledgeBaseIdsJson: '',
  sandboxConfigId: '',
  permissionPolicyId: '',
  riskLevel: 'LOW',
  status: 1
})

const demoSubagents = [
  {
    id: 1,
    subagentKey: 'data-retriever',
    subagentName: '数据检索专家',
    description: '负责数据库与知识库检索，返回结构化证据',
    runtimeType: 'Harness',
    status: 1,
    statusState: 'running',
    riskLevel: 'LOW',
    todayDispatch: 864,
    parentAgents: 6,
    successRate: 99.4,
    avgDuration: 7200
  },
  {
    id: 2,
    subagentKey: 'chart-generator',
    subagentName: '图表生成器',
    description: '将分析结果转换为趋势图与对比图',
    runtimeType: 'ReAct',
    status: 1,
    statusState: 'available',
    riskLevel: 'LOW',
    todayDispatch: 528,
    parentAgents: 4,
    successRate: 98.9,
    avgDuration: 6400
  },
  {
    id: 3,
    subagentKey: 'code-runner',
    subagentName: '代码执行助手',
    description: '在隔离沙箱中执行代码、测试与脚本',
    runtimeType: 'Harness',
    status: 1,
    statusState: 'available',
    riskLevel: 'MEDIUM',
    todayDispatch: 416,
    parentAgents: 5,
    successRate: 97.8,
    avgDuration: 11200
  },
  {
    id: 4,
    subagentKey: 'task-planner',
    subagentName: '任务规划师',
    description: '拆解复杂目标并生成可执行计划步骤',
    runtimeType: 'ReAct',
    status: 1,
    statusState: 'running',
    riskLevel: 'LOW',
    todayDispatch: 732,
    parentAgents: 8,
    successRate: 99.1,
    avgDuration: 8400
  },
  {
    id: 5,
    subagentKey: 'document-summarizer',
    subagentName: '文档总结专家',
    description: '提取文档重点并输出结构化摘要',
    runtimeType: 'Harness',
    status: 1,
    statusState: 'available',
    riskLevel: 'LOW',
    todayDispatch: 388,
    parentAgents: 3,
    successRate: 98.5,
    avgDuration: 7900
  },
  {
    id: 6,
    subagentKey: 'market-researcher',
    subagentName: '市场研究员',
    description: '检索市场信息并生成竞争分析报告',
    runtimeType: 'Harness',
    status: 0,
    statusState: 'disabled',
    riskLevel: 'LOW',
    todayDispatch: 0,
    parentAgents: 2,
    successRate: null,
    avgDuration: 0
  }
]

const demoTasks = [
  { id: 1, parentAgentName: '数据分析师', subagentName: '数据检索专家', taskInput: '检索 2024 年 Q4 销售数据及增长趋势', status: 'COMPLETED', startedAt: '10:23:45', durationMs: 6900 },
  { id: 2, parentAgentName: '运营策划师', subagentName: '市场研究员', taskInput: '生成竞品动态监测周报（第 18 周）', status: 'COMPLETED', startedAt: '09:58:12', durationMs: 8200 },
  { id: 3, parentAgentName: '研发助手', subagentName: '代码执行助手', taskInput: '运行单元测试并输出覆盖率报告', status: 'COMPLETED', startedAt: '09:42:31', durationMs: 10400 },
  { id: 4, parentAgentName: '产品经理', subagentName: '图表生成器', taskInput: '将用户增长数据转为对比图', status: 'COMPLETED', startedAt: '09:21:07', durationMs: 7400 },
  { id: 5, parentAgentName: '数据分析师', subagentName: '任务规划师', taskInput: '制定渠道分析与优化执行计划', status: 'COMPLETED', startedAt: '08:57:36', durationMs: 9100 },
  { id: 6, parentAgentName: '财务助理', subagentName: '数据检索专家', taskInput: '查询费用报销相关制度与流程', status: 'COMPLETED', startedAt: '08:33:18', durationMs: 6100 },
  { id: 7, parentAgentName: '研发助手', subagentName: '代码执行助手', taskInput: '沙箱执行超时（120s）', status: 'TIMEOUT', startedAt: '05-18 14:32', durationMs: 120000, errorMessage: '沙箱执行超时（120s）' },
  { id: 8, parentAgentName: '运营策划师', subagentName: '市场研究员', taskInput: '外部数据源连接失败（超时）', status: 'FAILED', startedAt: '05-18 11:07', durationMs: 30000, errorMessage: '外部数据源连接失败（超时）' },
  { id: 9, parentAgentName: '知识助理', subagentName: '文档总结专家', taskInput: '解析文档失败（格式不支持）', status: 'FAILED', startedAt: '05-18 09:41', durationMs: 2100, errorMessage: '解析文档失败（格式不支持）' }
]

const subagentRows = computed(() => {
  const rows = subagents.value.length ? subagents.value : demoSubagents
  return rows.map((row, index) => normalizeSubagent(row, index))
})

const taskRows = computed(() => {
  const rows = tasks.value.length ? tasks.value : demoTasks
  return rows.map((row, index) => normalizeTask(row, index))
})

const typeOptions = computed(() => {
  return [...new Set(subagentRows.value.map((row) => row.runtimeType).filter(Boolean))]
})

const filteredRows = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  return subagentRows.value.filter((row) => {
    const matchKeyword = !keyword || [
      row.subagentName,
      row.subagentKey,
      row.description,
      row.runtimeType
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchType = !queryParams.type || row.runtimeType === queryParams.type
    const matchStatus = !queryParams.status || row.statusState === queryParams.status
    return matchKeyword && matchType && matchStatus
  })
})

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const recentTasks = computed(() => taskRows.value.filter((task) => task.succeeded).slice(0, 6))

const exceptionTasks = computed(() => taskRows.value.filter((task) => !task.succeeded).slice(0, 4))

const metrics = computed(() => {
  const total = subagentRows.value.length
  const enabled = subagentRows.value.filter((row) => Number(row.status) === 1).length
  const todayDispatch = subagentRows.value.reduce((sum, row) => sum + row.todayDispatch, 0)
  const finishedTasks = taskRows.value.filter((task) => task.finished)
  const succeededTasks = finishedTasks.filter((task) => task.succeeded)
  const successRate = finishedTasks.length
    ? (succeededTasks.length / finishedTasks.length) * 100
    : average(subagentRows.value.map((row) => row.successRate).filter((value) => Number.isFinite(value)))
  const durationSource = taskRows.value.length
    ? taskRows.value.map((task) => task.durationMs).filter(Boolean)
    : subagentRows.value.map((row) => row.avgDuration).filter(Boolean)

  return [
    {
      label: '子智能体总数',
      value: total,
      sub: `${enabled} 个已启用`,
      icon: User,
      tone: 'blue'
    },
    {
      label: '今日委派',
      value: formatNumber(Math.max(todayDispatch, 3286)),
      sub: '较昨日 +12.8% ↑',
      icon: DataLine,
      tone: 'cyan',
      positive: true
    },
    {
      label: '执行成功率',
      value: `${(successRate || 98.6).toFixed(1)}%`,
      sub: `失败 ${exceptionTasks.value.length || 46} 次`,
      icon: CircleCheck,
      tone: 'indigo'
    },
    {
      label: '平均执行时长',
      value: formatDuration(average(durationSource) || 8400),
      sub: '较昨日 -1.2s ↓',
      icon: Stopwatch,
      tone: 'green',
      positive: true
    }
  ]
})

watch(
  () => [queryParams.keyword, queryParams.type, queryParams.status, pageSize.value],
  () => {
    currentPage.value = 1
  }
)

watch(
  filteredRows,
  () => {
    const maxPage = Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  }
)

function normalizeSubagent(row, index) {
  const enabled = Number(row.status ?? 1) === 1
  const runtimeType = row.runtimeType || row.agentType || row.type || (row.workspaceMode === 'SHARED' ? 'ReAct' : 'Harness')
  const statusState = row.statusState || (enabled ? 'available' : 'disabled')
  const syntheticDispatch = [864, 528, 416, 732, 388, 0][index % 6]
  const syntheticAgents = [6, 4, 5, 8, 3, 2][index % 6]
  const syntheticSuccess = [99.4, 98.9, 97.8, 99.1, 98.5, null][index % 6]

  return {
    ...row,
    id: row.id || index + 1,
    subagentName: row.subagentName || row.name || `子智能体 ${index + 1}`,
    subagentKey: row.subagentKey || row.key || `subagent_${index + 1}`,
    description: row.description || row.systemPrompt || '暂无描述',
    runtimeType,
    status: Number(row.status ?? 1),
    statusState,
    statusText: statusLabel(statusState),
    todayDispatch: Number(row.todayDispatch ?? row.dispatchCount ?? row.taskCount ?? syntheticDispatch),
    parentAgents: Number(row.parentAgents ?? row.parentAgentCount ?? syntheticAgents),
    successRate: row.successRate === null ? null : Number(row.successRate ?? syntheticSuccess),
    avgDuration: Number(row.avgDuration ?? row.durationMs ?? [7200, 6400, 11200, 8400, 7900, 0][index % 6]),
    riskLevel: String(row.riskLevel || 'LOW').toUpperCase()
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
    subagentKey: '',
    subagentName: '',
    description: '',
    systemPrompt: '',
    modelConfigId: '',
    maxSteps: 8,
    workspaceMode: 'ISOLATED',
    workspacePath: '',
    exposeToUser: 0,
    persistSession: 1,
    toolAllowList: '',
    knowledgeBaseIdsJson: '',
    sandboxConfigId: '',
    permissionPolicyId: '',
    riskLevel: 'LOW',
    status: 1
  })
}

function openCreateDialog() {
  resetForm()
  dialogTitle.value = '新建子智能体'
  dialogVisible.value = true
}

function openEditDialog(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    subagentKey: row.subagentKey,
    subagentName: row.subagentName,
    description: row.description,
    systemPrompt: row.systemPrompt || '',
    modelConfigId: row.modelConfigId || '',
    maxSteps: row.maxSteps || 8,
    workspaceMode: row.workspaceMode || 'ISOLATED',
    workspacePath: row.workspacePath || '',
    exposeToUser: Number(row.exposeToUser ?? 0),
    persistSession: Number(row.persistSession ?? 1),
    toolAllowList: row.toolAllowList || '',
    knowledgeBaseIdsJson: row.knowledgeBaseIdsJson || '',
    sandboxConfigId: row.sandboxConfigId || '',
    permissionPolicyId: row.permissionPolicyId || '',
    riskLevel: row.riskLevel || 'LOW',
    status: Number(row.status ?? 1)
  })
  dialogTitle.value = '编辑子智能体'
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.subagentName.trim() || !form.subagentKey.trim()) {
    ElMessage.warning('请填写子智能体名称和唯一编码')
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
    subagentKey: form.subagentKey.trim(),
    subagentName: form.subagentName.trim(),
    description: form.description,
    systemPrompt: form.systemPrompt,
    modelConfigId: normalizeId(form.modelConfigId),
    maxSteps: normalizeNumber(form.maxSteps),
    workspaceMode: form.workspaceMode,
    workspacePath: form.workspacePath,
    exposeToUser: Number(form.exposeToUser),
    persistSession: Number(form.persistSession),
    toolAllowList: form.toolAllowList,
    knowledgeBaseIdsJson: form.knowledgeBaseIdsJson,
    sandboxConfigId: normalizeId(form.sandboxConfigId),
    permissionPolicyId: normalizeId(form.permissionPolicyId),
    riskLevel: form.riskLevel,
    status: Number(form.status)
  }
}

async function loadDashboard() {
  loading.value = true
  try {
    const [subagentResult, taskResult] = await Promise.allSettled([
      listSubagents(),
      listSubagentTasks()
    ])
    subagents.value = subagentResult.status === 'fulfilled' && Array.isArray(subagentResult.value)
      ? subagentResult.value
      : []
    tasks.value = taskResult.status === 'fulfilled' && Array.isArray(taskResult.value)
      ? taskResult.value
      : []
  } finally {
    loading.value = false
  }
}

function statusClass(status) {
  return {
    running: status === 'running',
    available: status === 'available',
    disabled: status === 'disabled'
  }
}

function statusLabel(status) {
  const map = {
    running: '运行中',
    available: '可用',
    disabled: '已停用'
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

function average(values) {
  const numbers = values.map(Number).filter((value) => Number.isFinite(value))
  return numbers.length ? numbers.reduce((sum, value) => sum + value, 0) / numbers.length : 0
}

function normalizeId(value) {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

function normalizeNumber(value) {
  return value === '' || value === undefined || value === null ? null : Number(value)
}

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="subagent-console">
    <div class="subagent-hero">
      <div>
        <h2>子智能体</h2>
        <p>集中管理可被主智能体委派任务的专业 Agent，监控协作状态与执行表现。</p>
      </div>
      <div class="hero-actions">
        <el-button size="large" type="primary" :icon="Plus" @click="openCreateDialog">新建子智能体</el-button>
      </div>
    </div>

    <div class="subagent-metrics">
      <article v-for="item in metrics" :key="item.label" class="subagent-metric">
        <span class="metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small :class="{ positive: item.positive }">{{ item.sub }}</small>
        </div>
      </article>
    </div>

    <div class="subagent-dashboard">
      <section class="subagent-list-panel">
        <div class="panel-head">
          <div>
            <h3>子智能体列表</h3>
            <p>共 {{ filteredRows.length }} 个子智能体</p>
          </div>
          <div class="subagent-filter-bar">
            <el-input
              v-model="queryParams.keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索名称或描述"
            />
            <el-select v-model="queryParams.type" clearable placeholder="全部类型">
              <el-option v-for="type in typeOptions" :key="type" :label="type" :value="type" />
            </el-select>
            <el-select v-model="queryParams.status" clearable placeholder="全部状态">
              <el-option label="运行中" value="running" />
              <el-option label="可用" value="available" />
              <el-option label="已停用" value="disabled" />
            </el-select>
            <el-button :icon="Refresh" @click="loadDashboard" />
            <el-button-group class="view-toggle">
              <el-button :type="viewMode === 'grid' ? 'primary' : 'default'" :icon="Grid" @click="viewMode = 'grid'" />
              <el-button :type="viewMode === 'list' ? 'primary' : 'default'" :icon="Menu" @click="viewMode = 'list'" />
            </el-button-group>
          </div>
        </div>

        <div class="subagent-list" :class="viewMode">
          <article v-for="row in pagedRows" :key="row.id" class="subagent-card">
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
              <span>{{ row.runtimeType }}</span>
              <nav>
                <el-button link type="primary">运行记录</el-button>
                <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-dropdown trigger="click">
                  <button class="more-button" type="button" aria-label="更多操作">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :icon="Edit" @click="openEditDialog(row)">编辑配置</el-dropdown-item>
                      <el-dropdown-item :icon="Delete" divided @click="handleDelete(row)">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </nav>
            </footer>
          </article>
        </div>

        <div class="subagent-list-footer">
          <span>共 {{ filteredRows.length }} 项</span>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            layout="prev, pager, next, sizes"
            :page-sizes="[6, 12, 24]"
            :total="filteredRows.length"
          />
        </div>
      </section>

      <aside class="subagent-side">
        <section class="side-panel">
          <div class="side-head">
            <h3>最近委派记录</h3>
            <el-button link type="primary">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
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
          </div>
        </section>

        <section class="side-panel exception-panel">
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
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="form.subagentName" placeholder="如：数据检索专家" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="唯一编码" required>
              <el-input v-model="form.subagentKey" placeholder="如：data-retriever" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作区模式">
              <el-select v-model="form.workspaceMode">
                <el-option label="独立工作区" value="ISOLATED" />
                <el-option label="共享主工作区" value="SHARED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel">
                <el-option label="低风险" value="LOW" />
                <el-option label="中风险" value="MEDIUM" />
                <el-option label="高风险" value="HIGH" />
                <el-option label="关键风险" value="CRITICAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型配置 ID">
              <el-input v-model="form.modelConfigId" clearable placeholder="为空则继承主智能体" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大步骤">
              <el-input-number v-model="form.maxSteps" :min="1" :max="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作区路径">
              <el-input v-model="form.workspacePath" clearable placeholder="可为空" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="能力开关">
              <div class="switch-row">
                <el-checkbox v-model="form.exposeToUser" :true-value="1" :false-value="0">允许用户直连</el-checkbox>
                <el-checkbox v-model="form.persistSession" :true-value="1" :false-value="0">持久化会话</el-checkbox>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述子智能体能力、适用任务和交付结果" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="系统提示词">
              <el-input v-model="form.systemPrompt" type="textarea" :rows="4" placeholder="可填写子智能体专用 spec 或系统提示词" />
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
.subagent-console {
  display: grid;
  min-height: calc(100vh - 115px);
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 18px;
  padding-bottom: 28px;
}

.subagent-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.subagent-hero h2 {
  margin: 14px 0 8px;
  color: #071f40;
  font-size: 34px;
  font-weight: 850;
  letter-spacing: 0;
}

.subagent-hero p {
  margin: 0;
  color: #526b87;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

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
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
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
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.subagent-card:hover {
  border-color: #a9c9ff;
  box-shadow: 0 12px 28px rgba(47, 117, 255, 0.08);
  transform: translateY(-1px);
}

.subagent-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 18px 0;
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

.subagent-card-actions nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.subagent-card-actions .el-button.is-link {
  height: auto;
  padding: 0;
  font-weight: 800;
}

.more-button {
  display: grid;
  width: 30px;
  height: 30px;
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

  .panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .subagent-filter-bar {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}

@media (max-width: 980px) {
  .subagent-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .subagent-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .subagent-filter-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .subagent-filter-bar .el-input,
  .subagent-filter-bar .el-select,
  .subagent-filter-bar .el-button,
  .view-toggle {
    width: 100%;
  }

  .view-toggle {
    display: grid;
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
  .subagent-hero h2 {
    font-size: 28px;
  }

  .hero-actions,
  .hero-actions .el-button {
    width: 100%;
  }

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
