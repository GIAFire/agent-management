<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Briefcase,
  Check,
  Close,
  Collection,
  Connection,
  Coin,
  Files,
  FolderOpened,
  Grid,
  Lightning,
  Lock,
  MagicStick,
  Menu,
  Monitor,
  MoreFilled,
  Notebook,
  Plus,
  Search,
  SetUp,
  TrendCharts,
  Tools
} from '@element-plus/icons-vue'
import { createAgentFull, deleteAgent, getAgent, getAgentInfoList, updateAgent } from '@/axios/agent'
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
const wizardLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('创建智能体')
const wizardStep = ref(1)
const formRef = ref()
const agentRows = ref([])
const searchKeyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const viewMode = ref('grid')
const currentPage = ref(1)
const pageSize = ref(6)
const expandedResource = ref('')
const modelRows = ref([])
const promptRows = ref([])
const toolRows = ref([])
const knowledgeRows = ref([])
const subagentRows = ref([])

const sampleRows = []

const form = reactive({
  id: null,
  agentKey: '',
  agentName: '',
  description: '',
  agentType: 'HARNESS',
  status: 1
})

const configForm = reactive({
  versionNo: 'v1',
  sysPromptId: null,
  modelId: 1,
  modelName: 'Qwen3-235B',
  promptTemplate: '企业通用助手',
  sysPrompt: '你是一名专业、可靠的企业智能助手。请先理解用户目标，再合理使用工具完成任务。',
  maxIters: 10,
  workspacePath: '.agentscope/workspace',
  permissionMode: 'ACCEPT_EDITS',
  visualSchemaJson: '',
  agentPermissionPolicyId: null,
  publishStatus: 1,
  contextEnabled: 1,
  triggerMessages: 30,
  keepMessages: 10,
  triggerTokens: 6000,
  keepTokens: 0,
  flushBeforeCompact: 1,
  offloadBeforeCompact: 1,
  compactionModelConfigId: null,
  truncateArgsEnabled: 0,
  truncateArgsMaxChars: null,
  toolResultEvictionEnabled: 1,
  toolResultMaxChars: 20000,
  memoryEnable: 1,
  planModeEnabled: 0,
  planFileDirectory: 'plans',
  taskListEnabled: 0,
  allowShellInPlanMode: 0,
  planExitApprovalRequired: 1,
  planMaxSteps: 20,
  planAutoEnterEnabled: 1,
  planPrompt: '',
  sandboxEnabled: 0,
  sandboxConfigId: null
})

const selections = reactive({
  toolIds: [],
  skillIds: [],
  knowledgeBaseIds: [],
  subagentIds: [],
  tools: [],
  skills: [],
  knowledge: [],
  mcp: [],
  subAgents: []
})

