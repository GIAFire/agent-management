<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  ArrowUp,
  Briefcase,
  Check,
  Close,
  Collection,
  Connection,
  Coin,
  Delete,
  Files,
  FolderOpened,
  Grid,
  InfoFilled,
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
const modelConfigExpanded = ref(true)
const promptTemplateExpanded = ref(true)
const promptPreviewExpanded = ref(false)
const toolsCapabilityExpanded = ref(true)
const skillsCapabilityExpanded = ref(true)
const subagentsCapabilityExpanded = ref(true)
const knowledgeBaseExpanded = ref(true)
const mcpServiceExpanded = ref(true)
const modelRows = ref([])
const promptRows = ref([])
const toolRows = ref([])
const knowledgeRows = ref([])
const subagentRows = ref([])

const skillRows = ref([
  {
    id: 'sql-analysis',
    skillName: 'SQL 数据分析',
    skillKey: '/sql-analysis',
    description: '查询业务数据并生成分析结论',
    enabled: true
  },
  {
    id: 'report-generator',
    skillName: '数据报告生成',
    skillKey: '/report-generator',
    description: '将分析结果整理为结构化报告',
    enabled: true
  },
  {
    id: 'document-summary',
    skillName: '文档总结',
    skillKey: '/document-summary',
    description: '提取文档重点并形成摘要',
    enabled: true
  },
  {
    id: 'git-analysis',
    skillName: 'Git 仓库分析',
    skillKey: '/git-analysis',
    description: '分析代码结构、提交记录和变更',
    enabled: true
  }
])

const mcpRows = ref([
  {
    id: 'business-database-mcp',
    name: '业务数据库 MCP',
    protocol: 'SSE',
    endpoint: 'mcp-db.internal',
    description: '订单、客户与销售数据查询',
    toolCount: 8,
    connected: true,
    enabled: true
  },
  {
    id: 'github-mcp',
    name: 'GitHub MCP',
    protocol: 'Streamable HTTP',
    endpoint: '',
    description: '仓库、Issue、Pull Request 操作',
    toolCount: 12,
    connected: true,
    enabled: true
  },
  {
    id: 'filesystem-mcp',
    name: '文件系统 MCP',
    protocol: 'STDIO',
    endpoint: '本地服务',
    description: '受控目录的文件读写与检索',
    toolCount: 6,
    connected: true,
    enabled: true
  },
  {
    id: 'browser-mcp',
    name: '浏览器 MCP',
    protocol: 'SSE',
    endpoint: 'browser-service',
    description: '网页访问、内容提取与页面操作',
    toolCount: 9,
    connected: false,
    enabled: false
  }
])

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
  modelName: '',
  promptTemplate: '',
  sysPrompt: '',
  maxIters: 10,
  workspacePath: '',
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
  truncateArgsEnabled: 1,
  truncateArgsMaxChars: 2000,
  toolResultEvictionEnabled: 1,
  toolResultMaxChars: 20000,
  memoryEnable: 1,
  planModeEnabled: 1,
  planFileDirectory: 'plans',
  taskListEnabled: 1,
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
  { title: '基本信息', desc: '类型、名称与描述' },
  { title: '模型与提示词', desc: '选择模型和系统提示词' },
  { title: '工具、技能、子智能体', desc: '配置能力与协作成员' },
  { title: '知识库与 MCP', desc: '绑定知识库与外部服务' },
  { title: '高级配置', desc: '上下文、记忆与工作区' }
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
  }
]

const promptOptions = computed(() => promptRows.value.map((item) => ({
  label: item.promptName || `提示词 #${item.id}`,
  value: item.id,
  sysPrompt: item.sysPrompt || ''
})))

const selectedModel = computed(() => {
  return modelRows.value.find((item) => normalizeId(item.id) === normalizeId(configForm.modelId))
})

const selectedPrompt = computed(() => {
  return promptRows.value.find((item) => normalizeId(item.id) === normalizeId(configForm.sysPromptId))
})

const promptPreviewText = computed(() => {
  return configForm.sysPrompt || '选择系统提示词模板后，将在这里预览完整提示词内容。'
})

const formatProvider = (provider) => {
  if (!provider) {
    return '模型供应商'
  }
  return String(provider)
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (char) => char.toUpperCase())
}

