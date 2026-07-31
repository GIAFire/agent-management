<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Connection,
  Delete,
  Edit,
  Files,
  MagicStick,
  Menu,
  Monitor,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  Setting,
  TrendCharts,
  Warning
} from '@element-plus/icons-vue'
import {
  createAgent,
  deleteAgent,
  getAgent,
  getAgentMetrics,
  pageAgentRuns,
  pageAgents,
  updateAgent
} from '@/axios/agent'
import { listSkills } from '@/axios/skill'
import {
  listWizardKnowledgeBases,
  listWizardModels,
  listWizardPrompts,
  listWizardSubagents,
  listWizardTools
} from '@/axios/agentWizard'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const optionsLoading = ref(false)
const formRef = ref()
const activeStep = ref(0)
const viewMode = ref('grid')
const rows = ref([])
const total = ref(0)
const metrics = ref({})

const query = reactive({
  current: 1,
  size: 6,
  keyword: ''
})

const optionRows = reactive({
  models: [],
  prompts: [],
  tools: [],
  skills: [],
  knowledgeBases: [],
  subagents: []
})

const createForm = () => ({
  id: null,
  agentCode: '',
  agentName: '',
  description: '',
  agentVersion: null,
  configVersion: null,
  modelId: null,
  sysPromptId: null,
  maxIters: 10,
  permissionMode: 'DEFAULT',
  compactionEnabled: 1,
  triggerMessages: 30,
  keepMessages: 10,
  triggerTokens: 6000,
  keepTokens: 1000,
  toolResultEvictionEnabled: 1,
  memoryEnable: 1,
  planModeEnabled: 1,
  planFileDirectory: 'plans',
  taskListEnabled: 1,
  allowShellInPlanMode: 0,
  stateStoreType: 'local_file',
  selectedToolIds: [],
  selectedSkillIds: [],
  selectedKnowledgeBaseIds: [],
  selectedSubagentIds: []
})

const form = reactive(createForm())
const originalStateStoreType = ref('local_file')
const invalidPromptName = ref('')
const invalidBindings = ref([])

const runDialog = reactive({
  visible: false,
  loading: false,
  agent: null,
  rows: [],
  total: 0,
  current: 1,
  size: 10,
  status: '',
  range: []
})

const steps = [
  { title: '基础信息', description: '编码、名称与职责' },
  { title: '模型与提示词', description: '选择运行模型和模板' },
  { title: '能力配置', description: '工具、技能与子智能体' },
  { title: '知识库', description: '绑定可检索知识' },
  { title: '高级配置', description: '上下文、计划与状态存储' }
]

const permissionOptions = [
  { value: 'DEFAULT', label: '默认：按规则询问或拒绝' },
  { value: 'ACCEPT_EDITS', label: '自动允许工作区内文件编辑' },
  { value: 'EXPLORE', label: '只读探索' },
  { value: 'BYPASS', label: '放行全部操作' },
  { value: 'DONT_ASK', label: '将询问转为拒绝' }
]

const rules = {
  agentCode: [
    { required: true, message: '请输入智能体编码', trigger: 'blur' },
    {
      pattern: /^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])$/,
      message: '请输入 2–64 位小写字母、数字或连字符，首尾不能是连字符',
      trigger: 'blur'
    }
  ],
  agentName: [
    { required: true, message: '请输入智能体名称', trigger: 'blur' },
    { max: 100, message: '智能体名称不能超过 100 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '智能体描述不能超过 500 个字符', trigger: 'blur' }
  ]
}

const isEditing = computed(() => Boolean(form.id))
const dialogTitle = computed(() => isEditing.value ? '编辑智能体' : '新建智能体')
const selectedModel = computed(() => optionRows.models.find(
  item => String(item.id) === String(form.modelId)
))
const selectedPrompt = computed(() => optionRows.prompts.find(
  item => String(item.id) === String(form.sysPromptId)
))

const metricCards = computed(() => [
  {
    label: '智能体总数',
    value: formatNumber(metrics.value.totalAgents),
    note: `今日新增 ${formatNumber(metrics.value.newToday)}`,
    icon: Monitor,
    tone: 'blue'
  },
  {
    label: '今日消耗 Token',
    value: formatCompact(metrics.value.todayTokens),
    note: changeText(metrics.value.tokenChangePercent),
    icon: Files,
    tone: 'green'
  },
  {
    label: '今日运行',
    value: formatNumber(metrics.value.todayRuns),
    note: changeText(metrics.value.runChangePercent),
    icon: TrendCharts,
    tone: 'violet'
  },
  {
    label: '今日成功率',
    value: metrics.value.successRate == null ? '--' : `${metrics.value.successRate}%`,
    note: [
      changeText(metrics.value.successRateChange),
      metrics.value.averageDurationMs == null
        ? '暂无已结束运行'
        : `平均耗时 ${formatDuration(metrics.value.averageDurationMs)}`
    ].join(' · '),
    icon: Connection,
    tone: 'amber'
  }
])