const rules = {
  agentKey: [{ required: true, message: '请输入智能体英文名称', trigger: 'blur' }],
  agentName: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
  agentType: [{ required: true, message: '请选择智能体类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const wizardSteps = [
  { title: '选择类型', desc: '选择推理和协作模式' },
  { title: '基本信息', desc: '配置智能体的基本信息' },
  { title: '模型与提示词', desc: '选择模型和提示词模板' },
  { title: '工具与技能', desc: '配置钩子、工具、技能和敏感词' },
  { title: '知识库', desc: '配置知识库、MCP 服务和子智能体' },
  { title: '高级配置', desc: '配置计划、记忆、执行环境和 Studio 等' }
]

const agentTypes = [
  {
    value: 'HARNESS',
    title: 'Harness Agent',
    desc: '面向复杂任务，支持 Plan、Workspace、Sandbox 与多 Agent 协作',
    icon: Briefcase,
    tag: '推荐'
  },
  {
    value: 'REACT',
    title: 'ReAct Agent',
    desc: '通过“思考-行动-观察”循环处理轻量工具调用任务',
    icon: MagicStick
  },
  {
    value: 'TEMPLATE',
    title: '从企业模板创建',
    desc: '复用已验证的模型、工具、知识库与权限组合',
    icon: Lightning
  }
]

const capabilityCards = computed(() => [
  { key: 'tools', title: '工具', desc: '查询订单、文件读写、数据库访问', count: `${selections.toolIds.length} 个已选择`, icon: Tools },
  { key: 'skills', title: '技能包', desc: '组合提示词、脚本和资源', count: `${selections.skillIds.length} 个已选择`, icon: Lightning }
])

const knowledgeCards = computed(() => [
  { key: 'knowledge', title: '知识库', desc: '产品手册、合同法规库', action: `已选择 ${selections.knowledgeBaseIds.length} 个`, icon: Collection },
  { key: 'mcp', title: 'MCP 服务器', desc: '数据库查询、GitHub、内部工单', action: '配置连接', icon: Connection },
  { key: 'subagents', title: '子智能体', desc: '将任务委派给编程、检索或审查 Agent', action: `已选择 ${selections.subagentIds.length} 个`, icon: Briefcase }
])

const modelOptions = computed(() => modelRows.value.map((item) => ({
  label: [item.modelName, item.description].filter(Boolean).join(' - ') || `模型 #${item.id}`,
  value: item.id,
  name: item.modelName || `模型 #${item.id}`
})))

const promptOptions = computed(() => promptRows.value.map((item) => ({
  label: item.promptName || `提示词 #${item.id}`,
  value: item.id,
  sysPrompt: item.sysPrompt || ''
})))
// const permissionOptions = ['DEFAULT', 'ACCEPT_EDITS', 'EXPLORE', 'BYPASS','DONT_ASK']
const permissionOptions = [
  { label: '所有操作都需要显式规则或用户确认', value: 'DEFAULT' },
  { label: '自动放行工作目录内的文件操作', value: 'ACCEPT_EDITS' },
  { label: '只读：拒绝所有写与命令', value: 'EXPLORE' },
  { label: '放行一切', value: 'BYPASS' },
  { label: '把所有 询问 转为 拒绝', value: 'DONT_ASK' }
]
const publishOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已废弃', value: 2 }
]

const sourceRows = computed(() => agentRows.value.length ? agentRows.value : sampleRows)

const getRowStatus = (row) => Number(row?.agentStatus ?? row?.status ?? 0)

const getRowType = (row) => row?.agentType || row?.type || 'HARNESS'

const getRowModel = (row) => row?.modelName || row?.model || row?.defaultModel || 'Qwen3-235B'

const getRowDescription = (row) => row?.agentDescription || row?.description || '暂无描述'

const getSubagentCount = (row) => {
  if (Array.isArray(row?.subagents)) {
    return row.subagents.length
  }
  return Number(row?.subagentCount ?? row?.subAgentCount ?? row?.childAgentCount ?? 0)
}

const filteredRows = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return sourceRows.value.filter((row) => {
    const text = [
      row.agentName,
      row.agentKey,
      row.agentCode,
      getRowDescription(row),
      getRowModel(row)
    ].filter(Boolean).join(' ').toLowerCase()
    const matchesKeyword = !keyword || text.includes(keyword)
    const matchesType = !typeFilter.value || getRowType(row) === typeFilter.value
    const matchesStatus = statusFilter.value === '' || getRowStatus(row) === Number(statusFilter.value)
    return matchesKeyword && matchesType && matchesStatus
  })
})

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const stats = computed(() => {
  const total = sourceRows.value.length
  const running = sourceRows.value.filter((row) => getRowStatus(row) === 1).length
  const successRate = total ? Math.min(99.8, 96 + (running / total) * 3).toFixed(1) : '98.2'
  return [
    { label: '智能体总数', value: total || 0, note: `较昨日 +${Math.max(1, running || 1)}`, icon: Monitor, tone: 'blue' },
    { label: '今日消耗 Token', value: '8.42M', note: '较昨日 +14.6% ↑', icon: Coin, tone: 'green' },
    { label: '今日运行', value: '1,286', note: '较昨日 +12.6%', icon: TrendCharts, tone: 'purple' },
    { label: '平均成功率', value: `${successRate}%`, note: '平均响应 2.4s', icon: Lock, tone: 'orange' }
  ]
})

const typeOptions = computed(() => {
  const options = Array.from(new Set(sourceRows.value.map((row) => getRowType(row)).filter(Boolean)))
  return options.length ? options : ['HARNESS', 'REACT']
})

const emptyText = computed(() => loading.value ? '正在加载智能体' : '暂无智能体')

const configProgress = computed(() => {
  let score = 20
  if (form.agentName && form.agentKey) score += 15
  if (configForm.modelId && configForm.sysPrompt) score += 15
  if (selections.toolIds.length || selections.skillIds.length) score += 15
  if (selections.knowledgeBaseIds.length || selections.subagentIds.length || selections.mcp.length) score += 10
  if (configForm.contextEnabled) score += 10
  if (configForm.memoryEnable) score += 7
  if (configForm.sandboxEnabled) score += 8
  return Math.min(score, 100)
})

const statusMeta = (status) => {
  const value = Number(status)
  if (value === 1) {
    return { text: '已发布', className: 'published', detail: '成功率 98.7%' }
  }
  if (value === 2) {
    return { text: '草稿', className: 'draft', detail: '尚未发布' }
  }
  return { text: '已停用', className: 'muted', detail: '已停用' }
}

const resetPage = () => {
  currentPage.value = 1
}