const formatModelContext = (model) => {
  const value = model?.contextWindow || model?.contextTokens || model?.maxContextTokens || model?.maxTokens
  if (!value) {
    return '上下文 默认'
  }
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return `上下文 ${value}`
  }
  if (numberValue >= 1000000) {
    return `上下文 ${Math.round(numberValue / 1000000)}M`
  }
  if (numberValue >= 1000) {
    return `上下文 ${Math.round(numberValue / 1000)}K`
  }
  return `上下文 ${numberValue}`
}

const selectModel = (model) => {
  configForm.modelId = model.id
  configForm.modelName = model.modelName || `模型 #${model.id}`
}

const selectPrompt = (prompt) => {
  configForm.sysPromptId = prompt.id
  configForm.promptTemplate = prompt.promptName || `提示词 #${prompt.id}`
  configForm.sysPrompt = prompt.sysPrompt || ''
}
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

const capabilityTotal = computed(() => {
  return selections.toolIds.length + selections.skillIds.length + selections.subagentIds.length
})

const capabilitySummaryText = computed(() => {
  return `已选择 ${selections.toolIds.length} 个工具 · ${selections.skillIds.length} 个技能 · ${selections.subagentIds.length} 个子智能体`
})

const selectedMcpToolCount = computed(() => {
  return mcpRows.value.reduce((total, item) => {
    if (!isSelected(selections.mcp, item.id)) {
      return total
    }
    return total + Number(item.toolCount || 0)
  }, 0)
})

const knowledgeMcpTotal = computed(() => selections.knowledgeBaseIds.length + selections.mcp.length)

const knowledgeMcpSummaryText = computed(() => {
  return `已选择 ${selections.knowledgeBaseIds.length} 个知识库 · ${selections.mcp.length} 个 MCP 服务`
})

const getToolTitle = (tool) => tool?.toolNameExplain || tool?.toolName || tool?.toolKey || `工具 #${tool?.id}`

const getToolKey = (tool) => tool?.toolName || tool?.toolKey || tool?.permissionCode || tool?.methodName || '-'

const getToolDesc = (tool) => tool?.description || tool?.toolType || tool?.permissionCode || '暂无工具说明'

const isToolEnabled = (tool) => tool?.enabled !== false && Number(tool?.status ?? 1) !== 0

const getSkillTitle = (skill) => skill?.skillName || skill?.name || `技能 #${skill?.id}`

const getSkillKey = (skill) => skill?.skillKey || skill?.route || skill?.version || '-'

const getSkillDesc = (skill) => skill?.description || skill?.scene || '暂无技能说明'

const isSkillEnabled = (skill) => skill?.enabled !== false && skill?.status !== '停用'

const getSubagentTitle = (subagent) => subagent?.subagentName || subagent?.subagentKey || `子智能体 #${subagent?.id}`

const getSubagentType = (subagent) => subagent?.description || subagent?.workspaceMode || subagent?.subagentKey || '专业子智能体'

const getSubagentDesc = (subagent) => subagent?.systemPrompt || subagent?.workspacePath || subagent?.modelConfigId || '可按任务需要派发执行'

const isSubagentEnabled = (subagent) => Number(subagent?.status ?? 1) !== 0

const clearCapabilitySelections = () => {
  selections.toolIds.splice(0)
  selections.skillIds.splice(0)
  selections.subagentIds.splice(0)
}

const getKnowledgeTitle = (item) => item?.knowledgeName || item?.name || item?.collectionName || `知识库 #${item?.id}`

const getKnowledgeDesc = (item) => item?.description || item?.chunkStrategy || item?.embeddingModel || '可用于检索增强与来源引用'

const getKnowledgeMeta = (item) => {
  const documentCount = Number(item?.documentCount ?? item?.docCount ?? item?.documents ?? 0)
  const chunkCount = Number(item?.chunkCount ?? item?.sliceCount ?? item?.chunks ?? 0)
  if (documentCount || chunkCount) {
    return `${documentCount} 个文档 · ${chunkCount} 个切片`
  }
  return item?.collectionName || item?.knowledgeKey || '已建立向量索引'
}

const getKnowledgeStatus = (item) => {
  const raw = String(item?.statusText || item?.syncStatus || item?.state || item?.status || '').toLowerCase()
  if (raw.includes('updat') || raw.includes('sync') || raw.includes('更新') || raw === '2') {
    return { text: '更新中', tone: 'warning' }
  }
  if (raw === '0' || raw.includes('disable') || raw.includes('停')) {
    return { text: '未启用', tone: 'muted' }
  }
  return { text: '已就绪', tone: 'ready' }
}