const formatNumber = value => Number(value || 0).toLocaleString('zh-CN')

const formatCompact = value => {
  const number = Number(value || 0)
  if (number >= 1000000) return `${(number / 1000000).toFixed(2)}M`
  if (number >= 1000) return `${(number / 1000).toFixed(1)}K`
  return formatNumber(number)
}

const formatDuration = value => {
  const milliseconds = Number(value || 0)
  return milliseconds >= 1000
    ? `${(milliseconds / 1000).toFixed(2)}s`
    : `${Math.round(milliseconds)}ms`
}

const changeText = value => {
  if (value == null) return '暂无昨日同期数据'
  const number = Number(value)
  const sign = number > 0 ? '+' : ''
  return `较昨日同期 ${sign}${number.toFixed(1)}%`
}

const formatDateTime = value => value ? String(value).replace('T', ' ') : '--'

const modelLabel = row => {
  if (!row?.modelId) return '未绑定模型'
  return [
    row.modelConfigName,
    row.providerName,
    row.protocol,
    row.modelName
  ].filter(Boolean).join(' · ')
}

const successText = row => (
  row.todaySuccessRate == null ? '暂无已结束运行' : `成功率 ${row.todaySuccessRate}%`
)

const loadMetrics = async () => {
  metrics.value = await getAgentMetrics() || {}
}

