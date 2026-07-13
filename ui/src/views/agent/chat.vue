<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bottom, ChatLineRound, Close, Delete, MoreFilled, Plus, Promotion, Search } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { chatStream, userConfirmStream } from '@/axios/chat'

const route = useRoute()
const markdownRenderer = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})
const defaultLinkOpenRenderer = markdownRenderer.renderer.rules.link_open || ((tokens, index, options, env, self) => self.renderToken(tokens, index, options))

markdownRenderer.renderer.rules.link_open = (tokens, index, options, env, self) => {
  tokens[index].attrSet('target', '_blank')
  tokens[index].attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpenRenderer(tokens, index, options, env, self)
}

const sessions = ref([])
const activeSessionId = ref('')
const inputMessage = ref('')
const sessionKeyword = ref('')
const planDrawerOpen = ref(false)
const streaming = ref(false)
const autoScrollEnabled = ref(true)
const messageListRef = ref()
const abortController = ref(null)

let saveTimer = null
let scrollFrame = null
let pendingScrollForce = false
let snowflakeLastTimestamp = -1n
let snowflakeSequence = 0n
let snowflakeWorkerId = null
let snowflakeDatacenterId = null

const SCROLL_BOTTOM_THRESHOLD = 64
const SNOWFLAKE_EPOCH = 1288834974657n
const SNOWFLAKE_WORKER_ID_BITS = 5n
const SNOWFLAKE_DATACENTER_ID_BITS = 5n
const SNOWFLAKE_SEQUENCE_BITS = 12n
const SNOWFLAKE_MAX_WORKER_ID = (1n << SNOWFLAKE_WORKER_ID_BITS) - 1n
const SNOWFLAKE_MAX_DATACENTER_ID = (1n << SNOWFLAKE_DATACENTER_ID_BITS) - 1n
const SNOWFLAKE_SEQUENCE_MASK = (1n << SNOWFLAKE_SEQUENCE_BITS) - 1n
const SNOWFLAKE_WORKER_ID_SHIFT = SNOWFLAKE_SEQUENCE_BITS
const SNOWFLAKE_DATACENTER_ID_SHIFT = SNOWFLAKE_SEQUENCE_BITS + SNOWFLAKE_WORKER_ID_BITS
const SNOWFLAKE_TIMESTAMP_SHIFT = SNOWFLAKE_SEQUENCE_BITS + SNOWFLAKE_WORKER_ID_BITS + SNOWFLAKE_DATACENTER_ID_BITS
const SNOWFLAKE_LONG_ID_PATTERN = /^\d{16,19}$/
const SNOWFLAKE_WORKER_STORAGE_KEY = 'agent_chat_snowflake_worker_id'
const SNOWFLAKE_DATACENTER_STORAGE_KEY = 'agent_chat_snowflake_datacenter_id'

const agentInfo = reactive({
  id: null,
  name: '',
  key: ''
})

const agentId = computed(() => {
  const value = route.params.agentId
  return Array.isArray(value) ? String(value[0] || '') : String(value || '')
})
const storageKey = computed(() => `agent_chat_sessions_${agentId.value || 'unknown'}`)

const activeSession = computed(() => {
  return sessions.value.find((item) => item.id === activeSessionId.value) || null
})

const activeMessages = computed(() => {
  return activeSession.value?.messages || []
})

const filteredSessions = computed(() => {
  const keyword = sessionKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return sessions.value
  }
  return sessions.value.filter((session) => {
    return `${session.title || ''} ${session.updatedAt || ''}`.toLowerCase().includes(keyword)
  })
})

const canSend = computed(() => {
  return Boolean(inputMessage.value.trim()) && !streaming.value && Boolean(agentId.value)
})

const STREAM_STAGE_META = {
  reasoning: { label: '推理事件', order: 10 },
  tool: { label: '工具调用事件', order: 20 },
  subagent: { label: '子智能体事件', order: 30 },
  message: { label: '正文事件', order: 40 }
}

const STREAM_EVENT_LABELS = {
  AGENT_START: '智能体开始',
  AGENT_END: '智能体结束',
  MODEL_CALL_START: '模型调用开始',
  MODEL_CALL_END: '模型调用结束',
  TEXT_BLOCK_START: '文本块开始',
  TEXT_BLOCK_DELTA: '文本增量',
  TEXT_BLOCK_END: '文本块结束',
  THINKING_BLOCK_START: '思考开始',
  THINKING_BLOCK_DELTA: '思考增量',
  THINKING_BLOCK_END: '思考结束',
  TOOL_CALL_START: '工具调用开始',
  TOOL_CALL_DELTA: '工具参数增量',
  TOOL_CALL_END: '工具调用结束',
  TOOL_RESULT_START: '工具执行开始',
  TOOL_RESULT_TEXT_DELTA: '工具文本结果',
  TOOL_RESULT_DATA_DELTA: '工具数据结果',
  TOOL_RESULT_END: '工具执行结束',
  REQUIRE_USER_CONFIRM: '等待用户确认',
  REQUIRE_EXTERNAL_EXECUTION: '等待外部执行',
  USER_CONFIRM_RESULT: '用户确认结果',
  EXTERNAL_EXECUTION_RESULT: '外部执行结果',
  REQUEST_STOP: '请求停止',
  SUBAGENT_EXPOSED: '子智能体暴露',
  EXCEED_MAX_ITERS: '达到最大迭代',
  DONE: '流完成',
  ERROR: '执行异常'
}

const SSE_EVENT_LABELS = {
  agent_start: '智能体开始',
  agent_end: '智能体结束',
  model_call_start: '模型调用开始',
  model_call_end: '模型调用结束',
  message_start: '文本块开始',
  message_delta: '文本增量',
  message_end: '文本块结束',
  thinking_start: '思考开始',
  thinking_delta: '思考增量',
  thinking_end: '思考结束',
  tool_call_start: '工具调用开始',
  tool_call_delta: '工具参数增量',
  tool_call_end: '工具调用结束',
  tool_result_start: '工具执行开始',
  tool_result_text_delta: '工具文本结果',
  tool_result_data_delta: '工具数据结果',
  tool_result_end: '工具执行完成',
  require_user_confirm: '等待用户确认',
  require_external_execution: '等待外部执行',
  user_confirm_result: '用户确认结果',
  external_execution_result: '外部执行结果',
  subagent_exposed: '子智能体暴露',
  subagent_agent_start: '子智能体开始',
  subagent_agent_end: '子智能体结束',
  subagent_message_start: '子智能体文本开始',
  subagent_message_delta: '子智能体文本增量',
  subagent_message_end: '子智能体文本结束',
  subagent_thinking_start: '子智能体思考开始',
  subagent_thinking_delta: '子智能体思考增量',
  subagent_thinking_end: '子智能体思考结束',
  request_stop: '请求停止',
  exceed_max_iters: '达到最大迭代',
  error: '执行异常'
}