const resetConfigForm = () => {
  Object.assign(configForm, {
    versionNo: 'v1',
    sysPromptId: null,
    modelId: 1,
    modelName: 'Qwen3-235B',
    promptTemplate: '企业通用助手',
    sysPrompt: '你是一名专业、可靠的企业智能助手。请先理解用户目标，再合理使用工具完成任务。',
    maxIters: 10,
    workspacePath: '.agentscope/workspace',
    permissionMode: 'ACCEPT_EDITS',
    visualSchemaJson: '',
    agentPermissionPolicyId: null,
    publishStatus: 1,
    contextEnabled: 1,
    triggerMessages: 30,
    keepMessages: 10,
    triggerTokens: 6000,
    keepTokens: 0,
    flushBeforeCompact: 1,
    offloadBeforeCompact: 1,
    compactionModelConfigId: null,
    truncateArgsEnabled: 0,
    truncateArgsMaxChars: null,
    toolResultEvictionEnabled: 1,
    toolResultMaxChars: 20000,
    memoryEnable: 1,
    planModeEnabled: 0,
    planFileDirectory: 'plans',
    taskListEnabled: 0,
    allowShellInPlanMode: 0,
    planExitApprovalRequired: 1,
    planMaxSteps: 20,
    planAutoEnterEnabled: 1,
    planPrompt: '',
    sandboxEnabled: 0,
    sandboxConfigId: null
  })
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    agentKey: '',
    agentName: '',
    description: '',
    agentType: 'HARNESS',
    status: 1
  })
  resetConfigForm()
  selections.toolIds.splice(0)
  selections.skillIds.splice(0)
  selections.knowledgeBaseIds.splice(0)
  selections.subagentIds.splice(0)
  selections.tools.splice(0)
  selections.skills.splice(0)
  selections.knowledge.splice(0)
  selections.mcp.splice(0)
  selections.subAgents.splice(0)
  expandedResource.value = ''
}