const loadRows = async () => {
  loading.value = true
  try {
    const data = await pageAgents(query)
    rows.value = data?.records || []
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

const loadPage = async () => {
  await Promise.all([loadMetrics(), loadRows()])
}

const search = () => {
  query.current = 1
  loadRows()
}

const loadOptions = async () => {
  optionsLoading.value = true
  try {
    const [models, prompts, tools, skills, knowledgeBases, subagents] = await Promise.all([
      listWizardModels(),
      listWizardPrompts(),
      listWizardTools(),
      listSkills(),
      listWizardKnowledgeBases(),
      listWizardSubagents()
    ])
    optionRows.models = (models || []).filter(item => Number(item.status ?? 1) === 1)
    optionRows.prompts = (prompts || []).filter(
      item => Number(item.status ?? 1) === 1 && Number(item.deleted ?? 0) === 0
    )
    optionRows.tools = (tools || []).filter(
      item => item.enabled !== false && Number(item.deleted ?? 0) === 0
    )
    optionRows.skills = (skills || []).filter(
      item => Number(item.status ?? 1) === 1 && Number(item.deleted ?? 0) === 0
    )
    optionRows.knowledgeBases = (knowledgeBases || []).filter(
      item => Number(item.status ?? 1) === 1 && Number(item.deleted ?? 0) === 0
    )
    optionRows.subagents = (subagents || []).filter(
      item => Number(item.enabled ?? 1) === 1 && Number(item.deleted ?? 0) === 0
    )
  } finally {
    optionsLoading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, createForm())
  activeStep.value = 0
  originalStateStoreType.value = 'local_file'
  invalidPromptName.value = ''
  invalidBindings.value = []
  formRef.value?.clearValidate()
}

const handleCreate = async () => {
  resetForm()
  dialogVisible.value = true
  await loadOptions()
}

const activeBindingIds = bindings => (
  (bindings || []).filter(item => item.available).map(item => item.id)
)

const collectInvalidBindings = data => {
  const groups = [
    ['工具', data.tools],
    ['技能', data.skills],
    ['知识库', data.knowledgeBases],
    ['子智能体', data.subagents]
  ]
  return groups.flatMap(([type, items]) => (
    (items || [])
      .filter(item => !item.available)
      .map(item => ({ type, ...item }))
  ))
}

const handleEdit = async row => {
  resetForm()
  dialogVisible.value = true
  optionsLoading.value = true
  try {
    const [data] = await Promise.all([getAgent(row.id), loadOptions()])
    Object.assign(form, {
      id: data.id,
      agentCode: data.agentCode || '',
      agentName: data.agentName || '',
      description: data.description || '',
      agentVersion: data.agentVersion,
      configVersion: data.configVersion,
      modelId: data.modelId || null,
      sysPromptId: data.systemPromptAvailable ? data.sysPromptId : null,
      maxIters: data.maxIters ?? 10,
      permissionMode: data.permissionMode || 'DEFAULT',
      compactionEnabled: data.compactionEnabled ?? 1,
      triggerMessages: data.triggerMessages ?? 30,
      keepMessages: data.keepMessages ?? 10,
      triggerTokens: data.triggerTokens ?? 6000,
      keepTokens: data.keepTokens ?? 1000,
      toolResultEvictionEnabled: data.toolResultEvictionEnabled ?? 1,
      memoryEnable: data.memoryEnable ?? 1,
      planModeEnabled: data.planModeEnabled ?? 1,
      planFileDirectory: data.planFileDirectory || 'plans',
      taskListEnabled: data.taskListEnabled ?? 1,
      allowShellInPlanMode: data.allowShellInPlanMode ?? 0,
      stateStoreType: data.stateStoreType || 'local_file',
      selectedToolIds: activeBindingIds(data.tools),
      selectedSkillIds: activeBindingIds(data.skills),
      selectedKnowledgeBaseIds: activeBindingIds(data.knowledgeBases),
      selectedSubagentIds: activeBindingIds(data.subagents)
    })
    originalStateStoreType.value = form.stateStoreType
    invalidPromptName.value = data.sysPromptId && !data.systemPromptAvailable
      ? (data.sysPromptName || `提示词 #${data.sysPromptId}`)
      : ''
    invalidBindings.value = collectInvalidBindings(data)
  } catch (error) {
    dialogVisible.value = false
    throw error
  } finally {
    optionsLoading.value = false
  }
}

const validateCurrentStep = async () => {
  if (activeStep.value === 0) {
    await formRef.value?.validateField(['agentCode', 'agentName', 'description'])
  }
  if (activeStep.value === 4 && Number(form.compactionEnabled) === 1) {
    if (Number(form.keepMessages) > Number(form.triggerMessages)) {
      throw new Error('保留消息数不能大于触发消息数')
    }
    if (Number(form.keepTokens) > Number(form.triggerTokens)) {
      throw new Error('保留 Token 不能大于触发 Token')
    }
  }
}

const nextStep = async () => {
  try {
    await validateCurrentStep()
    activeStep.value = Math.min(activeStep.value + 1, steps.length - 1)
  } catch (error) {
    ElMessage.warning(error.message || '请检查当前步骤配置')
  }
}

const previousStep = () => {
  activeStep.value = Math.max(0, activeStep.value - 1)
}

const buildPayload = () => ({
  agentCode: form.agentCode,
  agentName: form.agentName,
  description: form.description,
  agentVersion: form.agentVersion,
  configVersion: form.configVersion,
  modelId: form.modelId || null,
  sysPromptId: form.sysPromptId || null,
  maxIters: Number(form.maxIters),
  permissionMode: form.permissionMode,
  compactionEnabled: Number(form.compactionEnabled),
  triggerMessages: Number(form.triggerMessages),
  keepMessages: Number(form.keepMessages),
  triggerTokens: Number(form.triggerTokens),
  keepTokens: Number(form.keepTokens),
  toolResultEvictionEnabled: Number(form.toolResultEvictionEnabled),
  memoryEnable: Number(form.memoryEnable),
  planModeEnabled: Number(form.planModeEnabled),
  planFileDirectory: form.planFileDirectory,
  taskListEnabled: Number(form.taskListEnabled),
  allowShellInPlanMode: Number(form.allowShellInPlanMode),
  stateStoreType: form.stateStoreType,
  selectedToolIds: form.selectedToolIds,
  selectedSkillIds: form.selectedSkillIds,
  selectedKnowledgeBaseIds: form.selectedKnowledgeBaseIds,
  selectedSubagentIds: form.selectedSubagentIds
})

const submit = async () => {
  await formRef.value?.validate()
  if (!form.modelId) {
    await ElMessageBox.confirm(
      '当前未绑定模型。智能体可以保存，但在绑定可用模型前不能对话或运行。',
      '确认保存未完成配置',
      { type: 'warning', confirmButtonText: '继续保存' }
    )
  }
  if (isEditing.value && form.stateStoreType !== originalStateStoreType.value) {
    await ElMessageBox.confirm(
      '切换会话状态存储不会迁移既有会话状态，原会话可能无法恢复之前的上下文。',
      '确认切换状态存储',
      { type: 'warning', confirmButtonText: '确认切换' }
    )
  }
  submitting.value = true
  try {
    const payload = buildPayload()
    if (isEditing.value) {
      await updateAgent(form.id, payload)
      ElMessage.success('智能体配置已更新')
    } else {
      await createAgent(payload)
      ElMessage.success('智能体已创建')
    }
    dialogVisible.value = false
    await loadPage()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async row => {
  await ElMessageBox.confirm(
    `确定删除智能体“${row.agentName}”吗？配置将被逻辑删除，历史运行仍会保留。`,
    '删除智能体',
    { type: 'warning', confirmButtonText: '删除' }
  )
  await deleteAgent(row.id)
  ElMessage.success('智能体已删除')
  if (rows.value.length === 1 && query.current > 1) query.current -= 1
  await loadPage()
}

const handleChat = row => {
  if (!row.modelId) {
    ElMessage.warning('该智能体未绑定可用模型，请先编辑配置')
    return
  }
  router.push({
    name: 'AgentChat',
    params: { agentId: row.id },
    query: { agentName: row.agentName, agentKey: row.agentCode }
  })
}

const openRuns = async row => {
  Object.assign(runDialog, {
    visible: true,
    agent: row,
    current: 1,
    status: '',
    range: []
  })
  await loadRuns()
}

const loadRuns = async () => {
  if (!runDialog.agent?.id) return
  runDialog.loading = true
  try {
    const params = {
      current: runDialog.current,
      size: runDialog.size,
      status: runDialog.status || undefined,
      start: runDialog.range?.[0] || undefined,
      end: runDialog.range?.[1] || undefined
    }
    const data = await pageAgentRuns(runDialog.agent.id, params)
    runDialog.rows = data?.records || []
    runDialog.total = Number(data?.total || 0)
  } finally {
    runDialog.loading = false
  }
}

const promptName = item => item.promptName || `提示词 #${item.id}`
const toolName = item => item.toolNameExplain || item.toolName || `工具 #${item.id}`
const skillName = item => item.name || item.skillName || item.source || `技能 #${item.id}`
const knowledgeName = item => item.knowledgeName || item.name || `知识库 #${item.id}`
const subagentName = item => item.subagentName || item.name || `子智能体 #${item.id}`

onMounted(async () => {
  await loadPage()
  if (route.query.create === '1') {
    await handleCreate()
  }
})
</script>

<template>
  <section class="agent-page">
    <header class="page-hero">
      <div>
        <span class="eyebrow">AGENT MANAGEMENT</span>
        <h2>智能体</h2>
        <p>统一配置模型、提示词和可调用能力，查看真实运行表现。</p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Refresh" circle @click="loadPage" />
        <el-button type="primary" :icon="Plus" @click="handleCreate">新建智能体</el-button>
      </div>
    </header>

    <div class="metric-grid">
      <article v-for="item in metricCards" :key="item.label" class="metric-card">
        <span class="metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <div>
          <small>{{ item.label }}</small>
          <strong>{{ item.value }}</strong>
          <p>{{ item.note }}</p>
        </div>
      </article>
    </div>

    <article class="list-panel">
      <header class="list-toolbar">
        <div>
          <h3>智能体列表</h3>
          <p>共 {{ formatNumber(total) }} 个智能体</p>
        </div>
        <div class="toolbar-actions">
          <el-input
            v-model="query.keyword"
            clearable
            :prefix-icon="Search"
            placeholder="搜索编码、名称或描述"
            @keyup.enter="search"
            @clear="search"
          />
          <el-button @click="search">查询</el-button>
          <div class="view-switch">
            <button :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">
              <el-icon><MagicStick /></el-icon>
            </button>
            <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
              <el-icon><Menu /></el-icon>
            </button>
          </div>
        </div>
      </header>

      <div v-loading="loading" class="agent-grid" :class="{ list: viewMode === 'list' }">
        <article v-for="row in rows" :key="row.id" class="agent-card">
          <header>
            <span class="agent-avatar">{{ (row.agentName || '智').slice(0, 1) }}</span>
            <div>
              <h4>{{ row.agentName }}</h4>
              <code>{{ row.agentCode }}</code>
            </div>
            <el-dropdown trigger="click">
              <el-button text :icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="Edit" @click="handleEdit(row)">编辑</el-dropdown-item>
                  <el-dropdown-item :icon="TrendCharts" @click="openRuns(row)">运行记录</el-dropdown-item>
                  <el-dropdown-item :icon="Delete" divided @click="handleDelete(row)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </header>
          <p class="description">{{ row.description || '暂无描述' }}</p>
          <div class="model-line" :class="{ missing: !row.modelId }">
            <el-icon><Setting /></el-icon>
            <span>{{ modelLabel(row) }}</span>
          </div>
          <div class="card-data">
            <span><b>{{ row.subagentCount || 0 }}</b> 子智能体</span>
            <span><b>{{ row.todayRuns || 0 }}</b> 今日运行</span>
            <span><b>{{ successText(row) }}</b></span>
          </div>
          <footer>
            <span>更新于 {{ formatDateTime(row.updatedAt) }}</span>
            <el-button type="primary" :icon="ChatDotRound" @click="handleChat(row)">
              对话
            </el-button>
          </footer>
        </article>
        <el-empty v-if="!loading && !rows.length" description="暂无智能体" />
      </div>

      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        background
        layout="total, prev, pager, next"
        :total="total"
        @current-change="loadRows"
      />
    </article>

    <el-dialog
      v-model="dialogVisible"
      class="agent-dialog"
      :title="dialogTitle"
      width="1040px"
      destroy-on-close
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-steps :active="activeStep" align-center class="agent-steps">
        <el-step
          v-for="step in steps"
          :key="step.title"
          :title="step.title"
          :description="step.description"
        />
      </el-steps>

      <el-form
        ref="formRef"
        v-loading="optionsLoading"
        :model="form"
        :rules="rules"
        label-position="top"
        class="agent-form"
      >
        <section v-show="activeStep === 0" class="form-section">
          <div class="section-heading">
            <span><el-icon><Monitor /></el-icon></span>
            <div><h4>基础信息</h4><p>创建后编码永久保留，名称与描述可以继续修改。</p></div>
          </div>
          <div class="form-grid">
            <el-form-item label="智能体编码" prop="agentCode">
              <el-input
                v-model="form.agentCode"
                :disabled="isEditing"
                maxlength="64"
                placeholder="例如 customer-support"
              />
            </el-form-item>
            <el-form-item label="智能体名称" prop="agentName">
              <el-input v-model="form.agentName" maxlength="100" placeholder="请输入展示名称" />
            </el-form-item>
            <el-form-item class="full" label="职责描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="5"
                maxlength="500"
                show-word-limit
                placeholder="说明智能体的职责、能力边界和适用场景"
              />
            </el-form-item>
          </div>
        </section>

        <section v-show="activeStep === 1" class="form-section">
          <div class="section-heading">
            <span><el-icon><Setting /></el-icon></span>
            <div><h4>模型与系统提示词</h4><p>模型与提示词均可暂时留空。</p></div>
          </div>
          <div class="form-grid">
            <el-form-item label="模型配置">
              <el-select v-model="form.modelId" filterable clearable placeholder="选择已启用模型">
                <el-option
                  v-for="item in optionRows.models"
                  :key="item.id"
                  :value="item.id"
                  :label="[item.configName, item.providerName, item.protocol, item.modelName].filter(Boolean).join(' · ')"
                />
              </el-select>
              <small v-if="!form.modelId" class="field-warning">
                未绑定模型时可以保存，但不能发起对话或运行。
              </small>
            </el-form-item>
            <el-form-item label="系统提示词模板">
              <el-select v-model="form.sysPromptId" filterable clearable placeholder="选择可用提示词">
                <el-option
                  v-for="item in optionRows.prompts"
                  :key="item.id"
                  :value="item.id"
                  :label="promptName(item)"
                />
              </el-select>
              <small v-if="invalidPromptName" class="field-warning">
                原提示词“{{ invalidPromptName }}”已失效，保存后将清除引用。
              </small>
            </el-form-item>
            <el-form-item class="full" label="提示词内容预览">
              <el-input
                :model-value="selectedPrompt?.sysPrompt || '未选择系统提示词模板'"
                type="textarea"
                :rows="9"
                disabled
              />
            </el-form-item>
          </div>
        </section>

        <section v-show="activeStep === 2" class="form-section">
          <div class="section-heading">
            <span><el-icon><MagicStick /></el-icon></span>
            <div><h4>能力配置</h4><p>选择运行时可用的工具、技能和子智能体。</p></div>
          </div>
          <el-alert
            v-if="invalidBindings.length"
            type="warning"
            :closable="false"
            show-icon
            title="存在已失效绑定"
          >
            <template #default>
              {{ invalidBindings.map(item => `${item.type}“${item.name}”`).join('、') }}
              将在本次保存时自动解除。
            </template>
          </el-alert>
          <div class="capability-columns">
            <article class="capability-box">
              <header><h5>工具</h5><b>{{ form.selectedToolIds.length }}</b></header>
              <el-checkbox-group v-model="form.selectedToolIds">
                <el-checkbox v-for="item in optionRows.tools" :key="item.id" :value="item.id">
                  <span><strong>{{ toolName(item) }}</strong><small>{{ item.description || item.toolName }}</small></span>
                </el-checkbox>
              </el-checkbox-group>
              <el-empty v-if="!optionRows.tools.length" description="暂无可用工具" :image-size="60" />
            </article>
            <article class="capability-box">
              <header><h5>技能</h5><b>{{ form.selectedSkillIds.length }}</b></header>
              <el-checkbox-group v-model="form.selectedSkillIds">
                <el-checkbox v-for="item in optionRows.skills" :key="item.id" :value="item.id">
                  <span><strong>{{ skillName(item) }}</strong><small>{{ item.description || item.source }}</small></span>
                </el-checkbox>
              </el-checkbox-group>
              <el-empty v-if="!optionRows.skills.length" description="暂无可用技能" :image-size="60" />
            </article>
            <article class="capability-box">
              <header><h5>子智能体</h5><b>{{ form.selectedSubagentIds.length }}</b></header>
              <el-checkbox-group v-model="form.selectedSubagentIds">
                <el-checkbox v-for="item in optionRows.subagents" :key="item.id" :value="item.id">
                  <span><strong>{{ subagentName(item) }}</strong><small>{{ item.description || item.subagentCode }}</small></span>
                </el-checkbox>
              </el-checkbox-group>
              <el-empty v-if="!optionRows.subagents.length" description="暂无可用子智能体" :image-size="60" />
            </article>
          </div>
        </section>

        <section v-show="activeStep === 3" class="form-section">
          <div class="section-heading">
            <span><el-icon><Files /></el-icon></span>
            <div><h4>知识库</h4><p>智能体可按需检索已绑定且当前可用的知识库。</p></div>
          </div>
          <el-checkbox-group v-model="form.selectedKnowledgeBaseIds" class="knowledge-grid">
            <el-checkbox
              v-for="item in optionRows.knowledgeBases"
              :key="item.id"
              :value="item.id"
            >
              <span>
                <strong>{{ knowledgeName(item) }}</strong>
                <small>{{ item.description || item.knowledgeCode || '暂无描述' }}</small>
              </span>
            </el-checkbox>
          </el-checkbox-group>
          <el-empty
            v-if="!optionRows.knowledgeBases.length"
            description="暂无可用知识库"
            :image-size="72"
          />
        </section>

        <section v-show="activeStep === 4" class="form-section">
          <div class="section-heading">
            <span><el-icon><Warning /></el-icon></span>
            <div><h4>高级配置</h4><p>这里只展示已经落库并被运行时读取的配置。</p></div>
          </div>

          <div class="advanced-grid">
            <article class="advanced-card">
              <header><h5>运行与权限</h5></header>
              <div class="form-grid compact">
                <el-form-item label="最大循环次数">
                  <el-input-number v-model="form.maxIters" :min="1" :max="100" />
                </el-form-item>
                <el-form-item label="权限模式">
                  <el-select v-model="form.permissionMode">
                    <el-option
                      v-for="item in permissionOptions"
                      :key="item.value"
                      :value="item.value"
                      :label="item.label"
                    />
                  </el-select>
                </el-form-item>
                <label class="switch-row">
                  <span><b>长期记忆</b><small>启用记忆工具与钩子</small></span>
                  <el-switch v-model="form.memoryEnable" :active-value="1" :inactive-value="0" />
                </label>
                <label class="switch-row">
                  <span><b>大工具结果卸载</b><small>减少上下文占用</small></span>
                  <el-switch
                    v-model="form.toolResultEvictionEnabled"
                    :active-value="1"
                    :inactive-value="0"
                  />
                </label>
              </div>
            </article>

            <article class="advanced-card">
              <header>
                <h5>上下文压缩</h5>
                <el-switch v-model="form.compactionEnabled" :active-value="1" :inactive-value="0" />
              </header>
              <div class="form-grid compact" :class="{ disabled: !form.compactionEnabled }">
                <el-form-item label="触发消息数">
                  <el-input-number v-model="form.triggerMessages" :min="0" />
                </el-form-item>
                <el-form-item label="保留消息数">
                  <el-input-number v-model="form.keepMessages" :min="0" />
                </el-form-item>
                <el-form-item label="触发 Token">
                  <el-input-number v-model="form.triggerTokens" :min="0" />
                </el-form-item>
                <el-form-item label="保留 Token">
                  <el-input-number v-model="form.keepTokens" :min="0" />
                </el-form-item>
              </div>
            </article>

            <article class="advanced-card">
              <header>
                <h5>计划模式</h5>
                <el-switch v-model="form.planModeEnabled" :active-value="1" :inactive-value="0" />
              </header>
              <div class="form-grid compact" :class="{ disabled: !form.planModeEnabled }">
                <el-form-item label="计划文件目录">
                  <el-input v-model="form.planFileDirectory" placeholder="plans" />
                </el-form-item>
                <label class="switch-row">
                  <span><b>任务列表</b><small>启用 todo_write</small></span>
                  <el-switch v-model="form.taskListEnabled" :active-value="1" :inactive-value="0" />
                </label>
                <label class="switch-row full">
                  <span><b>Plan 阶段允许 Shell</b><small>仅在确有需要时开启</small></span>
                  <el-switch
                    v-model="form.allowShellInPlanMode"
                    :active-value="1"
                    :inactive-value="0"
                  />
                </label>
              </div>
            </article>

            <article class="advanced-card">
              <header><h5>会话状态存储</h5></header>
              <el-radio-group v-model="form.stateStoreType" class="state-store-options">
                <el-radio-button value="local_file">本地文件</el-radio-button>
                <el-radio-button value="redis">Redis</el-radio-button>
              </el-radio-group>
              <p class="store-note">
                切换存储不会迁移既有会话状态；选择 Redis 时后端将在保存前检查连通性。
              </p>
            </article>
          </div>
        </section>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <span>第 {{ activeStep + 1 }} / {{ steps.length }} 步</span>
          <div>
            <el-button v-if="activeStep === 0" @click="dialogVisible = false">取消</el-button>
            <el-button v-else @click="previousStep">上一步</el-button>
            <el-button v-if="activeStep < steps.length - 1" type="primary" @click="nextStep">
              下一步
            </el-button>
            <el-button v-else type="primary" :loading="submitting" @click="submit">
              {{ isEditing ? '保存修改' : '创建智能体' }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="runDialog.visible"
      :title="`${runDialog.agent?.agentName || ''} · 运行记录`"
      width="980px"
    >
      <div class="run-filters">
        <el-select v-model="runDialog.status" clearable placeholder="全部状态" @change="loadRuns">
          <el-option label="运行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-date-picker
          v-model="runDialog.range"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          @change="() => { runDialog.current = 1; loadRuns() }"
        />
      </div>
      <el-table v-loading="runDialog.loading" :data="runDialog.rows">
        <el-table-column prop="id" label="运行 ID" min-width="170" />
        <el-table-column prop="sessionId" label="会话 ID" min-width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : row.status === 'RUNNING' ? 'primary' : 'info'"
            >
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="165">
          <template #default="{ row }">{{ formatDateTime(row.startedAt) }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ row.durationMs == null ? '--' : formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="220" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="runDialog.current"
        v-model:page-size="runDialog.size"
        background
        layout="total, prev, pager, next"
        :total="runDialog.total"
        @current-change="loadRuns"
      />
    </el-dialog>
  </section>
</template>

<style scoped>
.agent-page {
  min-height: 100%;
  padding: 24px;
  color: #182230;
  background:
    radial-gradient(circle at 92% 0%, rgba(80, 109, 255, 0.08), transparent 27%),
    #f5f7fb;
}

.page-hero,
.list-toolbar,
.agent-card header,
.agent-card footer,
.metric-card,
.section-heading,
.capability-box header,
.advanced-card header,
.switch-row,
.dialog-footer,
.run-filters {
  display: flex;
  align-items: center;
}

.page-hero {
  justify-content: space-between;
  margin-bottom: 20px;
}

.eyebrow {
  color: #60708a;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.page-hero h2 {
  margin: 5px 0 3px;
  font-size: 30px;
}

.page-hero p,
.list-toolbar p,
.section-heading p,
.store-note {
  margin: 0;
  color: #758197;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.metric-card {
  gap: 14px;
  min-height: 112px;
  padding: 18px;
  border: 1px solid #e7ebf2;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 25px rgba(31, 45, 72, 0.04);
}

.metric-icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  place-items: center;
  border-radius: 13px;
  font-size: 21px;
}

.metric-icon.blue { color: #3867ed; background: #edf2ff; }
.metric-icon.green { color: #15936f; background: #e9f8f3; }
.metric-icon.violet { color: #7b54d8; background: #f2edff; }
.metric-icon.amber { color: #c27a19; background: #fff4df; }

.metric-card small,
.metric-card p {
  color: #7d889b;
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 4px 0;
  font-size: 25px;
}

.metric-card p {
  margin: 0;
}

.list-panel {
  padding: 20px;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: #fff;
}

.list-toolbar {
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.list-toolbar h3 {
  margin: 0 0 3px;
  font-size: 18px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.toolbar-actions .el-input {
  width: 280px;
}

.view-switch {
  display: flex;
  padding: 3px;
  border: 1px solid #e1e6ee;
  border-radius: 9px;
  background: #f6f8fb;
}

.view-switch button {
  display: grid;
  width: 30px;
  height: 28px;
  place-items: center;
  border: 0;
  border-radius: 6px;
  color: #8390a5;
  background: transparent;
  cursor: pointer;
}

.view-switch button.active {
  color: #3567e9;
  background: #fff;
  box-shadow: 0 2px 7px rgba(36, 54, 89, 0.12);
}

.agent-grid {
  display: grid;
  min-height: 210px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.agent-grid.list {
  grid-template-columns: 1fr;
}

.agent-card {
  min-width: 0;
  padding: 17px;
  border: 1px solid #e5eaf2;
  border-radius: 15px;
  background: linear-gradient(180deg, #fff, #fbfcfe);
  transition: 0.2s ease;
}

.agent-card:hover {
  border-color: #bfcdf3;
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(43, 65, 108, 0.08);
}

.agent-card header {
  gap: 11px;
}

.agent-card header > div {
  min-width: 0;
  flex: 1;
}

.agent-avatar {
  display: grid;
  width: 39px;
  height: 39px;
  place-items: center;
  border-radius: 12px;
  color: #fff;
  font-weight: 700;
  background: linear-gradient(135deg, #4477ed, #7459d9);
}

.agent-card h4 {
  overflow: hidden;
  margin: 0 0 2px;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-card code {
  color: #77849a;
  font-size: 11px;
}

.description {
  display: -webkit-box;
  min-height: 42px;
  overflow: hidden;
  margin: 15px 0;
  color: #66738a;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.model-line {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 7px;
  padding: 9px 10px;
  border-radius: 9px;
  color: #3b5ca8;
  background: #f0f4ff;
  font-size: 12px;
}

.model-line.missing {
  color: #b36c0e;
  background: #fff5e5;
}

.model-line span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-data {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 13px 0;
}

.card-data span {
  color: #8a95a7;
  font-size: 11px;
}

.card-data b {
  display: block;
  overflow: hidden;
  color: #364157;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-card footer {
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #edf0f5;
}

.agent-card footer > span {
  color: #97a0ae;
  font-size: 11px;
}

.el-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

.agent-steps {
  margin: 0 12px 24px;
}

.agent-form {
  min-height: 470px;
}

.form-section {
  padding: 3px 5px;
}

.section-heading {
  gap: 12px;
  margin-bottom: 18px;
}

.section-heading > span {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  color: #416ae5;
  background: #edf2ff;
  font-size: 19px;
}

.section-heading h4 {
  margin: 0 0 3px;
  font-size: 17px;
}

.section-heading p {
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
}

.form-grid .full {
  grid-column: 1 / -1;
}

.field-warning {
  display: block;
  margin-top: 6px;
  color: #bb7418;
  line-height: 1.5;
}

.capability-columns {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.capability-box,
.advanced-card {
  padding: 15px;
  border: 1px solid #e4e9f1;
  border-radius: 13px;
  background: #fbfcfe;
}

.capability-box header,
.advanced-card header {
  justify-content: space-between;
  margin-bottom: 12px;
}

.capability-box h5,
.advanced-card h5 {
  margin: 0;
  font-size: 14px;
}

.capability-box header b {
  display: grid;
  min-width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 8px;
  color: #416ae5;
  background: #eaf0ff;
}

.capability-box .el-checkbox-group {
  display: flex;
  max-height: 310px;
  overflow-y: auto;
  flex-direction: column;
  gap: 8px;
}

.capability-box :deep(.el-checkbox),
.knowledge-grid :deep(.el-checkbox) {
  height: auto;
  margin: 0;
  padding: 10px;
  border: 1px solid #e7ebf1;
  border-radius: 9px;
  background: #fff;
  white-space: normal;
}

.capability-box :deep(.el-checkbox__label),
.knowledge-grid :deep(.el-checkbox__label) {
  min-width: 0;
  white-space: normal;
}

.capability-box span,
.knowledge-grid span {
  display: block;
}

.capability-box strong,
.knowledge-grid strong {
  display: block;
  color: #354057;
  font-size: 12px;
}

.capability-box small,
.knowledge-grid small {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 2px;
  color: #8b95a7;
  font-size: 11px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 11px;
}

.advanced-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 13px;
}

.form-grid.compact {
  gap: 0 12px;
}

.form-grid.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.switch-row {
  justify-content: space-between;
  min-height: 52px;
  padding: 0 2px;
}

.switch-row span,
.switch-row small {
  display: block;
}

.switch-row b {
  font-size: 12px;
}

.switch-row small {
  margin-top: 2px;
  color: #8b95a7;
  font-size: 11px;
}

.state-store-options {
  width: 100%;
}

.state-store-options :deep(.el-radio-button) {
  width: 50%;
}

.state-store-options :deep(.el-radio-button__inner) {
  width: 100%;
}

.store-note {
  margin-top: 12px;
  font-size: 11px;
  line-height: 1.6;
}

.dialog-footer {
  justify-content: space-between;
}

.dialog-footer > span {
  color: #8a95a7;
  font-size: 12px;
}

.run-filters {
  gap: 10px;
  margin-bottom: 14px;
}

.run-filters .el-select {
  width: 150px;
}

@media (max-width: 1180px) {
  .metric-grid,
  .agent-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .agent-page { padding: 14px; }
  .page-hero,
  .list-toolbar { align-items: flex-start; flex-direction: column; }
  .metric-grid,
  .agent-grid,
  .capability-columns,
  .knowledge-grid,
  .advanced-grid,
  .form-grid { grid-template-columns: 1fr; }
  .toolbar-actions { width: 100%; flex-wrap: wrap; }
  .toolbar-actions .el-input { width: 100%; }
  .form-grid .full { grid-column: auto; }
}
</style>