const PLAN_STATUS_META = {
  DRAFT: { label: '草稿', type: 'info' },
  WAITING_APPROVAL: { label: '待确认', type: 'warning' },
  APPROVED: { label: '已批准', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
  EXECUTING: { label: '执行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' }
}

const PLAN_STATUS_ALIAS = {
  RUNNING: 'EXECUTING',
  IN_PROGRESS: 'EXECUTING',
  WAITING: 'WAITING_APPROVAL',
  WAITING_CONFIRM: 'WAITING_APPROVAL',
  WAITING_USER_CONFIRM: 'WAITING_APPROVAL',
  DONE: 'COMPLETED',
  SUCCESS: 'COMPLETED',
  FINISHED: 'COMPLETED',
  ERROR: 'FAILED'
}

const TASK_STATE_META = {
  PENDING: { label: '待执行', type: 'info' },
  IN_PROGRESS: { label: '执行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  BLOCKED: { label: '阻塞', type: 'danger' },
  FAILED: { label: '失败', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' }
}

const TASK_STATE_ALIAS = {
  TODO: 'PENDING',
  WAITING: 'PENDING',
  RUNNING: 'IN_PROGRESS',
  EXECUTING: 'IN_PROGRESS',
  PROCESSING: 'IN_PROGRESS',
  DONE: 'COMPLETED',
  SUCCESS: 'COMPLETED',
  FINISHED: 'COMPLETED',
  ERROR: 'FAILED'
}

const PLAN_TOOL_NAMES = new Set(['plan_enter', 'plan_write', 'plan_exit', 'todo_write'])

const getStageKey = (sseEvent = '') => {
  const normalizedEvent = String(sseEvent || '').toLowerCase()

  if (normalizedEvent.startsWith('subagent_')) {
    return 'subagent'
  }
  if (normalizedEvent.startsWith('thinking_')) {
    return 'reasoning'
  }
  if (normalizedEvent.startsWith('tool_call_') || normalizedEvent.startsWith('tool_result_')) {
    return 'tool'
  }
  if (normalizedEvent === 'require_user_confirm' ||
    normalizedEvent === 'require_external_execution' ||
    normalizedEvent === 'user_confirm_result' ||
    normalizedEvent === 'external_execution_result') {
    return 'tool'
  }
  if (normalizedEvent.startsWith('message_')) {
    return 'message'
  }

  return null
}

const isStageEndEvent = (sseEvent = '') => {
  const normalizedEvent = String(sseEvent || '').toLowerCase()
  return normalizedEvent === 'error' ||
    normalizedEvent === 'thinking_end' ||
    normalizedEvent === 'tool_result_end' ||
    normalizedEvent === 'message_end' ||
    normalizedEvent === 'agent_end' ||
    normalizedEvent === 'request_stop' ||
    normalizedEvent === 'exceed_max_iters' ||
    normalizedEvent === 'require_user_confirm' ||
    normalizedEvent === 'require_external_execution'
}

const eventLabel = (eventName = '') => {
  return SSE_EVENT_LABELS[eventName] || STREAM_EVENT_LABELS[eventName] || eventName || '未知事件'
}

const formatDuration = (duration = 0) => {
  const milliseconds = Math.max(0, Math.round(duration || 0))
  if (milliseconds < 1000) {
    return `${milliseconds}ms`
  }
  if (milliseconds < 10000) {
    return `${(milliseconds / 1000).toFixed(1)}s`
  }
  return `${Math.round(milliseconds / 1000)}s`
}

const stageStatusText = (stage) => {
  if (stage.status === 'running') {
    return '进行中'
  }
  if (stage.status === 'waiting') {
    return '等待中'
  }
  if (stage.status === 'error') {
    return '异常'
  }
  return '完成'
}

const streamTotalText = (message) => {
  const totalMs = message.streamTiming?.totalMs
  return typeof totalMs === 'number' ? formatDuration(totalMs) : ''
}

const streamUsageTokenText = (message) => {
  const usageToken = Number(message?.usageToken)
  if (!Number.isFinite(usageToken) || usageToken <= 0) {
    return ''
  }
  return Math.round(usageToken).toLocaleString()
}

const streamUsageTimeText = (message) => {
  const usageTime = Number(message?.usageTime)
  if (!Number.isFinite(usageTime) || usageTime <= 0) {
    return ''
  }

  if (usageTime > 1000) {
    return formatDuration(usageTime)
  }
  if (usageTime < 1) {
    return `${Math.round(usageTime * 1000)}ms`
  }
  if (usageTime < 10) {
    return `${usageTime.toFixed(2)}s`
  }
  return `${usageTime.toFixed(1)}s`
}

const hasStreamSummary = (message) => {
  return Boolean(
    message?.eventStages?.length ||
    streamTotalText(message) ||
    streamUsageTokenText(message) ||
    streamUsageTimeText(message)
  )
}

const stageTimeText = (message, stageKey) => {
  const stage = message?.eventStages?.find((item) => item.key === stageKey)
  return stage ? formatDuration(stage.durationMs) : ''
}

const auxiliaryBlockTimeText = (block) => {
  return block?.durationMs ? formatDuration(block.durationMs) : ''
}

const auxiliaryBlockTitle = (block) => {
  if (block?.title) {
    return block.title
  }

  const sequence = block?.sequence ? ` ${block.sequence}` : ''
  if (block?.kind === 'tool') {
    return `工具调用${sequence}`
  }
  if (block?.kind === 'subagent') {
    return `子智能体${sequence}`
  }
  return `推理过程${sequence}`
}

const toolStatusText = (toolCall) => {
  if (toolCall?.status === 'done') {
    return '完成'
  }
  if (toolCall?.status === 'waiting') {
    return '等待确认'
  }
  if (toolCall?.status === 'waiting_external') {
    return '等待外部执行'
  }
  if (toolCall?.status === 'error') {
    return '异常'
  }
  return '调用中'
}

const isRecord = (value) => Boolean(value) && typeof value === 'object' && !Array.isArray(value)

// 统一计划状态大小写，避免后端枚举大小写变化影响展示。
const normalizePlanStatus = (status = '') => {
  const normalized = String(status || '').toUpperCase()
  return PLAN_STATUS_ALIAS[normalized] || normalized
}

// 统一任务状态大小写，并为空状态提供 PENDING 兜底。
const normalizeTaskState = (state = '') => {
  const normalized = String(state || 'PENDING').toUpperCase()
  return TASK_STATE_ALIAS[normalized] || normalized
}

const normalizePlanToolName = (toolName = '') => String(toolName || '').trim().toLowerCase()

const isPlanToolName = (toolName = '') => PLAN_TOOL_NAMES.has(normalizePlanToolName(toolName))

const isPlanEventType = (type = '') => {
  const normalized = String(type || '').toUpperCase()
  return normalized.startsWith('PLAN_') || normalized.startsWith('TODO_')
}

// 根据计划状态返回标签文案和 Element Plus 标签类型。
const planStatusMeta = (status) => PLAN_STATUS_META[normalizePlanStatus(status)] || { label: status || '未知', type: 'info' }

// 根据任务状态返回标签文案和 Element Plus 标签类型。
const taskStateMeta = (state) => TASK_STATE_META[normalizeTaskState(state)] || { label: state || '未知', type: 'info' }

// 返回计划状态中文文案。
const planStatusText = (status) => planStatusMeta(status).label

// 返回计划状态对应的标签颜色类型。
const planStatusType = (status) => planStatusMeta(status).type

// 返回任务状态中文文案。
const taskStateText = (state) => taskStateMeta(state).label

// 返回任务状态对应的标签颜色类型。
const taskStateType = (state) => taskStateMeta(state).type

// 标准化后端任务快照，补齐前端渲染任务行所需的字段。
const normalizePlanTask = (task = {}, index = 0) => ({
  id: task.id || `${task.planId || 'plan'}-${task.taskIndex || index + 1}`,
  planId: task.planId || '',
  taskIndex: Number(task.taskIndex ?? task.index ?? task.step ?? task.order) || index + 1,
  subject: task.subject || task.title || task.name || task.content || task.task || `任务 ${index + 1}`,
  detail: task.detail || task.description || task.desc || task.summary || '',
  state: normalizeTaskState(task.state || task.status || task.taskState),
  priority: task.priority || task.level || '',
  owner: task.owner || '',
  blocks: Array.isArray(task.blocks) ? task.blocks : [],
  blockedBy: Array.isArray(task.blockedBy) ? task.blockedBy : [],
  startedAt: task.startedAt || '',
  finishedAt: task.finishedAt || ''
})

// 根据任务列表计算本地进度，用于后端没有传 progress 时兜底。
const buildPlanProgress = (tasks = []) => {
  const total = tasks.length
  const completed = tasks.filter((task) => normalizeTaskState(task.state) === 'COMPLETED').length
  const inProgress = tasks.filter((task) => normalizeTaskState(task.state) === 'IN_PROGRESS').length
  const pending = tasks.filter((task) => normalizeTaskState(task.state) === 'PENDING').length
  return {
    total,
    completed,
    inProgress,
    pending,
    percent: total ? Math.round((completed * 100) / total) : 0
  }
}

// 标准化后端进度快照，并用任务列表补齐缺失统计项。
const normalizePlanProgress = (progress = {}, tasks = []) => {
  const fallback = buildPlanProgress(tasks)
  const total = Number(progress.total ?? fallback.total) || 0
  const completed = Number(progress.completed ?? fallback.completed) || 0
  const inProgress = Number(progress.inProgress ?? fallback.inProgress) || 0
  const pending = Number(progress.pending ?? fallback.pending) || 0
  return {
    total,
    completed,
    inProgress,
    pending,
    percent: total ? Number(progress.percent ?? Math.round((completed * 100) / total)) || 0 : 0
  }
}

// 判断当前消息是否已经包含计划或任务信息，决定是否显示 Plan 面板。
const hasPlanOutput = (message) => {
  return Boolean(
    message?.planState ||
    message?.lastPlanEvent ||
    (Array.isArray(message?.planTasks) && message.planTasks.length)
  )
}

// 生成 Plan 面板标题，优先展示计划标题，其次展示计划编号。
const planPanelTitle = (message) => {
  return message?.planState?.title || message?.planState?.planNo || '执行计划'
}

// 生成进度短文案，例如 3/5；没有任务时不展示。
const planProgressText = (message) => {
  const progress = message?.planProgress || buildPlanProgress(message?.planTasks || [])
  return progress.total ? `${progress.completed}/${progress.total}` : ''
}

const latestAssistantMessage = computed(() => {
  const messages = activeMessages.value
  for (let index = messages.length - 1; index >= 0; index -= 1) {
    if (messages[index]?.role === 'assistant') {
      return messages[index]
    }
  }
  return null
})

const activePlanMessage = computed(() => {
  const message = latestAssistantMessage.value
  return message && hasPlanOutput(message) ? message : null
})

const executionPlanDurationText = computed(() => {
  const message = activePlanMessage.value
  return message ? (streamUsageTimeText(message) || streamTotalText(message)) : ''
})

const executionPlanProgress = computed(() => {
  const message = activePlanMessage.value
  return message?.planProgress || buildPlanProgress(message?.planTasks || [])
})


const executionPlanEventText = computed(() => {
  const event = activePlanMessage.value?.lastPlanEvent
  if (!event?.toolName && !event?.type) {
    return ''
  }
  const toolName = normalizePlanToolName(event.toolName)
  const labels = {
    plan_enter: '进入计划',
    plan_write: '写入计划',
    todo_write: '同步任务',
    plan_exit: '申请执行'
  }
  return labels[toolName] || event.type || ''
})

const togglePlanDrawer = () => {
  planDrawerOpen.value = !planDrawerOpen.value
}

const executionTaskStateClass = (state) => normalizeTaskState(state).toLowerCase()

const isExecutionTaskDone = (task) => normalizeTaskState(task?.state) === 'COMPLETED'

const isExecutionTaskRunning = (task) => normalizeTaskState(task?.state) === 'IN_PROGRESS'

const taskPriorityClass = (priority = '') => String(priority || '').toLowerCase()

const executionTaskToolLabel = (task = {}) => {
  const block = (task.blocks || []).find((item) => item?.toolCall?.name || item?.toolName || item?.name)
  const toolName = block?.toolCall?.name || block?.toolName || block?.name || ''
  const labels = {
    sql_query: 'SQL Query',
    query_order: 'SQL Query',
    knowledge_search: 'Knowledge Search',
    search_knowledge: 'Knowledge Search',
    web_search: 'Web Search',
    plan_write: 'Plan Write',
    workspace_write: 'Workspace Write',
    database_query: 'Database Query'
  }
  return labels[String(toolName).toLowerCase()] || toolName
}

const hasPlanPayloadShape = (payload) => {
  return isRecord(payload) && (
    isRecord(payload.plan) ||
    Array.isArray(payload.tasks) ||
    isRecord(payload.progress) ||
    isPlanToolName(payload.toolName) ||
    isPlanEventType(payload.type)
  )
}

const extractPlanEventPayload = (streamEvent = {}) => {
  const payload = isRecord(streamEvent.payload) ? streamEvent.payload : null
  const dataPayload = isRecord(streamEvent.data?.payload) ? streamEvent.data.payload : null
  const candidates = [
    payload?.planEvent,
    payload?.plan_event,
    dataPayload?.planEvent,
    dataPayload?.plan_event,
    payload,
    dataPayload
  ]

  return candidates.find(hasPlanPayloadShape) || null
}

const planStatusFromEvent = (planEvent = {}, currentStatus = '') => {
  if (planEvent.plan?.status) {
    return normalizePlanStatus(planEvent.plan.status)
  }

  const type = String(planEvent.type || '').toUpperCase()
  if (type.includes('EXIT_REQUEST') || type.includes('WAITING')) {
    return 'WAITING_APPROVAL'
  }
  if (type.includes('APPROVE') || type.includes('EXECUTING')) {
    return 'EXECUTING'
  }
  if (type.includes('REJECT')) {
    return 'REJECTED'
  }
  if (type.includes('CANCEL')) {
    return 'CANCELLED'
  }
  if (type.includes('FAILED') || type.includes('ERROR')) {
    return 'FAILED'
  }
  if (type.includes('TODO')) {
    return currentStatus ? normalizePlanStatus(currentStatus) : 'EXECUTING'
  }
  if (type.includes('START') || type.includes('ENTER') || type.includes('WRITE')) {
    return currentStatus ? normalizePlanStatus(currentStatus) : 'DRAFT'
  }

  return currentStatus ? normalizePlanStatus(currentStatus) : 'EXECUTING'
}

const planTitleFromEvent = (planEvent = {}) => {
  if (planEvent.plan?.title || planEvent.plan?.planNo) {
    return planEvent.plan.title || planEvent.plan.planNo
  }
  if (isPlanToolName(planEvent.toolName)) {
    const labels = {
      plan_enter: '进入计划模式',
      plan_write: '生成执行计划',
      todo_write: '同步任务列表',
      plan_exit: '申请执行计划'
    }
    return labels[normalizePlanToolName(planEvent.toolName)] || '执行计划'
  }
  return '执行计划'
}

const isPlanExitEvent = (planEvent = {}) => {
  const toolName = normalizePlanToolName(planEvent.toolName)
  const type = String(planEvent.type || '').toUpperCase()
  return toolName === 'plan_exit' ||
    type === 'PLAN_EXIT' ||
    type.includes('PLAN_EXIT') ||
    type.includes('EXIT_REQUEST')
}

const parseJsonLikeText = (text = '') => {
  const value = String(text || '').trim()
  if (!value) {
    return null
  }

  const tryParse = (candidate) => {
    try {
      return JSON.parse(candidate)
    } catch {
      return null
    }
  }

  const direct = tryParse(value)
  if (direct) {
    return direct
  }

  const fenceMatch = value.match(/```(?:json)?\s*([\s\S]*?)```/i)
  if (fenceMatch) {
    const fenced = tryParse(fenceMatch[1].trim())
    if (fenced) {
      return fenced
    }
  }

  const objectStart = value.indexOf('{')
  const objectEnd = value.lastIndexOf('}')
  if (objectStart >= 0 && objectEnd > objectStart) {
    const objectValue = tryParse(value.slice(objectStart, objectEnd + 1))
    if (objectValue) {
      return objectValue
    }
  }

  const arrayStart = value.indexOf('[')
  const arrayEnd = value.lastIndexOf(']')
  if (arrayStart >= 0 && arrayEnd > arrayStart) {
    return tryParse(value.slice(arrayStart, arrayEnd + 1))
  }

  return null
}

const extractTasksFromParsedPayload = (payload) => {
  if (Array.isArray(payload)) {
    return payload
  }
  if (!isRecord(payload)) {
    return []
  }
  const keys = ['tasks', 'todos', 'todoList', 'taskList', 'items', 'list']
  for (const key of keys) {
    if (Array.isArray(payload[key])) {
      return payload[key]
    }
  }
  return []
}

const extractPlanFromParsedPayload = (payload) => {
  if (!isRecord(payload)) {
    return null
  }
  if (isRecord(payload.plan)) {
    return payload.plan
  }
  const plan = {}
  if (payload.title || payload.goal || payload.content || payload.planContent) {
    plan.title = payload.title || ''
    plan.goal = payload.goal || ''
    plan.planContent = payload.planContent || payload.content || ''
  }
  return Object.keys(plan).length ? plan : null
}

// 将 SSE 中的 planEvent 快照合并到当前 assistant 消息，驱动计划面板实时刷新。
const applyPlanEventPayload = (message, streamEvent) => {
  const planEvent = extractPlanEventPayload(streamEvent)
  if (!message || !planEvent) {
    return false
  }

  // 后端把计划和任务快照放在 payload.planEvent，前端直接合并即可实时刷新进度。
  if (planEvent.plan) {
    message.planState = {
      ...(message.planState || {}),
      ...planEvent.plan
    }
  }

  if (!message.planState) {
    message.planState = {
      title: planTitleFromEvent(planEvent),
      status: planStatusFromEvent(planEvent)
    }
  } else {
    message.planState = {
      ...message.planState,
      status: planStatusFromEvent(planEvent, message.planState.status)
    }
    if (!message.planState.title) {
      message.planState.title = planTitleFromEvent(planEvent)
    }
  }

  const currentTasks = Array.isArray(message.planTasks) ? message.planTasks : []
  const incomingTasks = Array.isArray(planEvent.tasks)
    ? planEvent.tasks.map(normalizePlanTask)
    : null

  if (incomingTasks?.length) {
    // todo_write 每次都是完整任务列表，这里按非空快照覆盖本地状态。
    message.planTasks = incomingTasks
  } else if (!Array.isArray(message.planTasks)) {
    message.planTasks = []
  } else {
    // plan_enter/plan_write 等事件可能携带空 tasks，不应清空 todo_write 已解析出的任务列表。
    message.planTasks = currentTasks
  }

  const progressTotal = Number(planEvent.progress?.total)
  const shouldUseIncomingProgress = incomingTasks?.length ||
    !message.planTasks.length ||
    (Number.isFinite(progressTotal) && progressTotal > 0)
  message.planProgress = normalizePlanProgress(
    shouldUseIncomingProgress ? planEvent.progress : {},
    message.planTasks || []
  )
  message.lastPlanEvent = {
    type: planEvent.type || '',
    toolName: planEvent.toolName || '',
    occurredAt: planEvent.occurredAt || ''
  }
  message.planExpanded = message.planExpanded ?? true
  if (isPlanExitEvent(planEvent)) {
    planDrawerOpen.value = true
  }
  return true
}

const applyPlanFallbackFromTool = (message) => {
  if (!message || !Array.isArray(message.toolCalls) || !message.toolCalls.length) {
    return false
  }

  const toolCall = message.toolCalls[message.toolCalls.length - 1]
  const toolName = normalizePlanToolName(toolCall?.name)
  if (!isPlanToolName(toolName)) {
    return false
  }

  const parsedInput = parseJsonLikeText(toolCall.rawInput || '')
  const parsedResult = parseJsonLikeText(toolCall.rawResult || toolCall.result || '')
  const tasks = extractTasksFromParsedPayload(parsedResult).length
    ? extractTasksFromParsedPayload(parsedResult)
    : extractTasksFromParsedPayload(parsedInput)
  const planFromTool = extractPlanFromParsedPayload(parsedResult) || extractPlanFromParsedPayload(parsedInput)
  let changed = false

  if (!message.planState) {
    message.planState = {
      title: planTitleFromEvent({ toolName }),
      status: toolName === 'todo_write' ? 'EXECUTING' : 'DRAFT'
    }
    changed = true
  }

  if (planFromTool) {
    message.planState = {
      ...message.planState,
      ...planFromTool
    }
    changed = true
  }

  if (tasks.length) {
    message.planTasks = tasks.map(normalizePlanTask)
    message.planProgress = normalizePlanProgress({}, message.planTasks)
    changed = true
  }

  if (changed || isPlanToolName(toolName)) {
    message.lastPlanEvent = {
      type: `${toolName.toUpperCase()}_STREAM`,
      toolName,
      occurredAt: nowText()
    }
    message.planExpanded = true
    if (toolName === 'plan_exit') {
      planDrawerOpen.value = true
    }
    return true
  }

  return false
}

// 切换 Plan 面板展开状态，并持久化到本地会话缓存。
const togglePlanPanel = (message) => {
  if (!message) {
    return
  }
  message.planExpanded = !message.planExpanded
  saveSessions()
}

const toggleAuxiliaryPanel = (message, key) => {
  if (!message) {
    return
  }

  if (typeof key === 'object' && key) {
    key.expanded = !key.expanded
    key.expandedByUser = true
    saveSessions()
    return
  }

  if (key === 'thinking') {
    message.thinkingExpanded = !message.thinkingExpanded
  }
  if (key === 'tool') {
    message.toolExpanded = !message.toolExpanded
  }

  saveSessions()
}

const normalizedSseEvent = (streamEvent) => String(streamEvent?.sseEvent || '').toLowerCase()

const isReasoningStartEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'thinking_start'
}

const isReasoningDeltaEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'thinking_delta'
}

const isReasoningEndEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'thinking_end'
}

