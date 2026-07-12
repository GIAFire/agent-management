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
  Download,
  Files,
  FolderOpened,
  Key,
  Lightning,
  MagicStick,
  Monitor,
  Notebook,
  SetUp,
  Tools
} from '@element-plus/icons-vue'
import { addAgent, deleteAgent, getAgent, listAgent, updateAgent } from '@/axios/agent'
import { addAgentConfig } from '@/axios/agentConfig'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('创建智能体')
const wizardStep = ref(1)
const formRef = ref()
const agentRows = ref([])
const activeFilter = ref('全部')

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
  permissionMode: 'ASK',
  visualSchemaJson: '',
  agentPermissionPolicyId: null,
  publishStatus: 0,
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
  tools: ['订单查询', '文件检索', '数据库访问'],
  skills: ['数据分析', '报告生成'],
  knowledge: ['产品手册', '合同法规库'],
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

const capabilityCards = [
  { title: '工具', desc: '查询订单、文件读写、数据库访问', count: '8 个已选择', icon: Tools },
  { title: '技能包', desc: '组合提示词、脚本和资源', count: '3 个已选择', icon: Lightning },
]

const knowledgeCards = [
  { title: '知识库', desc: '产品手册、合同法规库', action: '已选择 2 个', icon: Collection },
  { title: 'MCP 服务器', desc: '数据库查询、GitHub、内部工单', action: '配置连接', icon: Connection },
  { title: '子智能体', desc: '将任务委派给编程、检索或审查 Agent', action: '选择子 Agent', icon: Briefcase }
]

const modelOptions = [
  { label: 'Qwen3-235B', value: 1, name: 'Qwen3-235B' },
  { label: 'DeepSeek-V3', value: 2, name: 'DeepSeek-V3' },
  { label: 'Qwen3-32B', value: 3, name: 'Qwen3-32B' }
]

const promptTemplates = ['企业通用助手', '合同审查助手', '数据分析助手', '客服应答助手']
const permissionOptions = ['ASK', 'ALLOW', 'DENY', 'EXPLORE']
const publishOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已废弃', value: 2 }
]

const sourceRows = computed(() => agentRows.value.length ? agentRows.value : sampleRows)

const filteredRows = computed(() => {
  if (activeFilter.value === '运行中') {
    return sourceRows.value.filter((row) => Number(row.status) === 1)
  }
  if (activeFilter.value === '需关注') {
    return sourceRows.value.filter((row) => Number(row.status) !== 1)
  }
  return sourceRows.value
})

const stats = computed(() => {
  const total = sourceRows.value.length
  const running = sourceRows.value.filter((row) => Number(row.status) === 1).length
  return [
    { label: '智能体总数', value: total || 32, note: `${running || 24} 个正在运行` },
    { label: '已发布版本', value: 86, note: '本周新增 7 个' },
    { label: '今日对话', value: '4,692', note: '较昨日 +18.2%' }
  ]
})

const configProgress = computed(() => {
  let score = 20
  if (form.agentName && form.agentKey) score += 15
  if (configForm.modelId && configForm.sysPrompt) score += 15
  if (selections.tools.length || selections.skills.length) score += 15
  if (selections.knowledge.length || selections.mcp.length) score += 10
  if (configForm.contextEnabled) score += 10
  if (configForm.memoryEnable) score += 7
  if (configForm.sandboxEnabled) score += 8
  return Math.min(score, 100)
})

const statusMeta = (status) => {
  const value = Number(status)
  if (value === 1) {
    return { text: '运行中', className: 'running' }
  }
  if (value === 2) {
    return { text: '需关注', className: 'warning' }
  }
  return { text: '已停用', className: 'muted' }
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
    permissionMode: 'ASK',
    visualSchemaJson: '',
    agentPermissionPolicyId: null,
    publishStatus: 0,
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
}

const normalizeId = (value) => {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

const normalizeNumber = (value) => {
  return value === '' || value === undefined || value === null ? null : Number(value)
}

const toSwitchValue = (value) => Number(value) === 1 ? 1 : 0

const loadAgentList = async () => {
  loading.value = true
  try {
    const data = await listAgent()
    agentRows.value = Array.isArray(data) ? data : []
  } catch {
    agentRows.value = []
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  dialogTitle.value = '创建智能体'
  wizardStep.value = 1
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
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
      { id: 'tools', type: 'tools', label: `${selections.tools.length} tools` },
      { id: 'knowledge', type: 'knowledge', label: `${selections.knowledge.length} knowledge bases` }
    ],
    edges: [
      { source: 'model', target: 'tools' },
      { source: 'model', target: 'knowledge' }
    ]
  })
}