const isKnowledgeEnabled = (item) => getKnowledgeStatus(item).tone !== 'muted'

const getMcpEndpoint = (item) => [item.protocol, item.endpoint].filter(Boolean).join(' · ')

const isMcpEnabled = (item) => item?.enabled !== false && item?.connected !== false

const clearKnowledgeMcpSelections = () => {
  selections.knowledgeBaseIds.splice(0)
  selections.mcp.splice(0)
}

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
    modelId: null,
    modelName: '',
    promptTemplate: '',
    sysPrompt: '',
    maxIters: 10,
    workspacePath: '',
    permissionMode: 'ACCEPT_EDITS',
    visualSchemaJson: '',
    agentPermissionPolicyId: null,
    publishStatus: 1,
    contextEnabled: 1,
    triggerMessages: 30,
    keepMessages: 10,
    triggerTokens: 6000,
    keepTokens: 800,
    flushBeforeCompact: 1,
    offloadBeforeCompact: 1,
    compactionModelConfigId: null,
    truncateArgsEnabled: 1,
    truncateArgsMaxChars: 2000,
    toolResultEvictionEnabled: 1,
    toolResultMaxChars: 20000,
    memoryEnable: 1,
    planModeEnabled: 1,
    planFileDirectory: 'plans',
    taskListEnabled: 1,
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
  modelConfigExpanded.value = true
  promptTemplateExpanded.value = true
  promptPreviewExpanded.value = false
  toolsCapabilityExpanded.value = true
  skillsCapabilityExpanded.value = true
  subagentsCapabilityExpanded.value = true
  knowledgeBaseExpanded.value = true
  mcpServiceExpanded.value = true
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

const loadSubagents = async () => {
  if (subagentRows.value.length) {
    return
  }
  wizardLoading.value = true
  try {
    const subagents = await listWizardSubagents()
    subagentRows.value = Array.isArray(subagents) ? subagents : []
  } finally {
    wizardLoading.value = false
  }
}

const loadKnowledgeBases = async () => {
  if (knowledgeRows.value.length) {
    return
  }
  wizardLoading.value = true
  try {
    const knowledgeBases = await listWizardKnowledgeBases()
    knowledgeRows.value = Array.isArray(knowledgeBases) ? knowledgeBases : []
  } finally {
    wizardLoading.value = false
  }
}

const ensureStepData = async (step) => {
  if (step === 2) {
    await loadModelAndPrompts()
  }
  if (step === 3) {
    await loadTools()
    await loadSubagents()
  }
  if (step === 4) {
    await loadKnowledgeBases()
  }
}

const handleAdd = async () => {
  dialogTitle.value = '创建智能体'
  wizardStep.value = 1
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
  await ensureStepData(2)
}