const isReasoningStreamEvent = (streamEvent) => {
  return isReasoningStartEvent(streamEvent) ||
    isReasoningDeltaEvent(streamEvent) ||
    isReasoningEndEvent(streamEvent)
}

const isToolStreamEvent = (streamEvent) => {
  const sseEvent = normalizedSseEvent(streamEvent)
  return sseEvent.startsWith('tool_call_') ||
    sseEvent.startsWith('tool_result_')
}

const isToolCallStartEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'tool_call_start'
}

const isToolCallDeltaEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'tool_call_delta'
}

const isToolCallEndEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'tool_call_end'
}

const isToolResultStartEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'tool_result_start'
}

const isToolResultTextDeltaEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'tool_result_text_delta'
}

const isToolResultDataDeltaEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'tool_result_data_delta'
}

const isToolResultEndEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'tool_result_end'
}

const isRequireUserConfirmEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'require_user_confirm'
}

const isRequireExternalExecutionEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'require_external_execution'
}

const isInterventionEvent = (streamEvent) => {
  return isRequireUserConfirmEvent(streamEvent) || isRequireExternalExecutionEvent(streamEvent)
}

const normalizeSourcePath = (sourcePath = '') => String(sourcePath || '').trim()

const subAgentNameFromSource = (sourcePath = '') => {
  const normalizedPath = normalizeSourcePath(sourcePath).replace(/\\/g, '/')
  if (!normalizedPath || normalizedPath.toLowerCase() === 'main') {
    return ''
  }
  const parts = normalizedPath.split('/').filter(Boolean)
  return parts[parts.length - 1] || normalizedPath
}

const getSubAgentName = (streamEvent) => {
  return streamEvent?.subAgentName || subAgentNameFromSource(streamEvent?.sourcePath) || ''
}

const isSubAgentStreamEvent = (streamEvent) => {
  const sseEvent = normalizedSseEvent(streamEvent)
  return sseEvent.startsWith('subagent_')
}

const isSubAgentTextDeltaEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'subagent_message_delta'
}

const isSubAgentThinkingDeltaEvent = (streamEvent) => {
  return normalizedSseEvent(streamEvent) === 'subagent_thinking_delta'
}

const isSubAgentStartEvent = (streamEvent) => {
  if (!isSubAgentStreamEvent(streamEvent)) {
    return false
  }
  const sseEvent = normalizedSseEvent(streamEvent)
  return sseEvent === 'subagent_agent_start' ||
    sseEvent === 'subagent_message_start'
}

const isSubAgentEndEvent = (streamEvent) => {
  if (!isSubAgentStreamEvent(streamEvent)) {
    return false
  }
  const sseEvent = normalizedSseEvent(streamEvent)
  return sseEvent === 'subagent_agent_end' ||
    sseEvent === 'subagent_message_end' ||
    sseEvent === 'subagent_thinking_end'
}

const hasAuxiliaryOutput = (message) => {
  return Boolean(
    (Array.isArray(message?.auxiliaryBlocks) && message.auxiliaryBlocks.length) ||
    message?.thinkingContent ||
    message?.toolResultContent ||
    (Array.isArray(message?.toolCalls) && message.toolCalls.length) ||
    hasPlanOutput(message)
  )
}

const getNowMs = () => {
  return typeof performance !== 'undefined' ? performance.now() : Date.now()
}

const createToolCallId = () => {
  return `tool-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
}

const createToolCall = (name = '') => {
  const now = getNowMs()
  return {
    id: createToolCallId(),
    name: name || '工具调用',
    process: '',
    result: '',
    rawInput: '',
    rawResult: '',
    status: 'running',
    argumentStarted: false,
    resultStarted: false,
    startedAt: now,
    updatedAt: now
  }
}

const createAuxiliaryBlockId = (kind) => {
  return `${kind}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
}