const buildConfigPayload = (agentId) => ({
  agentId: normalizeId(agentId),
  versionNo: configForm.versionNo,
  sysPromptId: normalizeId(configForm.sysPromptId),
  modelId: normalizeId(configForm.modelId),
  maxIters: normalizeNumber(configForm.maxIters),
  workspacePath: configForm.workspacePath,
  workspaceConfigJson: JSON.stringify({ path: configForm.workspacePath }),
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
  compactionConfigJson: JSON.stringify({
    enabled: toSwitchValue(configForm.contextEnabled),
    triggerMessages: normalizeNumber(configForm.triggerMessages),
    keepMessages: normalizeNumber(configForm.keepMessages),
    triggerTokens: normalizeNumber(configForm.triggerTokens),
    keepTokens: normalizeNumber(configForm.keepTokens),
    flushBeforeCompact: toSwitchValue(configForm.flushBeforeCompact),
    offloadBeforeCompact: toSwitchValue(configForm.offloadBeforeCompact),
    compactionModelConfigId: normalizeId(configForm.compactionModelConfigId)
  }),
  truncateArgsEnabled: toSwitchValue(configForm.truncateArgsEnabled),
  truncateArgsMaxChars: normalizeNumber(configForm.truncateArgsMaxChars),
  toolResultEvictionEnabled: toSwitchValue(configForm.toolResultEvictionEnabled),
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
  sandboxConfigId: normalizeId(configForm.sandboxConfigId)
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
  wizardStep.value = Math.min(wizardStep.value + 1, wizardSteps.length)
}

const prevStep = () => {
  wizardStep.value = Math.max(wizardStep.value - 1, 1)
}

const findCreatedAgent = async () => {
  const data = await listAgent()
  agentRows.value = Array.isArray(data) ? data : []
  return agentRows.value.find((row) => {
    return row.agentCode === form.agentKey || row.agentKey === form.agentKey || row.agentName === form.agentName
  })
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
      const createResult = await addAgent(buildAgentPayload())
      const createdAgent = createResult?.id ? createResult : await findCreatedAgent()
      if (!createdAgent?.id) {
        ElMessage.warning('智能体已创建，但未能获取 agentId，版本配置暂未保存')
      } else {
        await addAgentConfig(buildConfigPayload(createdAgent.id))
        ElMessage.success('智能体与版本配置创建成功')
      }
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
  if (String(row.id).startsWith('demo-')) {
    ElMessage.info('示例智能体仅用于展示，请先创建真实智能体')
    return
  }

  router.push({
    name: 'AgentChat',
    params: { agentId: row.id },
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
        <span class="eyebrow">AGENT WORKSPACE</span>
        <h2>智能体中心</h2>
        <p>集中创建、配置和发布企业智能体，统一管理版本与运行状态。</p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Download">导出数据</el-button>
        <el-button type="primary" :icon="Lightning" @click="handleAdd">新建</el-button>
      </div>
    </div>

    <div class="agent-stats">
      <article v-for="item in stats" :key="item.label" class="agent-stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small><i />{{ item.note }}</small>
      </article>
    </div>

    <article class="agent-list-panel">
      <div class="agent-list-header">
        <div>
          <h3>智能体列表</h3>
          <p>展示平台当前配置与实时运行状态</p>
        </div>
        <el-segmented v-model="activeFilter" :options="['全部', '运行中', '需关注']" />
      </div>

      <el-table v-loading="loading" :data="filteredRows" class="agent-table">
        <el-table-column prop="agentName" label="智能体" min-width="180" />
        <el-table-column prop="agentType" label="类型" min-width="140" />
        <el-table-column label="当前版本" min-width="140">
          <template #default="{ row }">
            {{ row.currentVersionId || 'v1.0' }}
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="170">
          <template #default="{ row }">
            {{ row.modelName || row.model || 'Qwen3-235B' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <span class="status-badge" :class="statusMeta(row.status).className">
              <i />{{ statusMeta(row.status).text }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleChat(row)">对话</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <div class="agent-bottom-grid">
      <article class="insight-card">
        <span class="insight-icon">
          <el-icon><MagicStick /></el-icon>
        </span>
        <div>
          <h3>智能建议</h3>
          <p>根据最近 7 天运行数据，建议优先检查报告生成助手的模型超时配置。</p>
        </div>
        <el-button link type="primary">查看建议</el-button>
      </article>
      <article class="insight-card">
        <span class="insight-icon cyan">
          <el-icon><Key /></el-icon>
        </span>
        <div>
          <h3>安全与审计</h3>
          <p>所有敏感工具调用均经过租户、角色与运行时三层权限校验。</p>
        </div>
        <el-button link type="primary">打开审计</el-button>
      </article>
    </div>

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
        <div class="agent-wizard-body" :class="{ 'with-summary': wizardStep === 6 }">
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
                  <el-select v-model="configForm.modelId" @change="(value) => configForm.modelName = modelOptions.find((item) => item.value === value)?.name || ''">
                    <el-option
                      v-for="model in modelOptions"
                      :key="model.value"
                      :label="model.label"
                      :value="model.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="提示词模板">
                  <el-select v-model="configForm.promptTemplate">
                    <el-option
                      v-for="item in promptTemplates"
                      :key="item"
                      :label="item"
                      :value="item"
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
                <article v-for="card in capabilityCards" :key="card.title" class="capability-card">
                  <span class="type-icon">
                    <el-icon><component :is="card.icon" /></el-icon>
                  </span>
                  <div>
                    <h5>{{ card.title }}</h5>
                    <p>{{ card.desc }}</p>
                  </div>
                  <el-button link type="primary">{{ card.count }} <el-icon><ArrowRight /></el-icon></el-button>
                </article>
              </div>
            </section>

            <section v-else-if="wizardStep === 5" class="wizard-section">
              <h4>知识库与 MCP</h4>
              <p>为智能体连接企业知识、外部 MCP 服务和协作子智能体。</p>
              <div class="knowledge-list">
                <article v-for="card in knowledgeCards" :key="card.title" class="knowledge-card">
                  <span class="type-icon">
                    <el-icon><component :is="card.icon" /></el-icon>
                  </span>
                  <div>
                    <h5>{{ card.title }}</h5>
                    <p>{{ card.desc }}</p>
                  </div>
                  <el-button>{{ card.action }}</el-button>
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
                      <el-option v-for="item in permissionOptions" :key="item" :label="item" :value="item" />
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