const normalizeId = (value) => {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

const normalizeNumber = (value) => {
  return value === '' || value === undefined || value === null ? null : Number(value)
}

const toSwitchValue = (value) => Number(value) === 1 ? 1 : 0

const toggleId = (list, id) => {
  const normalized = normalizeId(id)
  if (!normalized) {
    return
  }
  const index = list.findIndex((item) => normalizeId(item) === normalized)
  if (index >= 0) {
    list.splice(index, 1)
    return
  }
  list.push(id)
}

const isSelected = (list, id) => {
  const normalized = normalizeId(id)
  return list.some((item) => normalizeId(item) === normalized)
}

const loadAgentList = async () => {
  loading.value = true
  try {
    const data = await getAgentInfoList()
    agentRows.value = Array.isArray(data) ? data : []
  } catch {
    agentRows.value = []
  } finally {
    loading.value = false
  }
}

const loadModelAndPrompts = async () => {
  if (modelRows.value.length && promptRows.value.length) {
    return
  }
  wizardLoading.value = true
  try {
    const [models, prompts] = await Promise.all([
      listWizardModels(),
      listWizardPrompts()
    ])
    modelRows.value = Array.isArray(models) ? models : []
    promptRows.value = Array.isArray(prompts) ? prompts : []
    if ((!configForm.modelId || !modelRows.value.some((item) => normalizeId(item.id) === normalizeId(configForm.modelId))) && modelRows.value[0]?.id) {
      configForm.modelId = modelRows.value[0].id
      configForm.modelName = modelRows.value[0].modelName || ''
    }
    if (!configForm.sysPromptId && promptRows.value[0]?.id) {
      configForm.sysPromptId = promptRows.value[0].id
      configForm.promptTemplate = promptRows.value[0].promptName || ''
      configForm.sysPrompt = promptRows.value[0].sysPrompt || configForm.sysPrompt
    }
  } finally {
    wizardLoading.value = false
  }
}

const loadTools = async () => {
  if (toolRows.value.length) {
    return
  }
  wizardLoading.value = true
  try {
    const data = await listWizardTools()
    toolRows.value = Array.isArray(data) ? data : []
  } finally {
    wizardLoading.value = false
  }
}

const loadKnowledgeAndSubagents = async () => {
  if (knowledgeRows.value.length && subagentRows.value.length) {
    return
  }
  wizardLoading.value = true
  try {
    const [knowledgeBases, subagents] = await Promise.all([
      listWizardKnowledgeBases(),
      listWizardSubagents()
    ])
    knowledgeRows.value = Array.isArray(knowledgeBases) ? knowledgeBases : []
    subagentRows.value = Array.isArray(subagents) ? subagents : []
  } finally {
    wizardLoading.value = false
  }
}

const ensureStepData = async (step) => {
  if (step === 3) {
    await loadModelAndPrompts()
  }
  if (step === 4) {
    await loadTools()
  }
  if (step === 5) {
    await loadKnowledgeAndSubagents()
  }
}

const handleAdd = async () => {
  dialogTitle.value = '创建智能体'
  wizardStep.value = 1
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
  await ensureStepData(3)
}

const handleEdit = async (row) => {
  if (String(row.id).startsWith('demo-')) {
    ElMessage.info('示例智能体仅用于展示，请先创建真实智能体')
    return
  }

  dialogTitle.value = '编辑智能体'
  wizardStep.value = 2
  resetForm()
  const data = await getAgent(row.id)
  Object.assign(form, {
    id: data?.id,
    agentKey: data?.agentKey || data?.agentCode || '',
    agentName: data?.agentName || '',
    description: data?.description || '',
    agentType: data?.agentType || 'HARNESS',
    status: Number(data?.status ?? 1)
  })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const buildAgentPayload = () => ({
  id: normalizeId(form.id),
  agentKey: form.agentKey,
  agentCode: form.agentKey,
  agentName: form.agentName,
  description: form.description,
  agentType: form.agentType === 'TEMPLATE' ? 'HARNESS' : form.agentType,
  status: form.status
})

const buildVisualSchema = () => {
  if (configForm.visualSchemaJson?.trim()) {
    return configForm.visualSchemaJson
  }

  return JSON.stringify({
    nodes: [
      { id: 'model', type: 'model', label: configForm.modelName },
      { id: 'tools', type: 'tools', label: `${selections.toolIds.length} tools`, toolIds: selections.toolIds },
      { id: 'knowledge', type: 'knowledge', label: `${selections.knowledgeBaseIds.length} knowledge bases`, knowledgeBaseIds: selections.knowledgeBaseIds },
      { id: 'subagents', type: 'subagents', label: `${selections.subagentIds.length} subagents`, subagentIds: selections.subagentIds }
    ],
    edges: [
      { source: 'model', target: 'tools' },
      { source: 'model', target: 'knowledge' }
    ]
  })
}

const buildCreateAgentPayload = () => ({
  ...buildAgentPayload(),
  agentDescription: form.description,
  agentStatus: form.status,
  tenantId: 1,
  versionNo: configForm.versionNo,
  sysPromptId: normalizeId(configForm.sysPromptId),
  sysPrompt: configForm.sysPrompt,
  promptName: promptOptions.value.find((item) => normalizeId(item.value) === normalizeId(configForm.sysPromptId))?.label || configForm.promptTemplate,
  modelId: normalizeId(configForm.modelId),
  modelName: configForm.modelName,
  maxIters: normalizeNumber(configForm.maxIters),
  workspacePath: configForm.workspacePath,
  permissionMode: configForm.permissionMode,
  visualSchemaJson: buildVisualSchema(),
  agentPermissionPolicyId: normalizeId(configForm.agentPermissionPolicyId),
  publishStatus: configForm.publishStatus,
  contextEnabled: toSwitchValue(configForm.contextEnabled),
  triggerMessages: normalizeNumber(configForm.triggerMessages),
  keepMessages: normalizeNumber(configForm.keepMessages),
  triggerTokens: normalizeNumber(configForm.triggerTokens),
  keepTokens: normalizeNumber(configForm.keepTokens),
  flushBeforeCompact: toSwitchValue(configForm.flushBeforeCompact),
  offloadBeforeCompact: toSwitchValue(configForm.offloadBeforeCompact),
  compactionModelConfigId: normalizeId(configForm.compactionModelConfigId),
  truncateArgsEnabled: toSwitchValue(configForm.truncateArgsEnabled) === 1,
  truncateArgsMaxChars: normalizeNumber(configForm.truncateArgsMaxChars),
  toolResultEvictionEnabled: toSwitchValue(configForm.toolResultEvictionEnabled) === 1,
  toolResultMaxChars: normalizeNumber(configForm.toolResultMaxChars),
  memoryEnable: toSwitchValue(configForm.memoryEnable),
  planModeEnabled: toSwitchValue(configForm.planModeEnabled),
  planFileDirectory: configForm.planFileDirectory,
  taskListEnabled: toSwitchValue(configForm.taskListEnabled),
  allowShellInPlanMode: toSwitchValue(configForm.allowShellInPlanMode),
  planExitApprovalRequired: toSwitchValue(configForm.planExitApprovalRequired),
  planMaxSteps: normalizeNumber(configForm.planMaxSteps),
  planAutoEnterEnabled: toSwitchValue(configForm.planAutoEnterEnabled),
  planPrompt: configForm.planPrompt,
  sandboxEnabled: toSwitchValue(configForm.sandboxEnabled),
  sandboxConfigId: normalizeId(configForm.sandboxConfigId),
  selectedToolIds: selections.toolIds.map(normalizeId).filter(Boolean),
  selectedKnowledgeBaseIds: selections.knowledgeBaseIds.map(normalizeId).filter(Boolean),
  selectedSubagentIds: selections.subagentIds.map(normalizeId).filter(Boolean)
})

const validateCurrentStep = async () => {
  if (wizardStep.value === 2) {
    await formRef.value?.validateField(['agentName', 'agentKey'])
  }
  if (wizardStep.value === 3 && !configForm.modelId) {
    ElMessage.warning('请选择默认模型')
    throw new Error('model required')
  }
}

const nextStep = async () => {
  await validateCurrentStep()
  const next = Math.min(wizardStep.value + 1, wizardSteps.length)
  await ensureStepData(next)
  wizardStep.value = next
}

const prevStep = () => {
  wizardStep.value = Math.max(wizardStep.value - 1, 1)
}

const submitForm = async () => {
  await formRef.value.validate()
  if (!configForm.modelId) {
    ElMessage.warning('请选择默认模型')
    return
  }

  submitting.value = true
  try {
    if (form.id) {
      await updateAgent(buildAgentPayload())
      ElMessage.success('智能体更新成功')
    } else {
      await createAgentFull(buildCreateAgentPayload())
      ElMessage.success('智能体与完整配置创建成功')
    }
    dialogVisible.value = false
    await loadAgentList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  if (String(row.id).startsWith('demo-')) {
    ElMessage.info('示例智能体仅用于展示')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除智能体「${row.agentName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteAgent(row.id)
  ElMessage.success('智能体删除成功')
  await loadAgentList()
}

const handleChat = (row) => {
  router.push({
    name: 'AgentChat',
    params: { agentId: row.agentId || row.id },
    query: {
      agentName: row.agentName || undefined,
      agentKey: row.agentKey || row.agentCode || undefined
    }
  })
}

onMounted(async () => {
  await loadAgentList()
  if (route.query.create === '1') {
    await handleAdd()
  }
})
</script>

<template>
  <section class="agent-center-page">
    <div class="page-hero agent-hero">
      <div>
        <h2>智能体中心</h2>
        <p>创建、配置和运行智能体，统一管理智能体能力与状态</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新建智能体</el-button>
      </div>
    </div>

    <div class="agent-stats">
      <article v-for="item in stats" :key="item.label" class="agent-stat-card">
        <span class="metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small><i />{{ item.note }}</small>
        </div>
      </article>
    </div>

    <article class="agent-list-panel">
      <div class="agent-list-header">
        <h3>智能体列表</h3>
        <div class="agent-list-tools">
          <el-input
            v-model="searchKeyword"
            class="agent-search"
            :prefix-icon="Search"
            clearable
            placeholder="搜索智能体名称或描述"
            @input="resetPage"
          />
          <el-select v-model="typeFilter" class="agent-filter" placeholder="全部类型" clearable @change="resetPage">
            <el-option label="全部类型" value="" />
            <el-option v-for="type in typeOptions" :key="type" :label="type" :value="type" />
          </el-select>
          <el-select v-model="statusFilter" class="agent-filter" placeholder="全部状态" clearable @change="resetPage">
            <el-option label="全部状态" value="" />
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="2" />
            <el-option label="已停用" :value="0" />
          </el-select>
          <div class="view-switch">
            <button type="button" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">
              <el-icon><Grid /></el-icon>
            </button>
            <button type="button" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
              <el-icon><Menu /></el-icon>
            </button>
          </div>
        </div>
      </div>

      <div v-loading="loading" class="agent-card-grid" :class="{ 'is-list': viewMode === 'list' }">
        <article v-for="row in pagedRows" :key="row.id || row.agentId || row.agentKey" class="agent-card">
          <header>
            <div>
              <h4>{{ row.agentName || row.agentKey || '未命名智能体' }}</h4>
            </div>
            <span class="status-badge" :class="statusMeta(getRowStatus(row)).className">
              {{ statusMeta(getRowStatus(row)).text }}
            </span>
          </header>
          <p>{{ getRowDescription(row) }}</p>
          <div class="agent-card-meta">
            <span>模型：{{ getRowModel(row) }}</span>
            <span>
              <el-icon><Connection /></el-icon>
              {{ getSubagentCount(row) }} 个子智能体
            </span>
          </div>
          <footer>
            <div class="agent-card-run">
              <span>今日运行 {{ row.todayRuns ?? row.runCount ?? 0 }}</span>
              <i />
              <span>{{ statusMeta(getRowStatus(row)).detail }}</span>
            </div>
            <nav>
              <el-button link type="primary" @click="handleChat(row)">对话</el-button>
              <b>/</b>
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <b>/</b>
              <el-dropdown trigger="click">
                <el-button link type="primary">
                  更多 <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleDelete(row)">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </nav>
          </footer>
        </article>

        <el-empty v-if="!pagedRows.length" class="agent-empty" :description="emptyText" :image-size="96" />
      </div>

      <div class="agent-list-footer">
        <span>共 {{ filteredRows.length }} 个</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="prev, pager, next, sizes"
          :total="filteredRows.length"
          :page-sizes="[6, 12, 24]"
        />
      </div>
    </article>

    <el-dialog
      v-model="dialogVisible"
      class="agent-dialog agent-dialog-large"
      width="1180px"
      destroy-on-close
      :show-close="false"
    >
      <template #header>
        <div class="dialog-title">
          <div>
            <span>NEW AGENT</span>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button type="button" class="dialog-close" @click="dialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>

      <div class="wizard-steps wizard-steps-six">
        <div
          v-for="(step, index) in wizardSteps"
          :key="step.title"
          :class="{ active: wizardStep >= index + 1, current: wizardStep === index + 1 }"
        >
          <span>
            <el-icon v-if="wizardStep > index + 1"><Check /></el-icon>
            <template v-else>{{ index + 1 }}</template>
          </span>
          <strong>{{ step.title }}</strong>
          <small>{{ step.desc }}</small>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div v-loading="wizardLoading" class="agent-wizard-body" :class="{ 'with-summary': wizardStep === 6 }">
          <div class="agent-wizard-main">
            <section v-if="wizardStep === 1" class="wizard-section agent-type-step">
              <h4>选择智能体类型</h4>
              <p>不同类型决定 Agent 的推理与工具调用方式，创建后仍可调整。</p>
              <button
                v-for="type in agentTypes"
                :key="type.value"
                type="button"
                class="agent-type-card"
                :class="{ selected: form.agentType === type.value }"
                @click="form.agentType = type.value"
              >
                <span class="type-icon">
                  <el-icon><component :is="type.icon" /></el-icon>
                </span>
                <span>
                  <strong>{{ type.title }}</strong>
                  <small>{{ type.desc }}</small>
                  <em v-if="type.tag">{{ type.tag }}</em>
                </span>
                <i class="type-check">
                  <el-icon><Check /></el-icon>
                </i>
              </button>
            </section>

            <section v-else-if="wizardStep === 2" class="wizard-section">
              <h4>基本信息</h4>
              <p>配置智能体身份、用途和默认运行状态。</p>
              <div class="agent-form-grid">
                <el-form-item label="智能体名称" prop="agentName">
                  <el-input v-model="form.agentName" placeholder="如 数据分析助手" />
                </el-form-item>
                <el-form-item label="智能体英文名称" prop="agentKey">
                  <el-input v-model="form.agentKey" placeholder="如 data-analyst" />
                </el-form-item>
                <el-form-item class="full" label="描述">
                  <el-input
                    v-model="form.description"
                    type="textarea"
                    :rows="5"
                    placeholder="说明智能体面向的业务场景、边界和目标用户"
                  />
                </el-form-item>
              </div>
            </section>

            <section v-else-if="wizardStep === 3" class="wizard-section">
              <h4>模型与提示词</h4>
              <p>选择主模型并定义智能体的角色、目标、边界与回复风格。</p>
              <div class="agent-form-grid">
                <el-form-item label="默认模型">
                  <el-select
                    v-model="configForm.modelId"
                    filterable
                    placeholder="请选择模型"
                    @change="(value) => configForm.modelName = modelOptions.find((item) => item.value === value)?.name || ''"
                  >
                    <el-option
                      v-for="model in modelOptions"
                      :key="model.value"
                      :label="model.label"
                      :value="model.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="提示词模板">
                  <el-select
                    v-model="configForm.sysPromptId"
                    filterable
                    placeholder="请选择提示词"
                    @change="(value) => {
                      const prompt = promptOptions.find((item) => item.value === value)
                      configForm.promptTemplate = prompt?.label || ''
                      configForm.sysPrompt = prompt?.sysPrompt || configForm.sysPrompt
                    }"
                  >
                    <el-option
                      v-for="item in promptOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item class="full" label="系统提示词">
                  <el-input
                    v-model="configForm.sysPrompt"
                    type="textarea"
                    :rows="6"
                    placeholder="建议明确角色、目标、边界和工具使用规则"
                  />
                  <small class="field-tip">建议明确角色、目标、边界和工具使用规则</small>
                </el-form-item>
              </div>
            </section>

            <section v-else-if="wizardStep === 4" class="wizard-section">
              <h4>工具与能力</h4>
              <p>按需装配执行能力，权限仍会在运行时按租户和角色校验。</p>
              <div class="capability-grid">
                <article
                  v-for="card in capabilityCards"
                  :key="card.title"
                  class="capability-card resource-card"
                  :class="{ expanded: expandedResource === card.key }"
                >
                  <button class="resource-card-head" type="button" @click="expandedResource = expandedResource === card.key ? '' : card.key">
                    <span class="type-icon">
                      <el-icon><component :is="card.icon" /></el-icon>
                    </span>
                    <span>
                      <h5>{{ card.title }}</h5>
                      <p>{{ card.desc }}</p>
                    </span>
                    <em>{{ card.count }} <el-icon><ArrowRight /></el-icon></em>
                  </button>
                  <div v-if="card.key === 'tools' && expandedResource === 'tools'" class="resource-list">
                    <label v-for="tool in toolRows" :key="tool.id" class="resource-option">
                      <el-checkbox
                        :model-value="isSelected(selections.toolIds, tool.id)"
                        @change="() => toggleId(selections.toolIds, tool.id)"
                      />
                      <span>
                        <strong>{{ tool.toolNameExplain || tool.toolName || tool.toolKey }}</strong>
                        <small>{{ tool.description || tool.permissionCode || tool.toolType }}</small>
                      </span>
                    </label>
                    <el-empty v-if="!toolRows.length" description="暂无工具" :image-size="72" />
                  </div>
                </article>
              </div>
            </section>

            <section v-else-if="wizardStep === 5" class="wizard-section">
              <h4>知识库与 MCP</h4>
              <p>为智能体连接企业知识、外部 MCP 服务和协作子智能体。</p>
              <div class="knowledge-list">
                <article
                  v-for="card in knowledgeCards"
                  :key="card.title"
                  class="knowledge-card resource-card"
                  :class="{ expanded: expandedResource === card.key }"
                >
                  <button class="resource-card-head" type="button" @click="expandedResource = expandedResource === card.key ? '' : card.key">
                    <span class="type-icon">
                      <el-icon><component :is="card.icon" /></el-icon>
                    </span>
                    <span>
                      <h5>{{ card.title }}</h5>
                      <p>{{ card.desc }}</p>
                    </span>
                    <em>{{ card.action }} <el-icon><ArrowRight /></el-icon></em>
                  </button>
                  <div v-if="card.key === 'knowledge' && expandedResource === 'knowledge'" class="resource-list">
                    <label v-for="item in knowledgeRows" :key="item.id" class="resource-option">
                      <el-checkbox
                        :model-value="isSelected(selections.knowledgeBaseIds, item.id)"
                        @change="() => toggleId(selections.knowledgeBaseIds, item.id)"
                      />
                      <span>
                        <strong>{{ item.knowledgeName || item.name || `知识库 #${item.id}` }}</strong>
                        <small>{{ item.description || item.collectionName || item.chunkStrategy }}</small>
                      </span>
                    </label>
                    <el-empty v-if="!knowledgeRows.length" description="暂无知识库" :image-size="72" />
                  </div>
                  <div v-if="card.key === 'subagents' && expandedResource === 'subagents'" class="resource-list">
                    <label v-for="item in subagentRows" :key="item.id" class="resource-option">
                      <el-checkbox
                        :model-value="isSelected(selections.subagentIds, item.id)"
                        @change="() => toggleId(selections.subagentIds, item.id)"
                      />
                      <span>
                        <strong>{{ item.subagentName || item.subagentKey || `子智能体 #${item.id}` }}</strong>
                        <small>{{ item.description || item.workspacePath || item.modelId }}</small>
                      </span>
                    </label>
                    <el-empty v-if="!subagentRows.length" description="暂无子智能体" :image-size="72" />
                  </div>
                </article>
              </div>
            </section>

            <section v-else class="wizard-section advanced-section">
              <h4>高级设置</h4>
              <p>配置版本运行参数、上下文治理、长期记忆、Plan Mode 与沙箱执行环境。</p>

              <article class="advanced-card">
                <header>
                  <span class="type-icon"><el-icon><SetUp /></el-icon></span>
                  <div>
                    <h5>版本与运行</h5>
                    <p>基础运行边界和默认权限</p>
                  </div>
                </header>
                <div class="agent-form-grid">
                  <el-form-item label="版本号">
                    <el-input v-model="configForm.versionNo" />
                  </el-form-item>
                  <el-form-item label="最大循环次数">
                    <el-input-number v-model="configForm.maxIters" :min="1" :max="100" :controls="false" />
                  </el-form-item>
                  <el-form-item label="工作区目录">
                    <el-input v-model="configForm.workspacePath" />
                  </el-form-item>
                  <el-form-item label="默认权限模式">
                    <el-select v-model="configForm.permissionMode">
                      <el-option v-for="item in permissionOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="全局权限策略">
                    <el-input v-model="configForm.agentPermissionPolicyId" placeholder="可留空" />
                  </el-form-item>
                  <el-form-item label="发布状态">
                    <el-select v-model="configForm.publishStatus">
                      <el-option v-for="item in publishOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                </div>
              </article>

              <article class="advanced-card">
                <header>
                  <span class="type-icon"><el-icon><Notebook /></el-icon></span>
                  <div>
                    <h5>上下文压缩</h5>
                    <p>控制长会话的压缩触发和保留范围</p>
                  </div>
                  <el-switch v-model="configForm.contextEnabled" :active-value="1" :inactive-value="0" />
                </header>
                <div class="agent-form-grid four">
                  <el-form-item label="触发消息数">
                    <el-input-number v-model="configForm.triggerMessages" :min="1" :controls="false" />
                  </el-form-item>
                  <el-form-item label="保留消息数">
                    <el-input-number v-model="configForm.keepMessages" :min="0" :controls="false" />
                  </el-form-item>
                  <el-form-item label="触发 Token">
                    <el-input-number v-model="configForm.triggerTokens" :min="0" :controls="false" />
                  </el-form-item>
                  <el-form-item label="保留 Token">
                    <el-input-number v-model="configForm.keepTokens" :min="0" :controls="false" />
                  </el-form-item>
                  <label class="setting-toggle">
                    <span>压缩前刷新上下文</span>
                    <el-switch v-model="configForm.flushBeforeCompact" :active-value="1" :inactive-value="0" />
                  </label>
                  <label class="setting-toggle">
                    <span>压缩前卸载大结果</span>
                    <el-switch v-model="configForm.offloadBeforeCompact" :active-value="1" :inactive-value="0" />
                  </label>
                  <el-form-item class="full" label="摘要模型">
                    <el-select v-model="configForm.compactionModelConfigId" placeholder="为空时使用 Agent 主模型" clearable>
                      <el-option label="使用主模型" :value="null" />
                      <el-option label="轻量摘要模型 #2" :value="2" />
                    </el-select>
                  </el-form-item>
                </div>
              </article>

              <article class="advanced-card">
                <header>
                  <span class="type-icon"><el-icon><Files /></el-icon></span>
                  <div>
                    <h5>工具结果治理</h5>
                    <p>降低大模型上下文占用</p>
                  </div>
                </header>
                <div class="agent-form-grid">
                  <label class="setting-toggle">
                    <span>工具参数预截断</span>
                    <el-switch v-model="configForm.truncateArgsEnabled" :active-value="1" :inactive-value="0" />
                  </label>
                  <el-form-item label="参数最大字符数">
                    <el-input-number
                      v-model="configForm.truncateArgsMaxChars"
                      :disabled="!configForm.truncateArgsEnabled"
                      :min="0"
                      :controls="false"
                      placeholder="例如 10000"
                    />
                  </el-form-item>
                  <label class="setting-toggle">
                    <span>大工具结果卸载</span>
                    <el-switch v-model="configForm.toolResultEvictionEnabled" :active-value="1" :inactive-value="0" />
                  </label>
                  <el-form-item label="结果最大字符数">
                    <el-input-number v-model="configForm.toolResultMaxChars" :min="0" :controls="false" />
                  </el-form-item>
                </div>
              </article>

              <article class="advanced-card">
                <header>
                  <span class="type-icon"><el-icon><FolderOpened /></el-icon></span>
                  <div>
                    <h5>记忆与 Plan Mode</h5>
                    <p>管理长期记忆和结构化任务计划</p>
                  </div>
                </header>
                <div class="advanced-toggle-row">
                  <label><span>长期记忆</span><el-switch v-model="configForm.memoryEnable" :active-value="1" :inactive-value="0" /></label>
                  <label><span>Plan Mode</span><el-switch v-model="configForm.planModeEnabled" :active-value="1" :inactive-value="0" /></label>
                  <label><span>任务列表</span><el-switch v-model="configForm.taskListEnabled" :active-value="1" :inactive-value="0" /></label>
                  <label><span>自主进入 Plan</span><el-switch v-model="configForm.planAutoEnterEnabled" :active-value="1" :inactive-value="0" /></label>
                </div>
                <div class="agent-form-grid">
                  <el-form-item label="计划文件目录">
                    <el-input v-model="configForm.planFileDirectory" />
                  </el-form-item>
                  <el-form-item label="计划最大步骤">
                    <el-input-number v-model="configForm.planMaxSteps" :min="1" :max="200" :controls="false" />
                  </el-form-item>
                  <label class="setting-toggle">
                    <span>Plan 阶段允许 Shell</span>
                    <el-switch v-model="configForm.allowShellInPlanMode" :active-value="1" :inactive-value="0" />
                  </label>
                  <label class="setting-toggle">
                    <span>退出 Plan 需人工确认</span>
                    <el-switch v-model="configForm.planExitApprovalRequired" :active-value="1" :inactive-value="0" />
                  </label>
                  <el-form-item class="full" label="Plan Mode 额外提示词">
                    <el-input
                      v-model="configForm.planPrompt"
                      type="textarea"
                      :rows="4"
                      placeholder="约束计划格式、风险说明和验收标准"
                    />
                  </el-form-item>
                </div>
              </article>

              <article class="advanced-card">
                <header>
                  <span class="type-icon"><el-icon><Monitor /></el-icon></span>
                  <div>
                    <h5>沙箱与可视化</h5>
                    <p>执行隔离和 Studio 画布快照</p>
                  </div>
                  <el-switch v-model="configForm.sandboxEnabled" :active-value="1" :inactive-value="0" />
                </header>
                <div class="agent-form-grid">
                  <el-form-item label="沙箱配置ID">
                    <el-input v-model="configForm.sandboxConfigId" placeholder="可留空" />
                  </el-form-item>
                  <el-form-item class="full" label="可视化画布 JSON">
                    <el-input
                      v-model="configForm.visualSchemaJson"
                      type="textarea"
                      :rows="4"
                      placeholder="为空时将根据当前选择自动生成基础节点和边"
                    />
                  </el-form-item>
                </div>
              </article>
            </section>
          </div>

          <aside v-if="wizardStep === 6" class="wizard-summary">
            <strong>{{ configProgress }}%</strong>
            <span>配置完成度</span>
            <div class="progress-track">
              <i :style="{ width: `${configProgress}%` }" />
            </div>
            <h5>配置检查</h5>
            <p class="ok"><el-icon><Check /></el-icon> 模型和提示词已配置</p>
            <p class="ok"><el-icon><Check /></el-icon> 工具权限策略已选择</p>
            <p class="ok"><el-icon><Check /></el-icon> 上下文压缩参数合理</p>
            <p class="warn">! 沙箱尚未启用</p>
            <div class="summary-note">
              <b>生产环境建议</b>
              <span>保持 Shell 禁用，并要求人工确认退出 Plan Mode。</span>
            </div>
          </aside>
        </div>
      </el-form>

      <template #footer>
        <el-button v-if="wizardStep === 1" @click="dialogVisible = false">取消</el-button>
        <el-button v-else @click="prevStep">上一步</el-button>
        <el-button
          v-if="wizardStep < wizardSteps.length"
          type="primary"
          :icon="ArrowRight"
          @click="nextStep"
        >
          下一步
        </el-button>
        <template v-else>
          <el-button :loading="submitting" @click="submitForm">保存草稿</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            创建智能体 <el-icon><Check /></el-icon>
          </el-button>
        </template>
      </template>
    </el-dialog>
  </section>
</template>