const createContentSegmentId = () => {
  return `content-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
}

const ensureAuxiliaryBlocks = (message) => {
  if (!Array.isArray(message.auxiliaryBlocks)) {
    message.auxiliaryBlocks = []
  }
  return message.auxiliaryBlocks
}

const ensureContentSegments = (message) => {
  if (!Array.isArray(message.contentSegments)) {
    message.contentSegments = []
  }
  return message.contentSegments
}

const appendContentSegment = (message, delta) => {
  const text = delta == null ? '' : String(delta)
  if (!text) {
    return
  }

  const segments = ensureContentSegments(message)
  const lastSegment = segments[segments.length - 1]
  if (lastSegment?.type === 'content') {
    lastSegment.content = `${lastSegment.content || ''}${text}`
    return
  }

  segments.push({
    id: createContentSegmentId(),
    type: 'content',
    content: text
  })
}

const replaceContentSegments = (message, content) => {
  if (!message) {
    return
  }

  message.contentSegments = content ? [{
    id: createContentSegmentId(),
    type: 'content',
    content
  }] : []
}

const appendAuxiliaryBlockSegment = (message, block) => {
  if (!message || !block?.id) {
    return
  }

  const segments = ensureContentSegments(message)
  if (segments.some((segment) => segment.type === 'auxiliary' && segment.blockId === block.id)) {
    return
  }

  segments.push({
    id: `auxiliary-${block.id}`,
    type: 'auxiliary',
    blockId: block.id
  })
}

const normalizeContentSegments = (message, auxiliaryBlocks = []) => {
  const blocksById = new Set(auxiliaryBlocks.map((block) => block.id).filter(Boolean))
  const normalized = []

  if (Array.isArray(message?.contentSegments) && message.contentSegments.length) {
    message.contentSegments.forEach((segment, index) => {
      if (segment?.type === 'content') {
        const content = segment.content == null ? '' : String(segment.content)
        if (content) {
          normalized.push({
            id: segment.id || `content-stored-${index}`,
            type: 'content',
            content
          })
        }
        return
      }

      const blockId = segment?.blockId || segment?.id
      if (blockId && blocksById.has(blockId)) {
        normalized.push({
          id: segment.id || `auxiliary-${blockId}`,
          type: 'auxiliary',
          blockId
        })
      }
    })

    const referencedBlocks = new Set(normalized.filter((segment) => segment.type === 'auxiliary').map((segment) => segment.blockId))
    auxiliaryBlocks.forEach((block) => {
      if (block?.id && !referencedBlocks.has(block.id)) {
        normalized.push({
          id: `auxiliary-${block.id}`,
          type: 'auxiliary',
          blockId: block.id
        })
      }
    })

    return normalized
  }

  if (message?.role === 'assistant') {
    auxiliaryBlocks.forEach((block) => {
      if (block?.id) {
        normalized.push({
          id: `auxiliary-${block.id}`,
          type: 'auxiliary',
          blockId: block.id
        })
      }
    })
  }

  const content = message?.content ?? message?.displayContent ?? ''
  if (content) {
    normalized.push({
      id: `content-${message?.id || 'message'}-legacy`,
      type: 'content',
      content
    })
  }

  return normalized
}

const syncContentSegmentsWithBlocks = (message) => {
  if (!message || !Array.isArray(message.contentSegments)) {
    return
  }

  const blockIds = new Set((message.auxiliaryBlocks || []).map((block) => block.id).filter(Boolean))
  message.contentSegments = message.contentSegments.filter((segment) => {
    if (segment?.type === 'content') {
      return Boolean(segment.content)
    }
    return Boolean(segment?.blockId && blockIds.has(segment.blockId))
  })
}

const findAuxiliaryBlock = (message, blockId) => {
  if (!blockId) {
    return null
  }
  return ensureAuxiliaryBlocks(message).find((block) => block.id === blockId) || null
}

const findLastRunningBlock = (message, kind) => {
  const blocks = ensureAuxiliaryBlocks(message)
  for (let index = blocks.length - 1; index >= 0; index -= 1) {
    const block = blocks[index]
    if (block.kind === kind && block.status === 'running') {
      return block
    }
  }
  return null
}

const nextAuxiliaryBlockSequence = (message, kind) => {
  return ensureAuxiliaryBlocks(message).filter((block) => block.kind === kind).length + 1
}

const createAuxiliaryBlock = (message, kind, extra = {}) => {
  const now = getNowMs()
  const sequence = nextAuxiliaryBlockSequence(message, kind)
  const defaultTitle = kind === 'tool'
    ? `工具调用 ${sequence}`
    : kind === 'subagent'
      ? `子智能体 ${sequence}`
      : `推理过程 ${sequence}`
  return {
    id: createAuxiliaryBlockId(kind),
    kind,
    sequence,
    title: defaultTitle,
    expanded: false,
    expandedByUser: false,
    status: 'running',
    startedAt: now,
    endedAt: null,
    durationMs: 0,
    content: '',
    ...extra
  }
}

const touchAuxiliaryBlock = (block, status = 'running') => {
  const now = getNowMs()
  block.status = status
  block.durationMs = block.startedAt ? now - block.startedAt : 0
  if (status === 'running') {
    block.endedAt = null
    return
  }
  block.endedAt = now
}

const normalizeAuxiliaryBlock = (block, index = 0) => {
  const kind = block?.kind === 'tool'
    ? 'tool'
    : block?.kind === 'subagent'
      ? 'subagent'
      : 'reasoning'
  const toolCall = kind === 'tool'
    ? {
      id: block.toolCall?.id || block.id || createToolCallId(),
      name: block.toolCall?.name || '工具调用',
      process: block.toolCall?.process || '',
      result: block.toolCall?.result || '',
      rawInput: block.toolCall?.rawInput || '',
      rawResult: block.toolCall?.rawResult || '',
      status: block.toolCall?.status || block.status || 'done',
      argumentStarted: Boolean(block.toolCall?.argumentStarted),
      resultStarted: Boolean(block.toolCall?.resultStarted),
      startedAt: block.toolCall?.startedAt || block.startedAt || 0,
      updatedAt: block.toolCall?.updatedAt || block.endedAt || 0
    }
    : null
  const subAgent = kind === 'subagent'
    ? {
      name: block?.subAgent?.name || block?.subAgentName || '子智能体',
      sourcePath: block?.subAgent?.sourcePath || block?.sourcePath || '',
      status: block?.subAgent?.status || block?.status || 'done'
    }
    : null
  const title = block?.title || (kind === 'tool'
    ? `工具调用 ${block?.sequence || index + 1}`
    : kind === 'subagent'
      ? `子智能体 ${subAgent?.name || block?.sequence || index + 1}`
      : `推理过程 ${block?.sequence || index + 1}`)

  return {
    id: block?.id || `${kind}-stored-${index}`,
    kind,
    sequence: block?.sequence || index + 1,
    title,
    expanded: block?.expandedByUser ? Boolean(block.expanded) : false,
    expandedByUser: Boolean(block?.expandedByUser),
    status: block?.status || 'done',
    startedAt: block?.startedAt || 0,
    endedAt: block?.endedAt || null,
    durationMs: block?.durationMs || 0,
    content: block?.content || '',
    toolCall,
    subAgent
  }
}

const normalizeStoredToolCalls = (message) => {
  if (Array.isArray(message?.toolCalls)) {
    return message.toolCalls.map((toolCall, index) => ({
      id: toolCall.id || `tool-${message.id || 'message'}-${index}`,
      name: toolCall.name || '工具调用',
      process: toolCall.process || '',
      result: toolCall.result || '',
      rawInput: toolCall.rawInput || '',
      rawResult: toolCall.rawResult || '',
      status: toolCall.status || 'done',
      argumentStarted: Boolean(toolCall.argumentStarted),
      resultStarted: Boolean(toolCall.resultStarted),
      startedAt: toolCall.startedAt || 0,
      updatedAt: toolCall.updatedAt || 0
    }))
  }

  if (message?.toolResultContent) {
    return [{
      id: `tool-${message.id || 'message'}-legacy`,
      name: '工具调用',
      process: '',
      result: message.toolResultContent,
      status: 'done',
      argumentStarted: false,
      resultStarted: true,
      startedAt: 0,
      updatedAt: 0
    }]
  }

  return []
}

const isGenericToolName = (name = '') => {
  return !name || name === '工具调用'
}

const appendMergedText = (current = '', next = '') => {
  if (!next) {
    return current || ''
  }
  if (!current) {
    return next
  }
  return `${current}\n${next}`
}

const isEmptyReasoningBlock = (block) => {
  return block?.kind === 'reasoning' && !String(block.content || '').trim()
}

const shouldMergeToolBlock = (previous, current) => {
  return previous?.kind === 'tool' &&
    current?.kind === 'tool' &&
    previous.toolCall &&
    current.toolCall &&
    !previous.toolCall.result &&
    isGenericToolName(current.toolCall.name)
}

const mergeToolBlocks = (previous, current) => {
  previous.toolCall.process = appendMergedText(previous.toolCall.process, current.toolCall.process)
  previous.toolCall.result = appendMergedText(previous.toolCall.result, current.toolCall.result)
  previous.toolCall.status = current.toolCall.status || previous.toolCall.status
  previous.toolCall.resultStarted = previous.toolCall.resultStarted || current.toolCall.resultStarted
  previous.toolCall.argumentStarted = previous.toolCall.argumentStarted || current.toolCall.argumentStarted
  previous.toolCall.updatedAt = current.toolCall.updatedAt || previous.toolCall.updatedAt
  previous.status = current.status || previous.status
  previous.endedAt = current.endedAt || previous.endedAt

  if (previous.startedAt && current.endedAt) {
    previous.durationMs = current.endedAt - previous.startedAt
  } else {
    previous.durationMs = Math.max(previous.durationMs || 0, current.durationMs || 0)
  }
}

const renumberAuxiliaryBlocks = (blocks = []) => {
  const counts = { reasoning: 0, tool: 0, subagent: 0 }
  blocks.forEach((block) => {
    counts[block.kind] = (counts[block.kind] || 0) + 1
    block.sequence = counts[block.kind]
    if (block.kind === 'tool') {
      block.title = `工具调用 ${block.sequence}`
    } else if (block.kind === 'subagent') {
      block.title = `子智能体 ${block.subAgent?.name || block.subAgentName || block.sequence}`
    } else {
      block.title = `推理过程 ${block.sequence}`
    }
  })
  return blocks
}

const compactAuxiliaryBlocks = (blocks = []) => {
  const compactBlocks = blocks.reduce((result, block) => {
    if (isEmptyReasoningBlock(block)) {
      return result
    }

    const previous = result[result.length - 1]
    if (shouldMergeToolBlock(previous, block)) {
      mergeToolBlocks(previous, block)
      return result
    }

    result.push(block)
    return result
  }, [])

  return renumberAuxiliaryBlocks(compactBlocks)
}

const stageDurationMs = (message, stageKey) => {
  const stage = message?.eventStages?.find((item) => item.key === stageKey)
  return stage?.durationMs || 0
}

const normalizeStoredAuxiliaryBlocks = (message, toolCalls = normalizeStoredToolCalls(message)) => {
  if (Array.isArray(message?.auxiliaryBlocks) && message.auxiliaryBlocks.length) {
    const counts = { reasoning: 0, tool: 0, subagent: 0 }
    const blocks = message.auxiliaryBlocks.map((block, index) => {
      const normalized = normalizeAuxiliaryBlock(block, index)
      counts[normalized.kind] += 1
      normalized.sequence = normalized.sequence || counts[normalized.kind]
      normalized.title = normalized.title || (normalized.kind === 'tool'
        ? `工具调用 ${normalized.sequence}`
        : normalized.kind === 'subagent'
          ? `子智能体 ${normalized.subAgent?.name || normalized.sequence}`
          : `推理过程 ${normalized.sequence}`)
      return normalized
    })
    return compactAuxiliaryBlocks(blocks)
  }

  const blocks = []
  if (message?.thinkingContent) {
    blocks.push(normalizeAuxiliaryBlock({
      id: `reasoning-${message.id || 'message'}-legacy`,
      kind: 'reasoning',
      sequence: 1,
      title: '推理过程 1',
      expanded: false,
      expandedByUser: false,
      status: 'done',
      durationMs: stageDurationMs(message, 'reasoning'),
      content: message.thinkingContent
    }, 0))
  }

  toolCalls.forEach((toolCall, index) => {
    blocks.push(normalizeAuxiliaryBlock({
      id: `tool-block-${toolCall.id || index}`,
      kind: 'tool',
      sequence: index + 1,
      title: `工具调用 ${index + 1}`,
      expanded: false,
      expandedByUser: false,
      status: toolCall.status || 'done',
      durationMs: stageDurationMs(message, 'tool'),
      toolCall
    }, blocks.length))
  })

  return compactAuxiliaryBlocks(blocks)
}

const ensureToolCalls = (message) => {
  if (!Array.isArray(message.toolCalls)) {
    message.toolCalls = []
  }
  return message.toolCalls
}

const appendToolLine = (toolCall, key, text) => {
  const value = String(text || '').trim()
  if (!value) {
    return
  }
  toolCall[key] = toolCall[key] ? `${toolCall[key]}\n${value}` : value
}

const appendToolText = (toolCall, key, text) => {
  const value = text == null ? '' : String(text)
  if (!value) {
    return
  }
  toolCall[key] = `${toolCall[key] || ''}${value}`
}

const getActiveAuxiliaryBlock = (message, kind, shouldCreate = false, extra = {}) => {
  const blocks = ensureAuxiliaryBlocks(message)
  const lastBlock = blocks[blocks.length - 1]

  if (shouldCreate || !lastBlock || lastBlock.kind !== kind || lastBlock.status !== 'running') {
    const block = createAuxiliaryBlock(message, kind, extra)
    blocks.push(block)
    return block
  }

  return lastBlock
}

const getReasoningBlock = (message, shouldCreate = false) => {
  let block = findAuxiliaryBlock(message, message.activeReasoningBlockId)

  if (!block && !shouldCreate) {
    block = findLastRunningBlock(message, 'reasoning')
  }

  if (!block && shouldCreate) {
    block = createAuxiliaryBlock(message, 'reasoning', {
      startedAt: message.pendingReasoningStartedAt || getNowMs()
    })
    ensureAuxiliaryBlocks(message).push(block)
    appendAuxiliaryBlockSegment(message, block)
  }

  if (block) {
    message.activeReasoningBlockId = block.id
  }

  return block
}

const appendReasoningEvent = (message, streamEvent) => {
  if (!message || !isReasoningStreamEvent(streamEvent)) {
    return false
  }

  const delta = streamEvent?.delta == null ? '' : String(streamEvent.delta)
  if (isReasoningStartEvent(streamEvent)) {
    const activeBlock = getReasoningBlock(message)
    if (activeBlock && activeBlock.content) {
      touchAuxiliaryBlock(activeBlock, 'done')
    }
    message.activeReasoningBlockId = null
    message.pendingReasoningStartedAt = getNowMs()
    return false
  }

  const block = getReasoningBlock(message, isReasoningDeltaEvent(streamEvent))
  if (!block) {
    message.pendingReasoningStartedAt = null
    return false
  }

  if (isReasoningDeltaEvent(streamEvent)) {
    block.content = `${block.content || ''}${delta}`
    message.thinkingContent = `${message.thinkingContent || ''}${delta}`
  }

  if (isReasoningEndEvent(streamEvent)) {
    touchAuxiliaryBlock(block, 'done')
    message.activeReasoningBlockId = null
    message.pendingReasoningStartedAt = null
  } else {
    touchAuxiliaryBlock(block, 'running')
  }

  return true
}

const getToolBlock = (message, shouldCreate = false, toolName = '') => {
  let block = shouldCreate ? null : findAuxiliaryBlock(message, message.activeToolBlockId)

  if (!block && !shouldCreate) {
    block = findLastRunningBlock(message, 'tool')
  }

  if (!block) {
    block = createAuxiliaryBlock(message, 'tool', { toolCall: createToolCall(toolName) })
    ensureAuxiliaryBlocks(message).push(block)
    ensureToolCalls(message).push(block.toolCall)
    appendAuxiliaryBlockSegment(message, block)
  }

  if (block) {
    message.activeToolBlockId = block.id
  }

  return block
}

const getToolCallFromBlock = (message, block, toolName = '') => {
  if (!block.toolCall) {
    block.toolCall = createToolCall(toolName)
    ensureToolCalls(message).push(block.toolCall)
  }

  if (toolName && (!block.toolCall.name || block.toolCall.name === '工具调用')) {
    block.toolCall.name = toolName
  }

  return block.toolCall
}

const appendToolEvent = (message, streamEvent) => {
  if (!message || !isToolStreamEvent(streamEvent)) {
    return false
  }

  const delta = streamEvent?.delta == null ? '' : String(streamEvent.delta)
  const isStartEvent = isToolCallStartEvent(streamEvent)
  const block = getToolBlock(message, isStartEvent, isStartEvent ? delta : '')
  const toolCall = getToolCallFromBlock(message, block, isStartEvent ? delta : '')
  toolCall.updatedAt = getNowMs()

  if (isStartEvent) {
    if (delta) {
      toolCall.name = delta
    }
    appendToolLine(toolCall, 'process', '开始调用工具')
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (isToolCallDeltaEvent(streamEvent)) {
    if (!toolCall.argumentStarted) {
      appendToolLine(toolCall, 'process', '调用参数：')
      toolCall.argumentStarted = true
    }
    toolCall.rawInput = `${toolCall.rawInput || ''}${delta}`
    appendToolText(toolCall, 'process', delta)
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (isToolCallEndEvent(streamEvent)) {
    appendToolLine(toolCall, 'process', '调用参数准备完成')
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (isToolResultStartEvent(streamEvent)) {
    if (!toolCall.resultStarted) {
      appendToolLine(toolCall, 'process', '工具开始执行')
      toolCall.resultStarted = true
    }
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (isToolResultTextDeltaEvent(streamEvent)) {
    toolCall.rawResult = `${toolCall.rawResult || ''}${delta}`
    appendToolText(toolCall, 'result', delta)
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (isToolResultDataDeltaEvent(streamEvent)) {
    toolCall.rawResult = `${toolCall.rawResult || ''}${delta}`
    appendToolText(toolCall, 'result', delta)
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (isToolResultEndEvent(streamEvent)) {
    toolCall.status = 'done'
    touchAuxiliaryBlock(block, 'done')
    message.activeToolBlockId = null
    return true
  }

  touchAuxiliaryBlock(block, 'running')
  return true
}

const subAgentBlockKey = (streamEvent) => {
  return normalizeSourcePath(streamEvent?.sourcePath) || getSubAgentName(streamEvent) || 'subagent'
}

const ensureSubAgentBlockIds = (message) => {
  if (!message.activeSubAgentBlockIds || typeof message.activeSubAgentBlockIds !== 'object') {
    message.activeSubAgentBlockIds = {}
  }
  return message.activeSubAgentBlockIds
}

const findRunningSubAgentBlock = (message, key, name) => {
  const blocks = ensureAuxiliaryBlocks(message)
  for (let index = blocks.length - 1; index >= 0; index -= 1) {
    const block = blocks[index]
    if (block.kind !== 'subagent' || block.status !== 'running') {
      continue
    }
    if (block.subAgent?.sourcePath === key || block.subAgent?.name === name) {
      return block
    }
  }
  return null
}

const getSubAgentBlock = (message, streamEvent, shouldCreate = false) => {
  const name = getSubAgentName(streamEvent) || '子智能体'
  const key = subAgentBlockKey(streamEvent)
  const activeIds = ensureSubAgentBlockIds(message)
  let block = findAuxiliaryBlock(message, activeIds[key])

  if (!block && !shouldCreate) {
    block = findRunningSubAgentBlock(message, key, name)
  }

  if (!block && shouldCreate) {
    block = createAuxiliaryBlock(message, 'subagent', {
      title: `子智能体 ${name}`,
      expanded: true,
      subAgentName: name,
      sourcePath: normalizeSourcePath(streamEvent?.sourcePath),
      subAgent: {
        name,
        sourcePath: normalizeSourcePath(streamEvent?.sourcePath),
        status: 'running'
      }
    })
    ensureAuxiliaryBlocks(message).push(block)
    appendAuxiliaryBlockSegment(message, block)
  }

  if (!block) {
    return null
  }

  block.subAgentName = name
  block.sourcePath = normalizeSourcePath(streamEvent?.sourcePath)
  block.subAgent = {
    ...(block.subAgent || {}),
    name,
    sourcePath: block.sourcePath,
    status: block.status || 'running'
  }
  activeIds[key] = block.id
  return block
}

const appendSubAgentEvent = (message, streamEvent) => {
  if (!message || !isSubAgentStreamEvent(streamEvent)) {
    return false
  }

  const shouldCreate = isSubAgentStartEvent(streamEvent) ||
    isSubAgentTextDeltaEvent(streamEvent) ||
    isSubAgentThinkingDeltaEvent(streamEvent)
  const shouldFinish = isSubAgentEndEvent(streamEvent)
  const shouldShowExposed = normalizedSseEvent(streamEvent) === 'subagent_exposed'
  if (!shouldCreate && !shouldFinish && !shouldShowExposed) {
    return false
  }

  const block = getSubAgentBlock(message, streamEvent, shouldCreate)
  if (!block) {
    return false
  }
  const delta = streamEvent?.delta == null ? '' : String(streamEvent.delta)

  if (shouldShowExposed) {
    const label = streamEvent?.payload?.label || getSubAgentName(streamEvent) || '子智能体'
    block.title = `子智能体 ${label}`
    appendToolLine(block, 'content', `${label} 已准备就绪`)
    touchAuxiliaryBlock(block, 'done')
    return true
  }

  if (isSubAgentTextDeltaEvent(streamEvent)) {
    block.content = `${block.content || ''}${delta}`
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (isSubAgentThinkingDeltaEvent(streamEvent)) {
    block.content = `${block.content || ''}${delta}`
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (shouldFinish) {
    touchAuxiliaryBlock(block, 'done')
    const activeIds = ensureSubAgentBlockIds(message)
    delete activeIds[subAgentBlockKey(streamEvent)]
    return true
  }

  if (isSubAgentStartEvent(streamEvent)) {
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  return false
}

const formatToolInput = (input) => {
  if (input == null || input === '') {
    return ''
  }
  if (typeof input === 'string') {
    return input
  }
  try {
    return JSON.stringify(input, null, 2)
  } catch {
    return String(input)
  }
}

const normalizeToolCallPayload = (toolCall = {}) => {
  return {
    id: toolCall.id || '',
    name: toolCall.name || '工具调用',
    input: toolCall.input || {},
    content: toolCall.content || '',
    metadata: toolCall.metadata || {},
    state: toolCall.state || ''
  }
}

const buildInterventionFromEvent = (message, streamEvent) => {
  if (!isInterventionEvent(streamEvent)) {
    return null
  }

  const payload = streamEvent.payload && typeof streamEvent.payload === 'object' ? streamEvent.payload : {}
  const toolCalls = Array.isArray(payload.toolCalls)
    ? payload.toolCalls.map(normalizeToolCallPayload)
    : []

  return {
    type: isRequireUserConfirmEvent(streamEvent) ? 'userConfirm' : 'externalExecution',
    replyId: payload.replyId || '',
    runId: streamEvent.runId || message?.runId || '',
    toolCalls
  }
}

const appendInterventionEvent = (message, streamEvent) => {
  const intervention = buildInterventionFromEvent(message, streamEvent)
  if (!message || !intervention) {
    return false
  }

  const isUserConfirm = intervention.type === 'userConfirm'
  const toolCalls = intervention.toolCalls.length ? intervention.toolCalls : [normalizeToolCallPayload()]

  toolCalls.forEach((pendingToolCall, index) => {
    const block = getToolBlock(message, index > 0, pendingToolCall.name)
    const toolCall = getToolCallFromBlock(message, block, pendingToolCall.name)
    toolCall.id = pendingToolCall.id || toolCall.id
    toolCall.name = pendingToolCall.name || toolCall.name
    toolCall.pendingToolCall = pendingToolCall
    toolCall.status = isUserConfirm ? 'waiting' : 'waiting_external'
    appendToolLine(toolCall, 'process', isUserConfirm ? '等待用户确认' : '等待外部系统执行')

    const inputText = formatToolInput(pendingToolCall.input)
    if (inputText) {
      appendToolLine(toolCall, 'process', `调用参数：${inputText}`)
    }

    touchAuxiliaryBlock(block, isUserConfirm ? 'waiting' : 'waiting_external')
  })

  message.pendingIntervention = intervention
  message.streamStatus = 'waiting'
  return true
}

const ensureStreamTiming = (message) => {
  if (!message.streamTiming) {
    message.streamTiming = {
      startedAt: 0,
      lastAt: 0,
      totalMs: 0,
      eventCount: 0
    }
  }
  if (!Array.isArray(message.eventStages)) {
    message.eventStages = []
  }
  return message.streamTiming
}

const recordStreamEvent = (message, streamEvent) => {
  if (!message || !streamEvent) {
    return
  }

  const timing = ensureStreamTiming(message)
  const now = getNowMs()
  const eventName = streamEvent.sseEvent || 'unknown'
  const stageKey = getStageKey(eventName)
  if (!stageKey) {
    return
  }

  const stageMeta = STREAM_STAGE_META[stageKey]

  if (!timing.startedAt) {
    timing.startedAt = now
    timing.lastAt = now
  }

  let stage = message.eventStages.find((item) => item.key === stageKey)
  if (!stage) {
    stage = {
      key: stageKey,
      label: stageMeta.label,
      order: stageMeta.order,
      startedAt: now,
      endedAt: null,
      durationMs: 0,
      eventCount: 0,
      lastEventType: '',
      lastEventLabel: '',
      gapMs: 0,
      status: 'running'
    }
    message.eventStages.push(stage)
    message.eventStages.sort((left, right) => left.order - right.order)
  }

  const gapMs = timing.eventCount > 0 ? now - timing.lastAt : 0
  stage.eventCount += 1
  stage.lastEventType = eventName
  stage.lastEventLabel = eventLabel(eventName)
  stage.gapMs = gapMs
  stage.durationMs = now - stage.startedAt
  stage.status = eventName === 'error' ? 'error' : isStageEndEvent(eventName) ? 'done' : 'running'

  if (stage.status !== 'running') {
    stage.endedAt = now
    stage.durationMs = stage.endedAt - stage.startedAt
  }

  timing.lastAt = now
  timing.eventCount += 1
  timing.totalMs = now - timing.startedAt
  message.streamStatus = eventName === 'error' ? 'error' : 'running'

  scheduleScrollToBottom()
}

const applyDoneUsage = (message, streamEvent) => {
  if (!message || !streamEvent) {
    return
  }

  const usageToken = Number(streamEvent.usageToken ?? streamEvent.data?.usageToken)
  const usageTime = Number(streamEvent.usageTime ?? streamEvent.data?.usageTime)

  if (Number.isFinite(usageToken) && usageToken > 0) {
    message.usageToken = usageToken
  }
  if (Number.isFinite(usageTime) && usageTime > 0) {
    message.usageTime = usageTime
  }
}

const appendAuxiliaryDelta = (message, session, streamEvent) => {
  if (!message || !streamEvent) {
    return
  }

  let changed = false

  const subAgentEvent = isSubAgentStreamEvent(streamEvent)

  if (appendSubAgentEvent(message, streamEvent)) {
    changed = true
  }

  if (!subAgentEvent) {
    if (appendReasoningEvent(message, streamEvent)) {
      changed = true
    }

    if (appendToolEvent(message, streamEvent)) {
      changed = true
    }

    if (applyPlanFallbackFromTool(message)) {
      changed = true
    }

    if (appendInterventionEvent(message, streamEvent)) {
      changed = true
    }
  }

  if (applyPlanEventPayload(message, streamEvent)) {
    changed = true
  }

  if (!changed) {
    return
  }

  if (session) {
    session.updatedAt = nowText()
  }

  scheduleScrollToBottom()
}

const finishStreamEvents = (message, status = 'done') => {
  if (!message?.streamTiming) {
    return
  }

  const now = getNowMs()
  const isWaiting = status === 'waiting'
  message.streamTiming.totalMs = now - message.streamTiming.startedAt
  message.streamStatus = status

  if (Array.isArray(message.eventStages)) {
    message.eventStages.forEach((stage) => {
      if (stage.status === 'running') {
        stage.status = status === 'error' ? 'error' : isWaiting ? 'waiting' : 'done'
        stage.endedAt = now
        stage.durationMs = stage.endedAt - stage.startedAt
      }
    })
  }

  if (Array.isArray(message.toolCalls)) {
    message.toolCalls.forEach((toolCall) => {
      if (toolCall.status === 'running') {
        toolCall.status = status === 'error' ? 'error' : isWaiting ? 'waiting' : 'done'
        toolCall.updatedAt = now
      }
    })
  }

  if (Array.isArray(message.auxiliaryBlocks)) {
    message.auxiliaryBlocks = compactAuxiliaryBlocks(message.auxiliaryBlocks)
    syncContentSegmentsWithBlocks(message)
    message.auxiliaryBlocks.forEach((block) => {
      if (block.status === 'running') {
        block.status = status === 'error' ? 'error' : isWaiting ? 'waiting' : 'done'
        block.endedAt = now
        block.durationMs = block.startedAt ? block.endedAt - block.startedAt : block.durationMs || 0
      }
      if (block.toolCall?.status === 'running') {
        block.toolCall.status = status === 'error' ? 'error' : isWaiting ? 'waiting' : 'done'
        block.toolCall.updatedAt = now
      }
    })
  }

  message.activeReasoningBlockId = null
  message.activeToolBlockId = null
  message.activeSubAgentBlockIds = {}
  message.pendingReasoningStartedAt = null

  scheduleSaveSessions()
}

const assistantMessageItems = (message) => {
  if (!message || message.role !== 'assistant') {
    return []
  }

  const blocks = Array.isArray(message.auxiliaryBlocks) ? message.auxiliaryBlocks : []
  const blocksById = new Map(blocks.map((block) => [block.id, block]))
  const segments = Array.isArray(message.contentSegments) && message.contentSegments.length
    ? message.contentSegments
    : normalizeContentSegments(message, blocks)
  const referencedBlocks = new Set()
  const items = []

  segments.forEach((segment, index) => {
    if (segment?.type === 'content') {
      const content = segment.content == null ? '' : String(segment.content)
      if (content) {
        items.push({
          id: segment.id || `content-${index}`,
          type: 'content',
          content
        })
      }
      return
    }

    const block = blocksById.get(segment?.blockId)
    if (block) {
      referencedBlocks.add(block.id)
      items.push({
        id: segment.id || `auxiliary-${block.id}`,
        type: 'auxiliary',
        block
      })
    }
  })

  blocks.forEach((block) => {
    if (block?.id && !referencedBlocks.has(block.id)) {
      items.push({
        id: `auxiliary-${block.id}`,
        type: 'auxiliary',
        block
      })
    }
  })

  if (!items.length && shouldShowMessageContent(message)) {
    items.push({
      id: `content-${message.id || 'pending'}`,
      type: 'content',
      content: messageDisplayText(message)
    })
  }

  return items
}

const messageText = (message) => {
  return message.content ?? message.displayContent ?? ''
}

const messageDisplayText = (message) => {
  return messageText(message) || (message.pending ? '思考中...' : '')
}

const shouldShowMessageContent = (message) => {
  return Boolean(messageText(message) || (message.pending && !hasAuxiliaryOutput(message)))
}

const renderAssistantMarkdown = (message) => {
  return markdownRenderer.render(messageDisplayText(message))
}

const renderMarkdownText = (text = '') => {
  return markdownRenderer.render(String(text || ''))
}

const nowText = () => {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const randomSnowflakePart = () => {
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    const array = new Uint32Array(1)
    crypto.getRandomValues(array)
    return BigInt(array[0] % 32)
  }
  return BigInt(Math.floor(Math.random() * 32))
}

const getStoredSnowflakePart = (key, maxValue) => {
  const storedValue = localStorage.getItem(key)
  if (/^\d+$/.test(storedValue || '')) {
    const parsedValue = BigInt(storedValue)
    if (parsedValue >= 0n && parsedValue <= maxValue) {
      return parsedValue
    }
  }

  const value = randomSnowflakePart()
  localStorage.setItem(key, value.toString())
  return value
}

const waitNextSnowflakeMillis = (lastTimestamp) => {
  let timestamp = BigInt(Date.now())
  while (timestamp <= lastTimestamp) {
    timestamp = BigInt(Date.now())
  }
  return timestamp
}

const createSnowflakeId = () => {
  if (snowflakeWorkerId == null) {
    snowflakeWorkerId = getStoredSnowflakePart(SNOWFLAKE_WORKER_STORAGE_KEY, SNOWFLAKE_MAX_WORKER_ID)
  }
  if (snowflakeDatacenterId == null) {
    snowflakeDatacenterId = getStoredSnowflakePart(SNOWFLAKE_DATACENTER_STORAGE_KEY, SNOWFLAKE_MAX_DATACENTER_ID)
  }

  let timestamp = BigInt(Date.now())
  if (timestamp < snowflakeLastTimestamp) {
    timestamp = snowflakeLastTimestamp
  }

  if (timestamp === snowflakeLastTimestamp) {
    snowflakeSequence = (snowflakeSequence + 1n) & SNOWFLAKE_SEQUENCE_MASK
    if (snowflakeSequence === 0n) {
      timestamp = waitNextSnowflakeMillis(snowflakeLastTimestamp)
    }
  } else {
    snowflakeSequence = 0n
  }

  snowflakeLastTimestamp = timestamp

  return (((timestamp - SNOWFLAKE_EPOCH) << SNOWFLAKE_TIMESTAMP_SHIFT) |
    (snowflakeDatacenterId << SNOWFLAKE_DATACENTER_ID_SHIFT) |
    (snowflakeWorkerId << SNOWFLAKE_WORKER_ID_SHIFT) |
    snowflakeSequence).toString()
}

const createSessionId = () => {
  return createSnowflakeId()
}

const getBackendSessionId = (session) => {
  const existingId = String(session?.backendSessionId ?? session?.id ?? '')
  if (SNOWFLAKE_LONG_ID_PATTERN.test(existingId)) {
    return existingId
  }

  const sessionId = createSnowflakeId()
  if (session) {
    session.backendSessionId = sessionId
  }
  return sessionId
}

const getDefaultTitle = () => {
  return `${agentInfo.name || '智能体'}会话`
}

const saveSessions = () => {
  localStorage.setItem(storageKey.value, JSON.stringify(sessions.value))
}

const scheduleSaveSessions = () => {
  if (saveTimer) {
    return
  }

  // 打字过程中只低频持久化，避免 localStorage 同步写入拖慢渲染。
  saveTimer = setTimeout(() => {
    saveTimer = null
    saveSessions()
  }, 1000)
}

const flushScheduledSave = () => {
  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }

  saveSessions()
}

const isNearBottom = (container) => {
  return container.scrollHeight - container.scrollTop - container.clientHeight <= SCROLL_BOTTOM_THRESHOLD
}

const handleMessageScroll = () => {
  const container = messageListRef.value
  if (container) {
    autoScrollEnabled.value = isNearBottom(container)
  }
}

const scrollToBottom = async (force = false) => {
  await nextTick()
  const container = messageListRef.value
  if (container && (force || autoScrollEnabled.value)) {
    container.scrollTop = container.scrollHeight
    autoScrollEnabled.value = true
  }
}

const scheduleScrollToBottom = (force = false) => {
  pendingScrollForce = pendingScrollForce || force

  if (scrollFrame) {
    return
  }

  // 仅在用户停留在底部附近时跟随输出；用户上滑阅读历史后不抢滚动位置。
  scrollFrame = window.requestAnimationFrame(() => {
    scrollFrame = null
    const forceScroll = pendingScrollForce
    pendingScrollForce = false
    const container = messageListRef.value
    if (container && (forceScroll || autoScrollEnabled.value)) {
      container.scrollTop = container.scrollHeight
      autoScrollEnabled.value = true
    }
  })
}

const appendStreamText = (message, session, delta) => {
  if (!delta) {
    return
  }

  message.content = `${message.content || ''}${delta}`
  message.displayContent = message.content
  appendContentSegment(message, delta)
  message.pending = false
  message.typing = true
  if (session) {
    session.updatedAt = nowText()
  }
  scheduleScrollToBottom()
}

const flushStreamText = () => {
  flushScheduledSave()
  scheduleScrollToBottom()
}

const applyRouteAgentInfo = () => {
  agentInfo.id = agentId.value
  agentInfo.name = route.query.agentName || `智能体 #${route.params.agentId}`
  agentInfo.key = route.query.agentKey || ''
}