const handleEdit = async (row) => {
  if (String(row.id).startsWith('demo-')) {
    ElMessage.info('示例智能体仅用于展示，请先创建真实智能体')
    return
  }

  dialogTitle.value = '编辑智能体'
  wizardStep.value = 1
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
  if (wizardStep.value === 2 && !configForm.modelId) {
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

      <div class="wizard-steps wizard-steps-five">
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
        <div v-loading="wizardLoading" class="agent-wizard-body" :class="{ 'with-summary': wizardStep === 5 }">
          <div class="agent-wizard-main">
            <section v-if="wizardStep === 1" class="wizard-section basic-info-step">
              <h4>基本信息</h4>
              <p>选择智能体运行类型，并填写用于识别智能体的基础信息。</p>

              <h5>选择智能体类型</h5>
              <div class="agent-type-options">
                <button
                  v-for="type in agentTypes"
                  :key="type.value"
                  type="button"
                  class="agent-type-card"
                  :class="{ selected: form.agentType === type.value }"
                  @click="form.agentType = type.value"
                >
                  <span>
                    <strong>{{ type.title }}</strong>
                    <em v-if="type.tag">{{ type.tag }}</em>
                    <small>{{ type.desc }}</small>
                  </span>
                  <i class="type-check">
                    <el-icon><Check /></el-icon>
                  </i>
                </button>
              </div>

              <article class="agent-basic-panel">
                <h5>填写基本信息</h5>
                <div class="agent-form-grid">
                  <el-form-item label="智能体英文名称" prop="agentKey">
                    <el-input v-model="form.agentKey" placeholder="例如：data-analyst" />
                    <small class="field-tip">用于 API、日志和运行实例标识，建议使用小写字母与连字符</small>
                  </el-form-item>
                  <el-form-item label="智能体中文名称" prop="agentName">
                    <el-input v-model="form.agentName" placeholder="例如：数据分析师" />
                    <small class="field-tip">用于页面展示和用户识别</small>
                  </el-form-item>
                  <el-form-item class="full" label="智能体描述">
                    <el-input
                      v-model="form.description"
                      type="textarea"
                      :rows="3"
                      maxlength="200"
                      show-word-limit
                      placeholder="简要描述智能体的职责、核心能力和适用场景..."
                    />
                  </el-form-item>
                </div>
              </article>

              <div class="basic-info-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>所有字段均可暂时留空，点击下一步继续配置模型与提示词。</span>
              </div>
            </section>

            <section v-else-if="wizardStep === 2" class="wizard-section">
              <article class="model-prompt-panel">
                <button type="button" class="config-section-head" @click="modelConfigExpanded = !modelConfigExpanded">
                  <span>
                    <h4>模型配置</h4>
                    <small>{{ selectedModel?.modelName || '选择默认模型' }}</small>
                  </span>
                  <el-icon><component :is="modelConfigExpanded ? ArrowUp : ArrowDown" /></el-icon>
                </button>

                <div v-show="modelConfigExpanded" class="model-card-grid">
                  <button
                    v-for="model in modelRows"
                    :key="model.id"
                    type="button"
                    class="model-option-card"
                    :class="{ selected: normalizeId(configForm.modelId) === normalizeId(model.id) }"
                    @click="selectModel(model)"
                  >
                    <i class="option-radio">
                      <el-icon><Check /></el-icon>
                    </i>
                    <span>
                      <strong>{{ model.modelName || `模型 #${model.id}` }}</strong>
                      <small>{{ model.description || model.modelType || 'chat' }}</small>
                      <em>{{ formatModelContext(model) }}</em>
                    </span>
                    <b>{{ formatProvider(model.provider) }}</b>
                  </button>
                  <el-empty v-if="!modelRows.length" description="暂无可选模型" :image-size="72" />
                </div>

<!--                <div v-if="selectedModel" class="selected-model-strip">-->
<!--                  <span>已选择：<strong>{{ selectedModel.modelName || configForm.modelName }}</strong></span>-->
<!--                  <em v-if="selectedModel.streaming">支持流式输出</em>-->
<!--                  <em v-if="selectedModel.thinking">支持思考</em>-->
<!--                  <button type="button" @click="modelConfigExpanded = true">-->
<!--                    查看模型详情 <el-icon><ArrowRight /></el-icon>-->
<!--                  </button>-->
<!--                </div>-->
              </article>

              <article class="model-prompt-panel">
                <button type="button" class="config-section-head" @click="promptTemplateExpanded = !promptTemplateExpanded">
                  <span>
                    <h4>系统提示词模板</h4>
                    <small>{{ selectedPrompt?.promptName || '选择模板后自动填充预览内容' }}</small>
                  </span>
                  <el-icon><component :is="promptTemplateExpanded ? ArrowUp : ArrowDown" /></el-icon>
                </button>

                <div v-show="promptTemplateExpanded" class="prompt-template-grid">
                  <button
                    v-for="prompt in promptRows"
                    :key="prompt.id"
                    type="button"
                    class="prompt-option-card"
                    :class="{ selected: normalizeId(configForm.sysPromptId) === normalizeId(prompt.id) }"
                    @click="selectPrompt(prompt)"
                  >
                    <i class="option-radio">
                      <el-icon><Check /></el-icon>
                    </i>
                    <span>
                      <strong>{{ prompt.promptName || `提示词 #${prompt.id}` }}</strong>
                      <small>{{ prompt.description || '选择后可在下方预览提示词内容' }}</small>
                    </span>
                  </button>
                  <el-empty v-if="!promptRows.length" description="暂无提示词模板" :image-size="72" />
                </div>
              </article>

              <article class="prompt-preview-panel" :class="{ expanded: promptPreviewExpanded }">
                <button type="button" class="prompt-preview-head" @click="promptPreviewExpanded = !promptPreviewExpanded">
                  <span>
                    <strong>模板内容预览</strong>
                    <small v-if="!promptPreviewExpanded">{{ promptPreviewText }}</small>
                  </span>
                  <em>{{ promptPreviewExpanded ? '收起预览' : '展开编辑' }} <el-icon><component :is="promptPreviewExpanded ? ArrowUp : ArrowDown" /></el-icon></em>
                </button>
                <el-input
                  disabled
                  v-show="promptPreviewExpanded"
                  v-model="configForm.sysPrompt"
                  class="prompt-preview-editor"
                  type="textarea"
                  :rows="7"
                  placeholder="选择系统提示词模板后，可在这里预览并调整完整提示词内容"
                />
              </article>

              <div class="basic-info-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>模型和提示词模板均可暂时不选择，点击下一步继续配置工具、技能、子智能体。</span>
              </div>
            </section>

            <section v-else-if="wizardStep === 3" class="wizard-section capability-step">
              <header class="capability-step-head">
                <div>
                  <h4>工具、技能、子智能体</h4>
                  <p>为智能体配置可调用的能力，并选择可委派任务的子智能体。</p>
                </div>
                <strong>{{ capabilitySummaryText }}</strong>
              </header>

              <article class="capability-section">
                <button type="button" class="capability-section-title capability-section-toggle" @click="toolsCapabilityExpanded = !toolsCapabilityExpanded">
                  <span class="capability-section-icon"><el-icon><Tools /></el-icon></span>
                  <h5>工具</h5>
                  <p>智能体在运行过程中按需调用，可单独设置使用模式</p>
                  <el-icon class="capability-section-arrow"><component :is="toolsCapabilityExpanded ? ArrowUp : ArrowDown" /></el-icon>
                </button>
                <div v-show="toolsCapabilityExpanded" class="capability-select-grid tools">
                  <button
                    v-for="tool in toolRows"
                    :key="tool.id"
                    type="button"
                    class="capability-select-card"
                    :class="{ selected: isSelected(selections.toolIds, tool.id), disabled: !isToolEnabled(tool) }"
                    :disabled="!isToolEnabled(tool)"
                    @click="toggleId(selections.toolIds, tool.id)"
                  >
                    <el-checkbox
                      :model-value="isSelected(selections.toolIds, tool.id)"
                      :disabled="!isToolEnabled(tool)"
                      @click.stop
                      @change="() => toggleId(selections.toolIds, tool.id)"
                    />
                    <strong>{{ getToolTitle(tool) }}</strong>
                    <small>{{ getToolKey(tool) }}</small>
                    <p>{{ getToolDesc(tool) }}</p>
                  </button>
                  <el-empty v-if="!toolRows.length" description="暂无工具" :image-size="72" />
                </div>
              </article>

              <article class="capability-section">
                <button type="button" class="capability-section-title capability-section-toggle" @click="skillsCapabilityExpanded = !skillsCapabilityExpanded">
                  <span class="capability-section-icon"><el-icon><MagicStick /></el-icon></span>
                  <h5>技能</h5>
                  <p>技能封装可复用的业务流程、知识与操作规范</p>
                  <el-icon class="capability-section-arrow"><component :is="skillsCapabilityExpanded ? ArrowUp : ArrowDown" /></el-icon>
                </button>
                <div v-show="skillsCapabilityExpanded" class="capability-select-grid">
                  <button
                    v-for="skill in skillRows"
                    :key="skill.id"
                    type="button"
                    class="capability-select-card"
                    :class="{ selected: isSelected(selections.skillIds, skill.id), disabled: !isSkillEnabled(skill) }"
                    :disabled="!isSkillEnabled(skill)"
                    @click="toggleId(selections.skillIds, skill.id)"
                  >
                    <el-checkbox
                      :model-value="isSelected(selections.skillIds, skill.id)"
                      :disabled="!isSkillEnabled(skill)"
                      @click.stop
                      @change="() => toggleId(selections.skillIds, skill.id)"
                    />
                    <strong>{{ getSkillTitle(skill) }}</strong>
                    <small>{{ getSkillKey(skill) }}</small>
                    <p>{{ getSkillDesc(skill) }}</p>
                  </button>
                </div>
              </article>

              <article class="capability-section">
                <button type="button" class="capability-section-title capability-section-toggle" @click="subagentsCapabilityExpanded = !subagentsCapabilityExpanded">
                  <span class="capability-section-icon"><el-icon><Connection /></el-icon></span>
                  <h5>子智能体</h5>
                  <p>主智能体可根据任务需要将子任务委派给专业子智能体</p>
                  <el-icon class="capability-section-arrow"><component :is="subagentsCapabilityExpanded ? ArrowUp : ArrowDown" /></el-icon>
                </button>
                <div v-show="subagentsCapabilityExpanded" class="capability-select-grid">
                  <button
                    v-for="subagent in subagentRows"
                    :key="subagent.id"
                    type="button"
                    class="capability-select-card subagent"
                    :class="{ selected: isSelected(selections.subagentIds, subagent.id), disabled: !isSubagentEnabled(subagent) }"
                    :disabled="!isSubagentEnabled(subagent)"
                    @click="toggleId(selections.subagentIds, subagent.id)"
                  >
                    <el-checkbox
                      :model-value="isSelected(selections.subagentIds, subagent.id)"
                      :disabled="!isSubagentEnabled(subagent)"
                      @click.stop
                      @change="() => toggleId(selections.subagentIds, subagent.id)"
                    />
                    <strong>{{ getSubagentTitle(subagent) }}</strong>
                    <small>{{ getSubagentType(subagent) }}</small>
                    <p>{{ getSubagentDesc(subagent) }}</p>
                    <em :class="{ unavailable: !isSubagentEnabled(subagent) }">
                      {{ isSubagentEnabled(subagent) ? '可用' : '未启用' }}
                    </em>
                  </button>
                  <el-empty v-if="!subagentRows.length" description="暂无子智能体" :image-size="72" />
                </div>
              </article>

              <div class="capability-summary-bar">
                <span><el-icon><Tools /></el-icon> 工具 <strong>{{ selections.toolIds.length }}</strong></span>
                <span><el-icon><MagicStick /></el-icon> 技能 <strong>{{ selections.skillIds.length }}</strong></span>
                <span><el-icon><Connection /></el-icon> 子智能体 <strong>{{ selections.subagentIds.length }}</strong></span>
                <button type="button" :disabled="!capabilityTotal" @click="clearCapabilitySelections">
                  <el-icon><Delete /></el-icon> 清空选择
                </button>
              </div>

              <div class="basic-info-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>当前配置可暂时不选择任何能力，点击下一步继续绑定知识库。</span>
              </div>
            </section>

            <section v-else-if="wizardStep === 4" class="wizard-section capability-step knowledge-mcp-step">
              <header class="capability-step-head">
                <div>
                  <h4>知识库与 MCP</h4>
                  <p>为智能体绑定可检索的知识库，并连接可调用的 MCP 服务。</p>
                </div>
                <strong>{{ knowledgeMcpSummaryText }}</strong>
              </header>

              <article class="capability-section">
                <button type="button" class="capability-section-title capability-section-toggle" @click="knowledgeBaseExpanded = !knowledgeBaseExpanded">
                  <span class="capability-section-icon"><el-icon><Collection /></el-icon></span>
                  <h5>知识库</h5>
                  <p>智能体可从已绑定的知识库中检索内容，并在回答中引用来源</p>
                  <el-icon class="capability-section-arrow"><component :is="knowledgeBaseExpanded ? ArrowUp : ArrowDown" /></el-icon>
                </button>
                <div v-show="knowledgeBaseExpanded" class="capability-select-grid knowledge">
                  <button
                    v-for="item in knowledgeRows"
                    :key="item.id"
                    type="button"
                    class="capability-select-card knowledge-source"
                    :class="{ selected: isSelected(selections.knowledgeBaseIds, item.id), disabled: !isKnowledgeEnabled(item) }"
                    :disabled="!isKnowledgeEnabled(item)"
                    @click="toggleId(selections.knowledgeBaseIds, item.id)"
                  >
                    <el-checkbox
                      :model-value="isSelected(selections.knowledgeBaseIds, item.id)"
                      :disabled="!isKnowledgeEnabled(item)"
                      @click.stop
                      @change="() => toggleId(selections.knowledgeBaseIds, item.id)"
                    />
                    <strong>{{ getKnowledgeTitle(item) }}</strong>
                    <p>{{ getKnowledgeDesc(item) }}</p>
                    <small>{{ getKnowledgeMeta(item) }}</small>
                    <em class="resource-status" :class="getKnowledgeStatus(item).tone">
                      {{ getKnowledgeStatus(item).text }}
                    </em>
                  </button>
                  <el-empty v-if="!knowledgeRows.length" description="暂无知识库" :image-size="72" />
                </div>
              </article>

              <article class="capability-section">
                <button type="button" class="capability-section-title capability-section-toggle" @click="mcpServiceExpanded = !mcpServiceExpanded">
                  <span class="capability-section-icon"><el-icon><Connection /></el-icon></span>
                  <h5>MCP 服务</h5>
                  <p>连接遵循 Model Context Protocol 的外部服务，为智能体提供标准化工具能力</p>
                  <el-icon class="capability-section-arrow"><component :is="mcpServiceExpanded ? ArrowUp : ArrowDown" /></el-icon>
                </button>
                <div v-show="mcpServiceExpanded" class="capability-select-grid mcp">
                  <button
                    v-for="item in mcpRows"
                    :key="item.id"
                    type="button"
                    class="capability-select-card mcp-service"
                    :class="{ selected: isSelected(selections.mcp, item.id), disabled: !isMcpEnabled(item) }"
                    :disabled="!isMcpEnabled(item)"
                    @click="toggleId(selections.mcp, item.id)"
                  >
                    <el-checkbox
                      :model-value="isSelected(selections.mcp, item.id)"
                      :disabled="!isMcpEnabled(item)"
                      @click.stop
                      @change="() => toggleId(selections.mcp, item.id)"
                    />
                    <strong>{{ item.name }}</strong>
                    <small>{{ getMcpEndpoint(item) }}</small>
                    <p>{{ item.description }}</p>
                    <span class="mcp-card-footer">
                      <b>{{ item.toolCount }} 个工具</b>
                      <em class="resource-status" :class="item.connected ? 'ready' : 'muted'">
                        {{ item.connected ? '已连接' : '未连接' }}
                      </em>
                    </span>
                  </button>
                </div>
              </article>

              <div class="mcp-warning-tip">
                <el-icon><Lock /></el-icon>
                <span>MCP 服务暴露的工具仍受智能体权限策略控制，敏感操作可在运行时请求确认。</span>
              </div>

              <div class="capability-summary-bar">
                <span><el-icon><Collection /></el-icon> 知识库 <strong>{{ selections.knowledgeBaseIds.length }}</strong></span>
                <span><el-icon><Connection /></el-icon> MCP 服务 <strong>{{ selections.mcp.length }}</strong></span>
                <span><el-icon><Tools /></el-icon> 可用 MCP 工具 <strong>{{ selectedMcpToolCount }}</strong></span>
                <button type="button" :disabled="!knowledgeMcpTotal" @click="clearKnowledgeMcpSelections">
                  <el-icon><Delete /></el-icon> 清空选择
                </button>
              </div>

              <div class="basic-info-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>知识库和 MCP 服务均可暂时不选择，点击下一步继续高级配置。</span>
              </div>
            </section>

            <section v-else class="wizard-section advanced-section">
              <h4>高级配置</h4>
              <p>配置版本运行参数、上下文治理、长期记忆、计划模式与沙箱执行环境。</p>

              <article class="advanced-card">
                <header>
                  <span class="type-icon"><el-icon><SetUp /></el-icon></span>
                  <div>
                    <h5>运行配置</h5>
                    <p>基础运行边界和默认权限</p>
                  </div>
                </header>
                <div class="agent-form-grid">
                  <label class="setting-toggle">
                    <span>长期记忆</span>
                    <el-switch v-model="configForm.memoryEnable" :active-value="1" :inactive-value="0" />
                  </label>
                  <el-form-item label="最大循环次数">
                    <el-input-number v-model="configForm.maxIters" :min="1" :max="100" :controls="false" />
                  </el-form-item>
                  <el-form-item label="工作区目录">
                    <el-input v-model="configForm.workspacePath" placeholder="可为空"/>
                  </el-form-item>
                  <el-form-item label="默认权限模式">
                    <el-select v-model="configForm.permissionMode">
                      <el-option v-for="item in permissionOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>

<!--                  <el-form-item label="发布状态">-->
<!--                    <el-select v-model="configForm.publishStatus">-->
<!--                      <el-option v-for="item in publishOptions" :key="item.value" :label="item.label" :value="item.value" />-->
<!--                    </el-select>-->
<!--                  </el-form-item>-->
                </div>
              </article>

              <article class="advanced-card" :class="{ collapsed: !configForm.contextEnabled }">
                <header>
                  <span class="type-icon"><el-icon><Notebook /></el-icon></span>
                  <div class="advanced-card-copy">
                    <div class="advanced-title-inline">
                      <h5>上下文压缩</h5>
                      <el-switch v-model="configForm.contextEnabled" :active-value="1" :inactive-value="0" />
                    </div>
                    <p>控制长会话的压缩触发和保留范围</p>
                  </div>
                </header>
                <div v-show="configForm.contextEnabled" class="agent-form-grid four">
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
<!--                  <el-form-item class="full" label="摘要模型">-->
<!--                    <el-select v-model="configForm.compactionModelConfigId" placeholder="为空时使用 Agent 主模型" clearable>-->
<!--                      <el-option label="使用主模型" :value="null" />-->
<!--                      <el-option label="轻量摘要模型 #2" :value="2" />-->
<!--                    </el-select>-->
<!--                  </el-form-item>-->
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
                    />
                  </el-form-item>
                  <label class="setting-toggle">
                    <span>大工具结果卸载</span>
                    <el-switch
                        v-model="configForm.toolResultEvictionEnabled"
                        :active-value="1"
                        :inactive-value="0" />
                  </label>
                  <el-form-item label="结果最大字符数">
                    <el-input-number
                        v-model="configForm.toolResultMaxChars"
                        :disabled="!configForm.toolResultEvictionEnabled"
                        :min="0" :controls="false" />
                  </el-form-item>
                </div>
              </article>

              <article class="advanced-card" :class="{ collapsed: !configForm.planModeEnabled }">
                <header>
                  <span class="type-icon"><el-icon><FolderOpened /></el-icon></span>
                  <div class="advanced-card-copy">
                    <div class="advanced-title-inline">
                      <h5>计划模式</h5>
                      <el-switch v-model="configForm.planModeEnabled" :active-value="1" :inactive-value="0" />
                    </div>
                    <p>管理结构化任务计划和执行约束</p>
                  </div>
                </header>
                <div v-show="configForm.planModeEnabled" class="advanced-toggle-row">
                  <label><span>开启任务列表</span><el-switch v-model="configForm.taskListEnabled" :active-value="1" :inactive-value="0" /></label>
                  <label><span>复杂任务自主进入Plan</span><el-switch v-model="configForm.planAutoEnterEnabled" :active-value="1" :inactive-value="0" /></label>
                  <label><span>Plan 阶段允许使用Shell命令</span><el-switch v-model="configForm.allowShellInPlanMode" :active-value="1" :inactive-value="0" /></label>
                  <label><span>计划制定完毕后是否需要人工确认</span><el-switch v-model="configForm.planExitApprovalRequired" :active-value="1" :inactive-value="0" /></label>
                </div>
                <div v-show="configForm.planModeEnabled" class="agent-form-grid">
                  <el-form-item label="计划文件目录">
                    <el-input v-model="configForm.planFileDirectory" />
                  </el-form-item>
                  <el-form-item label="计划最大步骤">
                    <el-input-number v-model="configForm.planMaxSteps" :min="1" :max="200" :controls="false" />
                  </el-form-item>
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

              <article class="advanced-card" :class="{ collapsed: !configForm.sandboxEnabled }">
                <header>
                  <span class="type-icon"><el-icon><Monitor /></el-icon></span>
                  <div class="advanced-card-copy">
                    <div class="advanced-title-inline">
                      <h5>沙箱与可视化</h5>
                      <el-switch
                          v-model="configForm.sandboxEnabled"
                          :disabled="!configForm.sandboxEnabled"
                          :active-value="1" :inactive-value="0" />
                    </div>
                    <p>执行隔离和 Studio 画布快照</p>
                  </div>
                </header>
                <div v-show="configForm.sandboxEnabled" class="agent-form-grid">
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
          下一步：{{ wizardSteps[wizardStep]?.title }}
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