const createSession = () => {
  const sessionId = createSessionId()
  const session = {
    id: sessionId,
    backendSessionId: sessionId,
    title: getDefaultTitle(),
    updatedAt: nowText(),
    messages: []
  }

  sessions.value.unshift(session)
  activeSessionId.value = session.id
  autoScrollEnabled.value = true
  saveSessions()
  scrollToBottom(true)
  return session
}

const loadSessions = () => {
  let storedSessions = []

  try {
    storedSessions = JSON.parse(localStorage.getItem(storageKey.value) || '[]')
  } catch {
    storedSessions = []
  }

  // 兼容旧会话数据：历史消息可能没有 displayContent 字段。
  sessions.value = Array.isArray(storedSessions)
    ? storedSessions.map((session) => ({
      ...session,
      messages: Array.isArray(session.messages)
        ? session.messages.map((message) => {
          const toolCalls = normalizeStoredToolCalls(message)
          const auxiliaryBlocks = normalizeStoredAuxiliaryBlocks(message, toolCalls)
          const planTasks = Array.isArray(message.planTasks)
            ? message.planTasks.map(normalizePlanTask)
            : []
          return {
            ...message,
            displayContent: message.displayContent ?? message.content ?? '',
            contentSegments: normalizeContentSegments(message, auxiliaryBlocks),
            usageToken: Number(message.usageToken) || 0,
            usageTime: Number(message.usageTime) || 0,
            thinkingContent: message.thinkingContent ?? '',
            thinkingExpanded: message.thinkingExpanded ?? Boolean(message.thinkingContent),
            toolResultContent: message.toolResultContent ?? '',
            toolResultExpanded: message.toolResultExpanded ?? Boolean(message.toolResultContent),
            toolCalls,
            auxiliaryBlocks,
            activeReasoningBlockId: null,
            activeToolBlockId: null,
            activeSubAgentBlockIds: {},
            pendingReasoningStartedAt: null,
            pendingIntervention: null,
            toolExpanded: message.toolExpanded ?? message.toolResultExpanded ?? Boolean(toolCalls.length),
            eventStages: Array.isArray(message.eventStages) ? message.eventStages : [],
            planState: message.planState || null,
            planTasks,
            planProgress: normalizePlanProgress(message.planProgress, planTasks),
            planExpanded: message.planExpanded ?? Boolean(message.planState || planTasks.length),
            lastPlanEvent: message.lastPlanEvent || null
          }
        })
        : []
    }))
    : []

  if (!sessions.value.length) {
    createSession()
    return
  }

  activeSessionId.value = sessions.value[0].id
  autoScrollEnabled.value = true
  scrollToBottom(true)
}

const selectSession = (session) => {
  activeSessionId.value = session.id
  autoScrollEnabled.value = true
  scrollToBottom(true)
}

const removeSession = async (session) => {
  if (sessions.value.length <= 1) {
    ElMessage.warning('至少保留一个会话')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除会话「${session.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const index = sessions.value.findIndex((item) => item.id === session.id)
  sessions.value.splice(index, 1)

  if (activeSessionId.value === session.id) {
    activeSessionId.value = sessions.value[Math.max(index - 1, 0)]?.id || sessions.value[0]?.id
  }

  saveSessions()
}

const clearSessions = async () => {
  if (!sessions.value.length) {
    return
  }

  try {
    await ElMessageBox.confirm('确认清空所有历史对话吗？', '清空确认', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  sessions.value = []
  sessionKeyword.value = ''
  createSession()
}

const appendMessage = (role, content, extra = {}, options = {}) => {
  const session = activeSession.value || createSession()
  const message = {
    id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    role,
    content,
    displayContent: content,
    contentSegments: content ? [{
      id: createContentSegmentId(),
      type: 'content',
      content
    }] : [],
    createdAt: nowText(),
    ...extra
  }

  session.messages.push(message)
  session.updatedAt = nowText()

  if (role === 'user' && session.messages.length <= 2) {
    session.title = content.slice(0, 18) || getDefaultTitle()
  }

  saveSessions()
  scrollToBottom(Boolean(options.forceScroll))
  return session.messages[session.messages.length - 1]
}

const stopStream = () => {
  abortController.value?.abort()
  abortController.value = null
  streaming.value = false
}

const formatInterventionToolCalls = (toolCalls = []) => {
  const calls = toolCalls.length ? toolCalls : [normalizeToolCallPayload()]
  return `
    <div style="display:grid;gap:12px;min-width:260px;max-width:min(560px, calc(100vw - 96px));">
      ${calls.map((toolCall, index) => formatInterventionToolCall(toolCall, index)).join('')}
    </div>
  `
}

const formatInterventionToolCall = (toolCall = {}, index = 0) => {
  const name = escapeHtml(toolCall.name || '工具调用')
  const params = toolInputEntries(toolCall.input)
  const titlePrefix = index > 0 ? `${index + 1}. ` : ''
  const paramHtml = params.length
    ? params.map(([key, value]) => `
        <div style="display:grid;grid-template-columns:max-content minmax(0, 1fr);gap:8px;align-items:start;padding:6px 0;border-top:1px solid #eef2f7;">
          <span style="color:#64748b;font-weight:700;white-space:nowrap;">${escapeHtml(key)}参数：</span>
          <span style="min-width:0;color:#1f2937;line-height:1.55;white-space:pre-wrap;overflow-wrap:anywhere;">${escapeHtml(formatToolParamValue(value))}</span>
        </div>
      `).join('')
    : '<div style="padding-top:6px;color:#94a3b8;">无参数</div>'

  return `
    <section style="display:grid;gap:8px;padding:2px 0;">
      <div style="display:flex;align-items:center;gap:8px;color:#111827;font-weight:800;">
        <span style="color:#2563eb;">工具名称：</span>
        <span style="overflow-wrap:anywhere;">${titlePrefix}${name}</span>
      </div>
      <div style="display:grid;gap:0;border:1px solid #e5eaf3;border-radius:8px;padding:2px 10px;background:#f8fafc;">
        ${paramHtml}
      </div>
    </section>
  `
}

const toolInputEntries = (input) => {
  if (input == null || input === '') {
    return []
  }
  if (typeof input === 'object' && !Array.isArray(input)) {
    return Object.entries(input)
  }
  return [['输入', input]]
}

const formatToolParamValue = (value) => {
  if (value == null) {
    return ''
  }
  if (typeof value === 'string') {
    return value
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

const escapeHtml = (value = '') => {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const markInterventionDecision = (message, intervention, confirmed) => {
  const pendingIds = new Set((intervention?.toolCalls || []).map((toolCall) => toolCall.id).filter(Boolean))
  const decisionText = confirmed ? '用户已确认，继续执行工具' : '用户已拒绝，停止执行工具'

  ;(message.toolCalls || []).forEach((toolCall) => {
    if (!pendingIds.size || pendingIds.has(toolCall.id)) {
      appendToolLine(toolCall, 'process', decisionText)
      toolCall.status = 'running'
      toolCall.updatedAt = getNowMs()
    }
  })

  ;(message.auxiliaryBlocks || []).forEach((block) => {
    if (block.toolCall && (!pendingIds.size || pendingIds.has(block.toolCall.id))) {
      block.status = 'running'
      block.toolCall.status = 'running'
      touchAuxiliaryBlock(block, 'running')
    }
  })

  message.pendingIntervention = null
  message.streamStatus = 'running'
  saveSessions()
}

const runAssistantStream = async (streamer, data, assistantMessage, session) => {
  let pendingIntervention = null
  abortController.value = new AbortController()

  await streamer(
    data,
    {
      onEvent: (streamEvent) => {
        if (streamEvent.runId) {
          assistantMessage.runId = streamEvent.runId
        }
        recordStreamEvent(assistantMessage, streamEvent)
        appendAuxiliaryDelta(assistantMessage, session, streamEvent)
        if (assistantMessage.pendingIntervention) {
          pendingIntervention = assistantMessage.pendingIntervention
        }
      },
      onMessage: (delta) => {
        appendStreamText(assistantMessage, session, delta)
      },
      onError: (message) => {
        finishStreamEvents(assistantMessage, 'error')
        assistantMessage.content = message
        assistantMessage.displayContent = message
        replaceContentSegments(assistantMessage, message)
        assistantMessage.pending = false
        assistantMessage.typing = false
        assistantMessage.error = true
        assistantMessage.pendingIntervention = null
        session.updatedAt = nowText()
        saveSessions()
        scrollToBottom()
      },
      onDone: (streamEvent) => {
        applyDoneUsage(assistantMessage, streamEvent)
        finishStreamEvents(assistantMessage, assistantMessage.pendingIntervention ? 'waiting' : 'done')
        session.updatedAt = nowText()
      }
    },
    {
      signal: abortController.value.signal
    }
  )

  assistantMessage.pending = false
  assistantMessage.typing = false
  assistantMessage.displayContent = assistantMessage.content
  session.updatedAt = nowText()
  flushScheduledSave()

  return pendingIntervention
}

const handleUserConfirmIntervention = async (assistantMessage, session, backendSessionId, intervention) => {
  const detail = formatInterventionToolCalls(intervention.toolCalls)
  let confirmed = false

  try {
    await ElMessageBox.confirm(
      detail || '智能体请求执行工具，是否允许？',
      '工具调用确认',
      {
        type: 'warning',
        confirmButtonText: '允许执行',
        cancelButtonText: '拒绝执行',
        dangerouslyUseHTMLString: true,
        distinguishCancelAndClose: false
      }
    )
    confirmed = true
  } catch {
    confirmed = false
  }

  markInterventionDecision(assistantMessage, intervention, confirmed)

  return runAssistantStream(
    userConfirmStream,
    {
      agentId: agentId.value,
      sessionId: backendSessionId,
      runId: intervention.runId || assistantMessage.runId,
      replyId: intervention.replyId,
      confirmResults: intervention.toolCalls.map((toolCall) => ({
        confirmed,
        toolCall
      }))
    },
    assistantMessage,
    session
  )
}

const handleExternalExecutionIntervention = async (assistantMessage, intervention) => {
  assistantMessage.pendingIntervention = intervention
  assistantMessage.streamStatus = 'waiting'
  ElMessage.warning('智能体正在等待外部系统回传工具执行结果')
  saveSessions()
  return null
}

const handlePendingInterventions = async (assistantMessage, session, backendSessionId, intervention) => {
  let currentIntervention = intervention

  while (currentIntervention) {
    if (currentIntervention.type === 'userConfirm') {
      currentIntervention = await handleUserConfirmIntervention(
        assistantMessage,
        session,
        backendSessionId,
        currentIntervention
      )
      continue
    }

    if (currentIntervention.type === 'externalExecution') {
      currentIntervention = await handleExternalExecutionIntervention(assistantMessage, currentIntervention)
      continue
    }

    currentIntervention = null
  }
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content) {
    return
  }
  if (!agentId.value) {
    ElMessage.error('智能体ID无效')
    return
  }

  inputMessage.value = ''
  autoScrollEnabled.value = true
  appendMessage('user', content, {}, { forceScroll: true })
  const assistantMessage = appendMessage(
    'assistant',
    '',
    {
      pending: true,
      typing: true,
      eventStages: [],
      streamTiming: null,
      streamStatus: 'running',
      usageToken: 0,
      usageTime: 0,
      thinkingContent: '',
      thinkingExpanded: true,
      toolResultContent: '',
      toolResultExpanded: true,
      toolCalls: [],
      auxiliaryBlocks: [],
      activeReasoningBlockId: null,
      activeToolBlockId: null,
      activeSubAgentBlockIds: {},
      pendingReasoningStartedAt: null,
      toolExpanded: true,
      pendingIntervention: null,
      planState: null,
      planTasks: [],
      planProgress: buildPlanProgress([]),
      planExpanded: true,
      lastPlanEvent: null,
      runId: null
    },
    { forceScroll: true }
  )
  const session = activeSession.value
  const backendSessionId = getBackendSessionId(session)
  saveSessions()

  streaming.value = true
  abortController.value = new AbortController()

  try {
    const pendingIntervention = await runAssistantStream(
      chatStream,
      {
        agentId: agentId.value,
        sessionId: backendSessionId,
        content
      },
      assistantMessage,
      session
    )
    await handlePendingInterventions(assistantMessage, session, backendSessionId, pendingIntervention)
  } catch (error) {
    flushStreamText()
    finishStreamEvents(assistantMessage, error.name === 'AbortError' ? 'done' : 'error')
    if (error.name === 'AbortError') {
      if (!assistantMessage.content) {
        assistantMessage.content = '已停止生成'
        assistantMessage.displayContent = assistantMessage.content
        appendContentSegment(assistantMessage, assistantMessage.content)
      } else {
        assistantMessage.displayContent = assistantMessage.displayContent || assistantMessage.content
      }
    } else {
      assistantMessage.content = error.message || '对话请求失败'
      assistantMessage.displayContent = assistantMessage.content
      replaceContentSegments(assistantMessage, assistantMessage.content)
      assistantMessage.error = true
    }
    assistantMessage.pending = false
    assistantMessage.typing = false
    flushScheduledSave()
  } finally {
    streaming.value = false
    abortController.value = null
    scrollToBottom()
  }
}

watch(
  () => route.params.agentId,
  () => {
    stopStream()
    planDrawerOpen.value = false
    applyRouteAgentInfo()
    loadSessions()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  stopStream()

  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }

  if (scrollFrame) {
    window.cancelAnimationFrame(scrollFrame)
    scrollFrame = null
  }
  pendingScrollForce = false
})
</script>

<template>
  <section
    class="agent-chat-page"
    :class="{ 'is-plan-drawer-open': planDrawerOpen }"
  >
    <aside class="chat-session-panel">
      <el-button
        class="new-session-button"
        type="primary"
        plain
        :icon="Plus"
        @click="createSession"
      >
        新建对话
      </el-button>

      <el-input
        v-model="sessionKeyword"
        class="session-search"
        :prefix-icon="Search"
        clearable
        placeholder="搜索历史对话"
      />

      <div class="session-list">
        <div class="session-group-title">
          {{ sessionKeyword ? '搜索结果' : '今天' }}
        </div>
        <button
          v-for="session in filteredSessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === activeSessionId }"
          type="button"
          @click="selectSession(session)"
        >
          <el-icon class="session-chat-icon"><ChatLineRound /></el-icon>
          <span class="session-main">
            <strong>{{ session.title }}</strong>
            <small>{{ session.updatedAt }}</small>
          </span>
          <el-tooltip content="删除会话" placement="right">
            <el-button
              class="session-delete"
              :icon="MoreFilled"
              link
              @click.stop="removeSession(session)"
            />
          </el-tooltip>
        </button>
        <div
          v-if="!filteredSessions.length"
          class="session-empty"
        >
          未找到相关对话
        </div>
      </div>

      <button
        class="clear-session-button"
        type="button"
        @click="clearSessions"
      >
        <el-icon><Delete /></el-icon>
        <span>清空历史记录</span>
      </button>
    </aside>

    <section class="chat-window">
      <header class="chat-header">
        <div class="chat-agent-title">
          <h2>{{ agentInfo.name }}</h2>
          <el-tag class="agent-state-tag" :type="streaming ? 'warning' : 'success'">
            {{ streaming ? '生成中' : '就绪' }}
          </el-tag>
        </div>
        <div class="chat-header-actions">
          <button
            class="plan-drawer-toggle"
            type="button"
            :aria-expanded="planDrawerOpen"
            @click="togglePlanDrawer"
          >
            <span>{{ planDrawerOpen ? '收起工作区 >' : '< 工作区' }}</span>
          </button>
        </div>
      </header>

      <div
        ref="messageListRef"
        class="message-list"
        @scroll="handleMessageScroll"
      >
        <div
          v-for="message in activeMessages"
          :key="message.id"
          class="message-row"
          :class="[message.role, { error: message.error, typing: message.typing }]"
        >
          <div class="message-bubble">
            <template v-if="message.role === 'assistant'">
              <template
                v-for="item in assistantMessageItems(message)"
                :key="item.id"
              >
                <div
                  v-if="item.type === 'content'"
                  class="message-content markdown-content assistant-content-segment"
                >
                  <div
                    class="markdown-rendered"
                    v-html="renderMarkdownText(item.content)"
                  />
                </div>
                <section
                  v-else-if="item.block"
                  class="auxiliary-panel"
                >
                <button
                  class="auxiliary-toggle"
                  type="button"
                  @click="toggleAuxiliaryPanel(message, item.block)"
                >
                  <span class="auxiliary-title">{{ auxiliaryBlockTitle(item.block) }}</span>
                  <small class="auxiliary-action">{{ item.block.expanded ? '收起' : '展开' }}</small>
                  <small v-if="auxiliaryBlockTimeText(item.block)" class="auxiliary-time">耗时 {{ auxiliaryBlockTimeText(item.block) }}</small>
                </button>
                <div
                  v-show="item.block.expanded"
                  class="auxiliary-content"
                >
                  <div v-if="item.block.kind === 'reasoning'">
                    {{ item.block.content }}
                  </div>
                  <div
                    v-else-if="item.block.kind === 'tool'"
                    class="tool-call"
                  >
                    <div class="tool-call-header">
                      <span class="tool-call-name">{{ item.block.toolCall?.name || '工具调用' }}</span>
                      <small>{{ toolStatusText(item.block.toolCall || item.block) }}</small>
                    </div>
                    <div
                      v-if="item.block.toolCall?.process"
                      class="tool-call-section"
                    >
                      <span class="tool-call-section-title">调用过程</span>
                      <div class="tool-call-text">{{ item.block.toolCall.process }}</div>
                    </div>
                    <div
                      v-if="item.block.toolCall?.result"
                      class="tool-call-section"
                    >
                      <span class="tool-call-section-title">调用结果</span>
                      <div class="tool-call-text">{{ item.block.toolCall.result }}</div>
                    </div>
                  </div>
                  <div
                    v-else-if="item.block.kind === 'subagent'"
                    class="subagent-call"
                  >
                    <div class="subagent-call-header">
                      <span class="subagent-call-name">{{ item.block.subAgent?.name || item.block.subAgentName || '子智能体' }}</span>
                      <small>{{ item.block.status === 'done' ? '完成' : '输出中' }}</small>
                    </div>
                    <div
                      v-if="item.block.content"
                      class="subagent-call-content markdown-rendered"
                      v-html="renderMarkdownText(item.block.content)"
                    />
                  </div>
                </div>
              </section>
              </template>
            </template>
            <div
              v-else-if="shouldShowMessageContent(message)"
              class="message-content"
            >
              {{ messageDisplayText(message) }}
            </div>
            <div
              v-if="message.role === 'assistant' && hasStreamSummary(message)"
              class="stream-stages"
            >
              <div class="stream-stages-header">
                <span v-if="streamUsageTokenText(message)">耗时统计</span>
                <span v-if="streamUsageTokenText(message)">Token消耗： {{ streamUsageTokenText(message) }}</span>
                <span v-if="streamUsageTimeText(message)">模型耗时 {{ streamUsageTimeText(message) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!activeMessages.length" class="empty-chat">
          暂无消息
        </div>
      </div>

      <el-button
        v-if="!autoScrollEnabled"
        class="back-bottom-button"
        :icon="Bottom"
        circle
        type="primary"
        @click="scrollToBottom(true)"
      />

      <footer class="chat-input-panel">
        <div class="chat-input-shortcuts">
          <el-button size="small">使用工具</el-button>
        </div>
        <el-input
          v-model="inputMessage"
          class="chat-input"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 3 }"
          resize="none"
          :placeholder="`给${agentInfo.name || '智能体'}发送消息，输入 / 使用快捷指令...`"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="chat-actions">
          <el-button
            v-if="streaming"
            :icon="Close"
            @click="stopStream"
          >
            停止
          </el-button>
          <el-button
            type="primary"
            :icon="Promotion"
            :disabled="!canSend"
            @click="sendMessage"
          >
            发送
          </el-button>
        </div>
      </footer>
    </section>

    <aside
      class="execution-plan-panel"
      :class="{ open: planDrawerOpen }"
      :aria-hidden="!planDrawerOpen"
    >
      <header class="execution-plan-header">
        <div class="execution-plan-title">
          <h3>执行计划</h3>
          <p v-if="activePlanMessage?.planState?.title || activePlanMessage?.planState?.planNo">
            {{ planPanelTitle(activePlanMessage) }}
          </p>
        </div>
        <div class="execution-plan-actions">
          <el-tag
            size="small"
            :type="planStatusType(activePlanMessage?.planState?.status)"
          >
            {{ planStatusText(activePlanMessage?.planState?.status) }}
          </el-tag>
        </div>
      </header>

      <div class="execution-plan-meta">
        <span v-if="executionPlanEventText">{{ executionPlanEventText }}</span>
        <span v-else>本次任务</span>
        <span v-if="executionPlanDurationText">用时 {{ executionPlanDurationText }}</span>
        <span v-if="planProgressText(activePlanMessage)">进度 {{ planProgressText(activePlanMessage) }}</span>
      </div>

      <div
        v-if="executionPlanProgress.total"
        class="execution-plan-progress"
      >
        <div class="execution-progress-bar">
          <span :style="{ width: `${executionPlanProgress.percent || 0}%` }" />
        </div>
        <small>{{ executionPlanProgress.completed }}/{{ executionPlanProgress.total }} 完成</small>
      </div>

      <ol
        v-if="activePlanMessage?.planTasks?.length"
        class="execution-task-list"
      >
        <li
          v-for="(task, taskIndex) in activePlanMessage.planTasks"
          :key="task.id"
          class="execution-task-item"
          :class="executionTaskStateClass(task.state)"
        >
          <div class="execution-task-rail">
            <span class="execution-task-index">{{ task.taskIndex || taskIndex + 1 }}</span>
          </div>
          <div class="execution-task-content">
            <div class="execution-task-head">
              <strong>{{ task.subject }}</strong>
              <span class="execution-task-state">{{ taskStateText(task.state) }}</span>
            </div>
            <span
              v-if="executionTaskToolLabel(task)"
              class="execution-task-tool"
            >
              {{ executionTaskToolLabel(task) }}
            </span>
            <p v-if="task.detail">{{ task.detail }}</p>
          </div>
        </li>
      </ol>
      <div
        v-else
        class="execution-task-empty"
      >
        <strong>{{ activePlanMessage ? '计划生成中' : '暂无执行计划' }}</strong>
        <span>{{ activePlanMessage?.lastPlanEvent?.occurredAt || (activePlanMessage ? '正在等待任务列表' : 'Agent 生成计划后会在这里更新') }}</span>
      </div>
    </aside>
  </section>
</template>

<style scoped>
.app-main{
  padding: 5px;
}
.agent-chat-page {
  display: grid;
  position: relative;
  height: calc(100vh - 80px);
  min-height: 560px;
  grid-template-columns: 230px minmax(0, 1fr) 0;
  padding: 5px;
  margin: -30px;  /* 反向偏移，抵消父元素的 padding */
  gap: 5px;
  overflow: hidden;
  transition: grid-template-columns 180ms ease;
}

.agent-chat-page.is-plan-drawer-open {
  grid-template-columns: 230px minmax(0, 1fr) 230px;
}

.chat-session-panel,
.chat-window,
.execution-plan-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.chat-session-panel {
  display: flex;
  flex-direction: column;
  padding: 20px 12px 14px;
}

.session-panel-header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 22px;
}

.session-panel-header h2 {
  margin: 0;
  color: #0f1f3a;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0;
}

.new-session-button {
  width: 100%;
  height: 35px;
  margin-bottom: 14px;
  border-radius: 8px;
  font-weight: 760;
}

.session-search {
  margin-bottom: 20px;
}

.session-search :deep(.el-input__wrapper) {
  min-height: 35px;
  border-radius: 8px;
}

.session-group-title {
  margin: 0 0 10px;
  color: #74839b;
  font-size: 13px;
  font-weight: 760;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 0 12px;
}

.session-item {
  display: grid;
  width: 100%;
  min-height: 52px;
  align-items: center;
  grid-template-columns: 24px minmax(0, 1fr) 28px;
  gap: 10px;
  margin: 0 0 8px;
  padding: 10px 8px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  background: transparent;
  text-align: left;
}

.session-item:hover {
  background: #f3f7ff;
}

.session-item.active {
  border-color: #d8e6ff;
  background: #eaf2ff;
}

.session-chat-icon {
  color: #1d6ff2;
  font-size: 18px;
}

.session-main {
  min-width: 0;
}

.session-main strong,
.session-main small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-main strong {
  color: #15243d;
  font-size: 14px;
  font-weight: 760;
}

.session-main small {
  margin-top: 5px;
  color: #7d8aa0;
  font-size: 12px;
}

.session-delete {
  flex: 0 0 auto;
  color: #5f6f89;
}

.session-empty {
  display: grid;
  min-height: 80px;
  place-items: center;
  color: #94a3b8;
  font-size: 13px;
}

.clear-session-button {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  gap: 8px;
  border: 0;
  cursor: pointer;
  background: transparent;
  color: #1d6ff2;
  font: inherit;
  font-size: 14px;
  font-weight: 760;
}

.clear-session-button:hover {
  color: #1257c8;
}

.chat-window {
  display: flex;
  position: relative;
  flex-direction: column;
}

.chat-header {
  display: flex;
  height: 40px;
  min-height: 40px;
  flex: 0 0 40px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  box-sizing: border-box;
  padding: 0 20px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(180deg, rgba(247, 251, 255, 0.96), rgba(255, 255, 255, 0.9));
}

.chat-header-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
}

.chat-agent-title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 20px;
}

.chat-agent-title h2 {
  margin: 0;
  color: #0f1f3a;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 22px;
}

.agent-state-tag {
  flex: 0 0 auto;
  height: 24px;
  margin: 0;
  border-radius: 6px;
  font-weight: 700;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 28px 34px 22px;
  background: linear-gradient(180deg, #f7fbff 0%, #ffffff 38%, #f8fbff 100%);
}

.back-bottom-button {
  border: 1px solid #a0cfff;
  background: #a0cfff;
  position: absolute;
  right: 24px;
  bottom: 112px;
  z-index: 3;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.18);
}

.message-row {
  display: flex;
  margin-bottom: 18px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

.message-row.user + .message-row.assistant,
.message-row.assistant + .message-row.user {
  padding-top: 26px;
}

.message-bubble {
  max-width: min(860px, 88%);
  border: 0;
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.message-row.assistant .message-bubble {
  display: grid;
  gap: 8px;
  max-width: min(920px, 100%);
}

.message-row.user .message-bubble {
  max-width: min(680px, 76%);
  color: #17233c;
}

.message-row.error .message-bubble {
  color: var(--danger);
}

.message-meta {
  display: none;
}

.message-content {
  margin: 40px 0px 40px 0px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.message-row.user .message-content {
  padding: 12px 18px;
  border: 1px solid #e5eaf3;
  border-radius: 20px;
  background: #f3f3f3;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.message-row.assistant .message-content {
  color: #17233c;
  font-size: 15px;
}

.assistant-content-segment {
  min-width: 0;
}

.message-content.markdown-content {
  white-space: normal;
}

.markdown-rendered :deep(> :first-child) {
  margin-top: 0;
}

.markdown-rendered :deep(> :last-child) {
  margin-bottom: 0;
}

.markdown-rendered :deep(p) {
  margin: 0 0 10px;
}

.markdown-rendered :deep(h1),
.markdown-rendered :deep(h2),
.markdown-rendered :deep(h3),
.markdown-rendered :deep(h4),
.markdown-rendered :deep(h5),
.markdown-rendered :deep(h6) {
  margin: 14px 0 8px;
  color: var(--ink);
  font-weight: 760;
  line-height: 1.35;
}

.markdown-rendered :deep(h1) {
  font-size: 22px;
}

.markdown-rendered :deep(h2) {
  font-size: 19px;
}

.markdown-rendered :deep(h3) {
  font-size: 17px;
}

.markdown-rendered :deep(ul),
.markdown-rendered :deep(ol) {
  margin: 8px 0 10px;
  padding-left: 22px;
}

.markdown-rendered :deep(li + li) {
  margin-top: 4px;
}

.markdown-rendered :deep(blockquote) {
  margin: 10px 0;
  padding-left: 12px;
  border-left: 3px solid var(--border-strong);
  color: var(--muted);
}

.markdown-rendered :deep(a) {
  color: var(--primary);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.markdown-rendered :deep(code) {
  border-radius: 4px;
  padding: 2px 5px;
  background: rgba(15, 23, 42, 0.08);
  color: #0f172a;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 0.92em;
}

.markdown-rendered :deep(pre) {
  overflow-x: auto;
  margin: 10px 0;
  padding: 12px;
  border-radius: 8px;
  background: #0f172a;
  color: #e5e7eb;
  line-height: 1.6;
}

.markdown-rendered :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
  font-size: 13px;
}

.markdown-rendered :deep(table) {
  display: block;
  width: 100%;
  overflow-x: auto;
  margin: 10px 0;
  border-collapse: collapse;
}

.markdown-rendered :deep(th),
.markdown-rendered :deep(td) {
  padding: 7px 9px;
  border: 1px solid var(--border);
  text-align: left;
}

.markdown-rendered :deep(th) {
  background: var(--surface-muted);
  color: var(--ink);
  font-weight: 700;
}

.auxiliary-output {
  display: grid;
  gap: 6px;
  margin-bottom: 8px;
}

.auxiliary-panel {
  min-width: 0;
  margin: 5px 0px 5px 0px;
}

.auxiliary-toggle {
  display: inline-flex;
  width: auto;
  max-width: 100%;
  min-height: 22px;
  align-items: center;
  padding: 0;
  border: 0;
  cursor: pointer;
  background: transparent;
  color: #64748b;
  //font: inherit;
  text-align: left;
}

.auxiliary-toggle:hover {
  color: #2563eb;
}

.auxiliary-title {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auxiliary-toggle small {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.auxiliary-action {
  margin-left: 20px;
}

.auxiliary-time {
  margin-left: 12px;
}

.auxiliary-content {
  margin-top: 20px;
  margin-left: 20px;
  padding: 2px 0 4px;
  color: #8b9bb0;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.plan-progress-panel {
  overflow: hidden;
  margin-bottom: 10px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  background: #ffffff;
}

.plan-progress-toggle {
  display: grid;
  width: 100%;
  min-height: 38px;
  align-items: center;
  grid-template-columns: minmax(0, 1fr) max-content max-content max-content;
  gap: 8px;
  border: 0;
  cursor: pointer;
  background: rgba(239, 246, 255, 0.74);
  color: #334155;
  font: inherit;
  text-align: left;
}

.plan-progress-toggle:hover {
  background: rgba(219, 234, 254, 0.78);
}

.plan-progress-title {
  overflow: hidden;
  font-size: 13px;
  font-weight: 760;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-progress-toggle small {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.plan-progress-body {
  display: grid;
  gap: 10px;
  padding: 10px;
}

.plan-progress-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: #64748b;
  font-size: 12px;
}

.plan-progress-bar {
  overflow: hidden;
  height: 7px;
  border-radius: 999px;
  background: #e2e8f0;
}

.plan-progress-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #2563eb;
  transition: width 180ms ease;
}

.plan-task-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.plan-task-item {
  display: grid;
  align-items: start;
  grid-template-columns: minmax(0, 1fr) max-content;
  gap: 10px;
  padding: 8px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: #f8fafc;
}

.plan-task-item.in_progress {
  border-color: rgba(245, 158, 11, 0.36);
  background: #fffbeb;
}

.plan-task-item.completed {
  border-color: rgba(34, 197, 94, 0.28);
  background: #f0fdf4;
}

.plan-task-main {
  display: grid;
  min-width: 0;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 8px;
}

.plan-task-index {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 999px;
  background: #e2e8f0;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.plan-task-main strong {
  display: block;
  overflow-wrap: anywhere;
  color: #1f2937;
  font-size: 13px;
}

.plan-task-main p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.tool-call {
  display: grid;
  gap: 6px;
  color: #8b9bb0;
  font-size: 12px;
  white-space: normal;
}

.tool-call + .tool-call {
  margin-top: 8px;
  padding-top: 8px;
}

.tool-call-header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  color: #8b9bb0;
  white-space: normal;
}

.tool-call-name {
  min-width: 0;
  overflow-wrap: anywhere;
  font-weight: 700;
}

.tool-call-header small {
  flex: 0 0 auto;
  color: #8b9bb0;
  font-size: 12px;
}

.tool-call-section {
  display: grid;
  gap: 4px;
}

.tool-call-section-title {
  color: #8b9bb0;
  font-size: 12px;
  font-weight: 700;
}

.tool-call-text {
  color: #8b9bb0;
  font-size: 12px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.subagent-call {
  display: grid;
  gap: 8px;
  white-space: normal;
}

.subagent-call-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #334155;
}

.subagent-call-name {
  min-width: 0;
  overflow-wrap: anywhere;
  font-weight: 760;
}

.subagent-call-header small {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
}

.subagent-call-content {
  color: #374151;
  overflow-wrap: anywhere;
}

.stream-stages {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
}

.stream-stages-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 30px;
  margin-top: 10px;
  color: var(--subtle);
  font-size: 12px;
}

.stream-stages-header span {
  white-space: nowrap;
}

.stream-stages-header span:first-child {
    color: var(--subtle);
  font-weight: 700;
}

.plan-drawer-toggle {
  display: inline-flex;
  height: 34px;
  align-items: center;
  gap: 6px;
  border: 1px solid #cfe0f8;
  border-radius: 999px;
  padding: 0 10px;
  cursor: pointer;
  color: #ffffff;
  background: #1d6ff2;
  box-shadow: 0 10px 24px rgba(42, 87, 143, 0.12);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  transition: background 180ms ease, color 180ms ease;
}

.plan-drawer-toggle:hover,
.agent-chat-page.is-plan-drawer-open .plan-drawer-toggle {
  color: #ffffff;
  background: #a0cfff;
}

.plan-drawer-toggle small {
  display: grid;
  min-width: 18px;
  height: 18px;
  place-items: center;
  border-radius: 999px;
  background: rgba(29, 111, 242, 0.12);
  color: inherit;
  font-size: 11px;
  line-height: 1;
}

.execution-plan-panel {
  display: flex;
  position: relative;
  z-index: 9;
  grid-column: 3;
  grid-row: 1;
  flex-direction: column;
  width: 230px;
  height: 100%;
  padding: 16px 12px;
  overflow-y: auto;
  opacity: 0;
  transform: translateX(calc(100% + 5px));
  transition: transform 180ms ease, opacity 180ms ease;
  pointer-events: none;
}

.execution-plan-panel.open {
  opacity: 1;
  transform: translateX(0);
  pointer-events: auto;
}

.execution-plan-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.execution-plan-title {
  min-width: 0;
}

.execution-plan-header h3 {
  margin: 0;
  color: #0f1f3a;
  font-size: 17px;
  font-weight: 820;
  letter-spacing: 0;
}

.execution-plan-header p {
  margin: 5px 0 0;
  overflow: hidden;
  color: #6c7890;
  font-size: 12px;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.execution-plan-actions {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
}

.execution-plan-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 8px;
  margin-bottom: 10px;
  color: #1f2f49;
  font-size: 12px;
  font-weight: 760;
}

.execution-plan-meta span + span::before {
  margin-right: 6px;
  color: #8da0ba;
  content: '·';
}

.execution-plan-progress {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.execution-progress-bar {
  overflow: hidden;
  height: 6px;
  border-radius: 999px;
  background: #e5eef9;
}

.execution-progress-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1d6ff2, #5aa2ff);
  transition: width 180ms ease;
}

.execution-plan-progress small {
  color: #64748b;
  font-size: 11px;
  white-space: nowrap;
}

.execution-task-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.execution-task-item {
  display: grid;
  position: relative;
  min-height: 0;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 8px;
  padding: 0;
}

.execution-task-item:last-child {
  min-height: 0;
  padding-bottom: 0;
}

.execution-task-rail {
  display: flex;
  position: relative;
  justify-content: center;
}

.execution-task-item:not(:last-child) .execution-task-rail::after {
  position: absolute;
  top: 30px;
  bottom: -10px;
  left: 50%;
  border-left: 2px dashed #d9e5f5;
  content: '';
  transform: translateX(-50%);
}

.execution-task-index {
  display: grid;
  position: relative;
  z-index: 1;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #eaf4ff;
  color: #1d6ff2;
  font-size: 13px;
  font-weight: 800;
}


.execution-task-content {
  min-width: 0;
  padding: 2px 0 10px;
}

.execution-task-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content;
  align-items: start;
  gap: 6px;
}

.execution-task-content strong {
  display: block;
  color: #1d2c45;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.execution-task-state {
  border-radius: 999px;
  padding: 2px 6px;
  color: #1d6ff2;
  background: #eef5ff;
  font-size: 11px;
  font-weight: 760;
  white-space: nowrap;
}

.execution-task-content p {
  display: -webkit-box;
  overflow: hidden;
  margin: 5px 0 0;
  color: #79869b;
  font-size: 12px;
  line-height: 1.55;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.execution-task-tool {
  display: inline-flex;
  max-width: 100%;
  margin-top: 5px;
  border-radius: 6px;
  padding: 2px 6px;
  overflow: hidden;
  color: #1d6ff2;
  background: #eff6ff;
  font-size: 11px;
  font-weight: 760;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.execution-task-priority {
  display: inline-flex;
  margin: 5px 0 0 5px;
  border-radius: 6px;
  padding: 2px 6px;
  color: #64748b;
  background: #f1f5f9;
  font-size: 11px;
  font-weight: 760;
  white-space: nowrap;
}

.execution-task-priority.high {
  color: #d92d45;
  background: #fff0f2;
}

.execution-task-priority.medium {
  color: #b77904;
  background: #fff7df;
}

.execution-task-priority.low {
  color: #159252;
  background: #eafaf1;
}

.execution-task-item.in_progress .execution-task-index {
  background: #fff4db;
  color: #d98909;
}

.execution-task-item.in_progress .execution-task-state {
  color: #b77904;
  background: #fff7df;
}

.execution-task-item.completed .execution-task-state {
  color: #159252;
  background: #eafaf1;
}

.execution-task-item.failed .execution-task-index,
.execution-task-item.blocked .execution-task-index {
  background: #fff0f2;
  color: #e5485d;
}

.execution-task-item.failed .execution-task-state,
.execution-task-item.blocked .execution-task-state {
  color: #d92d45;
  background: #fff0f2;
}

.execution-task-item.cancelled .execution-task-index {
  background: #eef2f7;
  color: #64748b;
}

.execution-task-item.cancelled .execution-task-state {
  color: #64748b;
  background: #eef2f7;
}

.execution-task-empty {
  display: flex;
  min-height: 92px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed #d9e5f5;
  border-radius: 10px;
  background: #f8fbff;
  color: #8b9ab0;
  font-size: 12px;
  text-align: center;
}

.execution-task-empty strong {
  color: #1d2c45;
  font-size: 13px;
}

.stream-stage {
  display: grid;
  min-height: 28px;
  align-items: center;
  grid-template-columns: 8px minmax(76px, 0.8fr) minmax(110px, 1fr) minmax(54px, auto) minmax(66px, auto) minmax(44px, auto);
  gap: 8px;
  color: var(--subtle);
  font-size: 12px;
}

.stream-stage + .stream-stage {
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.stream-stage-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #94a3b8;
}

.stream-stage.status-running .stream-stage-dot {
  animation: stage-pulse 1.2s ease-in-out infinite;
  background: #eab308;
}

.stream-stage.status-waiting .stream-stage-dot {
  background: #f59e0b;
}

.stream-stage.status-done .stream-stage-dot {
  background: #16a34a;
}

.stream-stage.status-error .stream-stage-dot {
  background: #ef4444;
}

.stream-stage-label,
.stream-stage-time {
  color: var(--ink);
  font-weight: 700;
}

.stream-stage-event,
.stream-stage-label,
.stream-stage-count,
.stream-stage-state,
.stream-stage-time {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stream-stage-state {
  justify-self: end;
}

.stream-stage.status-running .stream-stage-state {
  color: #a16207;
}

.stream-stage.status-waiting .stream-stage-state {
  color: #b45309;
}

.stream-stage.status-done .stream-stage-state {
  color: #15803d;
}

.stream-stage.status-error .stream-stage-state {
  color: #dc2626;
}

@keyframes stage-pulse {
  0%,
  100% {
    opacity: 0.45;
    transform: scale(0.9);
  }

  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

.message-row.assistant.typing .message-content::after {
  display: inline-block;
  width: 6px;
  height: 1em;
  margin-left: 3px;
  animation: cursor-blink 1s steps(2, start) infinite;
  background: var(--primary);
  content: '';
  vertical-align: -2px;
}

@keyframes cursor-blink {
  0%,
  45% {
    opacity: 1;
  }

  46%,
  100% {
    opacity: 0;
  }
}

.empty-chat {
  display: grid;
  height: 100%;
  place-items: center;
  color: var(--subtle);
}

.chat-input-panel {
  display: grid;
  grid-template-areas:
    "shortcuts shortcuts"
    "input actions";
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 8px 12px;
  margin: 0 32px 8px;
  padding: 10px 12px;
  border: 1px solid #d8e4f5;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.06);
}

.chat-input-shortcuts {
  display: flex;
  grid-area: shortcuts;
  gap: 8px;
}

.chat-input {
  grid-area: input;
}

.chat-input :deep(.el-textarea__inner) {
  min-height: 38px !important;
  max-height: 96px !important;
  padding: 8px 0;
  border: 0;
  box-shadow: none;
  color: #17233c;
  font-size: 14px;
  line-height: 22px;
}

.chat-actions {
  display: flex;
  grid-area: actions;
  align-items: center;
  gap: 8px;
}

.chat-actions .el-button {
  min-width: 76px;
  height: 38px;
  border-radius: 999px;
  font-weight: 760;
}

.chat-disclaimer {
  padding: 4px 16px 14px;
  color: #8a96aa;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 960px) {
  .agent-chat-page,
  .agent-chat-page.is-plan-drawer-open {
    height: auto;
    min-height: 0;
    grid-template-columns: 1fr;
  }

  .chat-session-panel {
    min-height: 220px;
  }

  .chat-window {
    min-height: 560px;
  }

  .execution-plan-panel {
    position: absolute;
    top: 5px;
    right: 5px;
    bottom: 5px;
    grid-column: auto;
    width: min(280px, calc(100% - 20px));
    height: auto;
    min-height: 360px;
  }

  .chat-input-panel {
    grid-template-areas:
      "shortcuts"
      "input actions";
    grid-template-columns: minmax(0, 1fr) auto;
    margin: 0 16px 8px;
  }

  .chat-actions {
    justify-content: flex-end;
  }

  .stream-stage {
    grid-template-columns: 8px minmax(74px, 0.9fr) minmax(92px, 1fr) minmax(52px, auto);
  }

  .stream-stage-count,
  .stream-stage-state {
    display: none;
  }
}
</style>
