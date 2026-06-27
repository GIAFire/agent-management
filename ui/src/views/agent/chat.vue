<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bottom, ChatLineRound, Close, Delete, Plus, Promotion } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { chatStream } from '@/axios/chat'

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
const streaming = ref(false)
const autoScrollEnabled = ref(true)
const messageListRef = ref()
const abortController = ref(null)

// 网络流和屏幕显示解耦：SSE 增量先进队列，再由动画帧平滑吐字。
let typewriterFrame = null
let typewriterQueue = []
let typewriterMessage = null
let typewriterSession = null
let typewriterIdleResolve = null
let saveTimer = null
let scrollFrame = null
let pendingScrollForce = false
let snowflakeLastTimestamp = -1n
let snowflakeSequence = 0n
let snowflakeWorkerId = null
let snowflakeDatacenterId = null

const TYPEWRITER_CHARS_PER_FRAME = 1
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

const canSend = computed(() => {
  return Boolean(inputMessage.value.trim()) && !streaming.value && Boolean(agentId.value)
})

const STREAM_STAGE_META = {
  reasoning: { label: '推理事件', order: 10 },
  tool: { label: '工具调用事件', order: 20 },
  message: { label: '正文事件', order: 30 }
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

const getStageKey = (eventType = '') => {
  const normalizedType = String(eventType || '').toUpperCase()

  if (normalizedType.startsWith('THINKING_BLOCK_') || normalizedType.startsWith('THINKING_')) {
    return 'reasoning'
  }
  if (normalizedType.startsWith('TOOL_CALL_') || normalizedType.startsWith('TOOL_RESULT_')) {
    return 'tool'
  }
  if (normalizedType.startsWith('TEXT_BLOCK_') || normalizedType.startsWith('MESSAGE_')) {
    return 'message'
  }

  return null
}

const isStageEndEvent = (eventType = '') => {
  const normalizedType = String(eventType || '').toUpperCase()
  return normalizedType === 'DONE' ||
    normalizedType === 'ERROR' ||
    normalizedType === 'THINKING_BLOCK_END' ||
    normalizedType === 'TOOL_RESULT_END' ||
    normalizedType === 'TEXT_BLOCK_END' ||
    normalizedType === 'MESSAGE_END'
}

const eventLabel = (eventType = '') => {
  return STREAM_EVENT_LABELS[eventType] || eventType || '未知事件'
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
  return block?.kind === 'tool' ? `工具调用${sequence}` : `推理过程${sequence}`
}

const toolStatusText = (toolCall) => {
  if (toolCall?.status === 'done') {
    return '完成'
  }
  if (toolCall?.status === 'error') {
    return '异常'
  }
  return '调用中'
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

const normalizedEventType = (streamEvent) => String(streamEvent?.eventType || '').toUpperCase()

const normalizedSseEvent = (streamEvent) => String(streamEvent?.sseEvent || '').toLowerCase()

const isReasoningStartEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'THINKING_BLOCK_START' ||
    normalizedSseEvent(streamEvent) === 'thinking_start'
}

const isReasoningDeltaEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'THINKING_BLOCK_DELTA' ||
    normalizedSseEvent(streamEvent) === 'thinking_delta'
}

const isReasoningEndEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'THINKING_BLOCK_END' ||
    normalizedSseEvent(streamEvent) === 'thinking_end'
}

const isReasoningStreamEvent = (streamEvent) => {
  return isReasoningStartEvent(streamEvent) ||
    isReasoningDeltaEvent(streamEvent) ||
    isReasoningEndEvent(streamEvent)
}

const isToolStreamEvent = (streamEvent) => {
  const eventType = normalizedEventType(streamEvent)
  const sseEvent = normalizedSseEvent(streamEvent)
  return eventType.startsWith('TOOL_CALL_') ||
    eventType.startsWith('TOOL_RESULT_') ||
    sseEvent.startsWith('tool_call_') ||
    sseEvent.startsWith('tool_result_')
}

const isToolCallStartEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'TOOL_CALL_START' ||
    normalizedSseEvent(streamEvent) === 'tool_call_start'
}

const isToolCallDeltaEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'TOOL_CALL_DELTA' ||
    normalizedSseEvent(streamEvent) === 'tool_call_delta'
}

const isToolCallEndEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'TOOL_CALL_END' ||
    normalizedSseEvent(streamEvent) === 'tool_call_end'
}

const isToolResultStartEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'TOOL_RESULT_START' ||
    normalizedSseEvent(streamEvent) === 'tool_result_start'
}

const isToolResultTextDeltaEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'TOOL_RESULT_TEXT_DELTA' ||
    normalizedSseEvent(streamEvent) === 'tool_result_text_delta'
}

const isToolResultDataDeltaEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'TOOL_RESULT_DATA_DELTA' ||
    normalizedSseEvent(streamEvent) === 'tool_result_data_delta'
}

const isToolResultEndEvent = (streamEvent) => {
  return normalizedEventType(streamEvent) === 'TOOL_RESULT_END' ||
    normalizedSseEvent(streamEvent) === 'tool_result_end'
}

const hasAuxiliaryOutput = (message) => {
  return Boolean(
    (Array.isArray(message?.auxiliaryBlocks) && message.auxiliaryBlocks.length) ||
    message?.thinkingContent ||
    message?.toolResultContent ||
    (Array.isArray(message?.toolCalls) && message.toolCalls.length)
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

const ensureAuxiliaryBlocks = (message) => {
  if (!Array.isArray(message.auxiliaryBlocks)) {
    message.auxiliaryBlocks = []
  }
  return message.auxiliaryBlocks
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
  return {
    id: createAuxiliaryBlockId(kind),
    kind,
    sequence,
    title: kind === 'tool' ? `工具调用 ${sequence}` : `推理过程 ${sequence}`,
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
  const kind = block?.kind === 'tool' ? 'tool' : 'reasoning'
  const toolCall = kind === 'tool'
    ? {
      id: block.toolCall?.id || block.id || createToolCallId(),
      name: block.toolCall?.name || '工具调用',
      process: block.toolCall?.process || '',
      result: block.toolCall?.result || '',
      status: block.toolCall?.status || block.status || 'done',
      argumentStarted: Boolean(block.toolCall?.argumentStarted),
      resultStarted: Boolean(block.toolCall?.resultStarted),
      startedAt: block.toolCall?.startedAt || block.startedAt || 0,
      updatedAt: block.toolCall?.updatedAt || block.endedAt || 0
    }
    : null

  return {
    id: block?.id || `${kind}-stored-${index}`,
    kind,
    sequence: block?.sequence || index + 1,
    title: block?.title || (kind === 'tool' ? `工具调用 ${block?.sequence || index + 1}` : `推理过程 ${block?.sequence || index + 1}`),
    expanded: block?.expandedByUser ? Boolean(block.expanded) : false,
    expandedByUser: Boolean(block?.expandedByUser),
    status: block?.status || 'done',
    startedAt: block?.startedAt || 0,
    endedAt: block?.endedAt || null,
    durationMs: block?.durationMs || 0,
    content: block?.content || '',
    toolCall
  }
}

const normalizeStoredToolCalls = (message) => {
  if (Array.isArray(message?.toolCalls)) {
    return message.toolCalls.map((toolCall, index) => ({
      id: toolCall.id || `tool-${message.id || 'message'}-${index}`,
      name: toolCall.name || '工具调用',
      process: toolCall.process || '',
      result: toolCall.result || '',
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
  const counts = { reasoning: 0, tool: 0 }
  blocks.forEach((block) => {
    counts[block.kind] = (counts[block.kind] || 0) + 1
    block.sequence = counts[block.kind]
    block.title = block.kind === 'tool' ? `工具调用 ${block.sequence}` : `推理过程 ${block.sequence}`
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
    const counts = { reasoning: 0, tool: 0 }
    const blocks = message.auxiliaryBlocks.map((block, index) => {
      const normalized = normalizeAuxiliaryBlock(block, index)
      counts[normalized.kind] += 1
      normalized.sequence = normalized.sequence || counts[normalized.kind]
      normalized.title = normalized.title || (normalized.kind === 'tool'
        ? `工具调用 ${normalized.sequence}`
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
    appendToolText(toolCall, 'result', delta)
    touchAuxiliaryBlock(block, 'running')
    return true
  }

  if (isToolResultDataDeltaEvent(streamEvent)) {
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
  const eventType = streamEvent.eventType || streamEvent.sseEvent || 'UNKNOWN'
  const stageKey = getStageKey(eventType)
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
  stage.lastEventType = eventType
  stage.lastEventLabel = eventLabel(eventType)
  stage.gapMs = gapMs
  stage.durationMs = now - stage.startedAt
  stage.status = eventType === 'ERROR' ? 'error' : isStageEndEvent(eventType) ? 'done' : 'running'

  if (stage.status !== 'running') {
    stage.endedAt = now
    stage.durationMs = stage.endedAt - stage.startedAt
  }

  timing.lastAt = now
  timing.eventCount += 1
  timing.totalMs = now - timing.startedAt
  message.streamStatus = eventType === 'ERROR' ? 'error' : 'running'

  scheduleSaveSessions()
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

  if (appendReasoningEvent(message, streamEvent)) {
    changed = true
  }

  if (appendToolEvent(message, streamEvent)) {
    changed = true
  }

  if (!changed) {
    return
  }

  if (session) {
    session.updatedAt = nowText()
  }

  scheduleSaveSessions()
  scheduleScrollToBottom()
}

const finishStreamEvents = (message, status = 'done') => {
  if (!message?.streamTiming) {
    return
  }

  const now = getNowMs()
  message.streamTiming.totalMs = now - message.streamTiming.startedAt
  message.streamStatus = status

  if (Array.isArray(message.eventStages)) {
    message.eventStages.forEach((stage) => {
      if (stage.status === 'running') {
        stage.status = status === 'error' ? 'error' : 'done'
        stage.endedAt = now
        stage.durationMs = stage.endedAt - stage.startedAt
      }
    })
  }

  if (Array.isArray(message.toolCalls)) {
    message.toolCalls.forEach((toolCall) => {
      if (toolCall.status === 'running') {
        toolCall.status = status === 'error' ? 'error' : 'done'
        toolCall.updatedAt = now
      }
    })
  }

  if (Array.isArray(message.auxiliaryBlocks)) {
    message.auxiliaryBlocks = compactAuxiliaryBlocks(message.auxiliaryBlocks)
    message.auxiliaryBlocks.forEach((block) => {
      if (block.status === 'running') {
        block.status = status === 'error' ? 'error' : 'done'
        block.endedAt = now
        block.durationMs = block.startedAt ? block.endedAt - block.startedAt : block.durationMs || 0
      }
      if (block.toolCall?.status === 'running') {
        block.toolCall.status = status === 'error' ? 'error' : 'done'
        block.toolCall.updatedAt = now
      }
    })
  }

  message.activeReasoningBlockId = null
  message.activeToolBlockId = null
  message.pendingReasoningStartedAt = null

  scheduleSaveSessions()
}

const messageText = (message) => {
  return message.displayContent ?? message.content ?? ''
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

const cancelTypewriterFrame = () => {
  if (typewriterFrame) {
    window.cancelAnimationFrame(typewriterFrame)
    typewriterFrame = null
  }
}

const resolveTypewriterIdle = () => {
  if (typewriterIdleResolve) {
    typewriterIdleResolve()
    typewriterIdleResolve = null
  }
}

const drainTypewriter = () => {
  typewriterFrame = null

  if (!typewriterMessage || !typewriterQueue.length) {
    resolveTypewriterIdle()
    return
  }

  const nextText = typewriterQueue.splice(0, TYPEWRITER_CHARS_PER_FRAME).join('')
  // 只更新 displayContent，content 保留完整原文，形成 ChatGPT 式平滑输出。
  typewriterMessage.displayContent = `${typewriterMessage.displayContent || ''}${nextText}`
  typewriterMessage.pending = false
  typewriterMessage.typing = true

  scheduleSaveSessions()
  scheduleScrollToBottom()

  if (typewriterQueue.length) {
    startTypewriter()
  } else {
    resolveTypewriterIdle()
  }
}

const startTypewriter = () => {
  if (!typewriterFrame) {
    typewriterFrame = window.requestAnimationFrame(drainTypewriter)
  }
}

const pushTypewriterText = (message, session, delta) => {
  if (!delta) {
    return
  }

  typewriterMessage = message
  typewriterSession = session
  // content 立即累计完整回复，displayContent 由打字机逐帧追上。
  message.content = `${message.content || ''}${delta}`
  message.displayContent = message.displayContent ?? ''
  typewriterQueue.push(...Array.from(delta))
  message.pending = false
  message.typing = true
  startTypewriter()
}

const flushTypewriter = () => {
  cancelTypewriterFrame()

  if (typewriterMessage && typewriterQueue.length) {
    typewriterMessage.displayContent = typewriterMessage.content || `${typewriterMessage.displayContent || ''}${typewriterQueue.join('')}`
    typewriterMessage.pending = false
    typewriterMessage.typing = false
    if (typewriterSession) {
      typewriterSession.updatedAt = nowText()
    }
    typewriterQueue = []
  }

  flushScheduledSave()
  scheduleScrollToBottom()
  resolveTypewriterIdle()
}

const resetTypewriter = () => {
  cancelTypewriterFrame()
  typewriterQueue = []
  typewriterMessage = null
  typewriterSession = null
  resolveTypewriterIdle()
}

const waitTypewriterIdle = () => {
  if (!typewriterQueue.length && !typewriterFrame) {
    return Promise.resolve()
  }

  return new Promise((resolve) => {
    typewriterIdleResolve = resolve
  })
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
          return {
            ...message,
            displayContent: message.displayContent ?? message.content ?? '',
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
            pendingReasoningStartedAt: null,
            toolExpanded: message.toolExpanded ?? message.toolResultExpanded ?? Boolean(toolCalls.length),
            eventStages: Array.isArray(message.eventStages) ? message.eventStages : []
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

const appendMessage = (role, content, extra = {}, options = {}) => {
  const session = activeSession.value || createSession()
  const message = {
    id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    role,
    content,
    displayContent: content,
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
      pendingReasoningStartedAt: null,
      toolExpanded: true
    },
    { forceScroll: true }
  )
  const session = activeSession.value
  const backendSessionId = getBackendSessionId(session)
  saveSessions()

  resetTypewriter()
  streaming.value = true
  abortController.value = new AbortController()

  try {
    await chatStream(
      {
        agentId: agentId.value,
        sessionId: backendSessionId,
        content
      },
      {
        onEvent: (streamEvent) => {
          recordStreamEvent(assistantMessage, streamEvent)
          appendAuxiliaryDelta(assistantMessage, session, streamEvent)
        },
        onMessage: (delta) => {
          pushTypewriterText(assistantMessage, session, delta)
        },
        onError: (message) => {
          finishStreamEvents(assistantMessage, 'error')
          resetTypewriter()
          assistantMessage.content = message
          assistantMessage.displayContent = message
          assistantMessage.pending = false
          assistantMessage.typing = false
          assistantMessage.error = true
          session.updatedAt = nowText()
          saveSessions()
          scrollToBottom()
        },
        onDone: (streamEvent) => {
          applyDoneUsage(assistantMessage, streamEvent)
          finishStreamEvents(assistantMessage, 'done')
          session.updatedAt = nowText()
        }
      },
      {
        signal: abortController.value.signal
      }
    )
    await waitTypewriterIdle()
    assistantMessage.pending = false
    assistantMessage.typing = false
    assistantMessage.displayContent = assistantMessage.content
    session.updatedAt = nowText()
    flushScheduledSave()
  } catch (error) {
    flushTypewriter()
    finishStreamEvents(assistantMessage, error.name === 'AbortError' ? 'done' : 'error')
    if (error.name === 'AbortError') {
      assistantMessage.content = assistantMessage.content || '已停止生成'
      assistantMessage.displayContent = assistantMessage.displayContent || assistantMessage.content
    } else {
      assistantMessage.content = error.message || '对话请求失败'
      assistantMessage.displayContent = assistantMessage.content
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
    resetTypewriter()
    applyRouteAgentInfo()
    loadSessions()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  stopStream()
  resetTypewriter()

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
  <section class="agent-chat-page">
    <aside class="chat-session-panel">
      <div class="session-panel-header">
        <div>
          <h2>会话列表</h2>
          <span>{{ agentInfo.name }}</span>
        </div>
        <el-tooltip content="新建会话" placement="right">
          <el-button type="primary" :icon="Plus" circle @click="createSession" />
        </el-tooltip>
      </div>

      <div class="session-list">
        <button
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === activeSessionId }"
          type="button"
          @click="selectSession(session)"
        >
          <span class="session-main">
            <strong>{{ session.title }}</strong>
            <small>{{ session.updatedAt }}</small>
          </span>
          <el-tooltip content="删除会话" placement="right">
            <el-button
              class="session-delete"
              :icon="Delete"
              link
              type="danger"
              @click.stop="removeSession(session)"
            />
          </el-tooltip>
        </button>
      </div>
    </aside>

    <section class="chat-window">
      <header class="chat-header">
        <div class="chat-agent-title">
          <span class="chat-agent-icon">
            <el-icon><ChatLineRound /></el-icon>
          </span>
          <div>
            <h2>{{ agentInfo.name }}</h2>
            <span>{{ agentInfo.key || 'Agent Chat' }}</span>
          </div>
        </div>
        <el-tag :type="streaming ? 'warning' : 'success'">
          {{ streaming ? '生成中' : '就绪' }}
        </el-tag>
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
            <div class="message-meta">
              <span>{{ message.role === 'user' ? '我' : agentInfo.name }}</span>
              <time>{{ message.createdAt }}</time>
            </div>
            <div
              v-if="message.role === 'assistant' && hasAuxiliaryOutput(message)"
              class="auxiliary-output"
            >
              <section
                v-for="block in message.auxiliaryBlocks || []"
                :key="block.id"
                class="auxiliary-panel"
              >
                <button
                  class="auxiliary-toggle"
                  type="button"
                  @click="toggleAuxiliaryPanel(message, block)"
                >
                  <span>{{ auxiliaryBlockTitle(block) }}</span>
                  <small v-if="auxiliaryBlockTimeText(block)">耗时 {{ auxiliaryBlockTimeText(block) }}</small>
                  <small>{{ block.expanded ? '收起' : '展开' }}</small>
                </button>
                <div
                  v-show="block.expanded"
                  class="auxiliary-content"
                >
                  <div v-if="block.kind === 'reasoning'">
                    {{ block.content }}
                  </div>
                  <div
                    v-else
                    class="tool-call"
                  >
                    <div class="tool-call-header">
                      <span class="tool-call-name">{{ block.toolCall?.name || '工具调用' }}</span>
                      <small>{{ toolStatusText(block.toolCall || block) }}</small>
                    </div>
                    <div
                      v-if="block.toolCall?.process"
                      class="tool-call-section"
                    >
                      <span class="tool-call-section-title">调用过程</span>
                      <div class="tool-call-text">{{ block.toolCall.process }}</div>
                    </div>
                    <div
                      v-if="block.toolCall?.result"
                      class="tool-call-section"
                    >
                      <span class="tool-call-section-title">调用结果</span>
                      <div class="tool-call-text">{{ block.toolCall.result }}</div>
                    </div>
                  </div>
                </div>
              </section>
            </div>

            <div
              v-if="shouldShowMessageContent(message)"
              class="message-content"
              :class="{ 'markdown-content': message.role === 'assistant' }"
            >
              <div
                v-if="message.role === 'assistant'"
                class="markdown-rendered"
                v-html="renderAssistantMarkdown(message)"
              />
              <template v-else>
                {{ messageDisplayText(message) }}
              </template>
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
        <el-input
          v-model="inputMessage"
          class="chat-input"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入消息"
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
  </section>
</template>

<style scoped>
.agent-chat-page {
  display: grid;
  height: calc(100vh - 142px);
  min-height: 560px;
  grid-template-columns: 288px minmax(0, 1fr);
  gap: 14px;
}

.chat-session-panel,
.chat-window {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow);
}

.chat-session-panel {
  display: flex;
  flex-direction: column;
}

.session-panel-header {
  display: flex;
  min-height: 68px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border);
}

.session-panel-header h2,
.chat-agent-title h2 {
  margin: 0;
  color: var(--ink);
  font-size: 16px;
  font-weight: 760;
  letter-spacing: 0;
}

.session-panel-header span,
.chat-agent-title span {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  color: var(--subtle);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  display: flex;
  width: 100%;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 0 0 8px;
  padding: 10px 8px 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  background: transparent;
  text-align: left;
}

.session-item:hover {
  background: var(--surface-muted);
}

.session-item.active {
  border-color: var(--primary);
  background: var(--primary-soft);
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
  color: var(--ink);
  font-size: 14px;
  font-weight: 700;
}

.session-main small {
  margin-top: 5px;
  color: var(--subtle);
  font-size: 12px;
}

.session-delete {
  flex: 0 0 auto;
}

.chat-window {
  display: flex;
  position: relative;
  flex-direction: column;
}

.chat-header {
  display: flex;
  min-height: 68px;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 0 18px;
  border-bottom: 1px solid var(--border);
}

.chat-agent-title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.chat-agent-icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: var(--primary);
  font-size: 18px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  background: var(--page-bg);
}

.back-bottom-button {
  position: absolute;
  right: 24px;
  bottom: 112px;
  z-index: 3;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.18);
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

.message-row.user + .message-row.assistant,
.message-row.assistant + .message-row.user {
  padding-top: 100px;
}

.message-bubble {
  max-width: min(720px, 78%);
  border: 0;
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.message-row.user .message-bubble {
  color: var(--ink);
}

.message-row.error .message-bubble {
  color: var(--danger);
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 30px;
  margin-bottom: 8px;
  font-size: 12px;
}

.message-meta span {
  font-weight: 700;
}

.message-meta time {
  color: var(--subtle);
}

.message-row.user .message-meta time {
  color: var(--subtle);
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
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
  gap: 8px;
  margin-bottom: 10px;
}

.auxiliary-panel {
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.78);
}

.auxiliary-toggle {
  display: grid;
  width: 100%;
  min-height: 32px;
  align-items: center;
  grid-template-columns: max-content max-content max-content;
  gap: 10px;
  border: 0;
  cursor: pointer;
  background: transparent;
  color: #64748b;
  font: inherit;
  text-align: left;
}

.auxiliary-toggle:hover {
  background: rgba(226, 232, 240, 0.48);
}

.auxiliary-toggle span {
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

.auxiliary-toggle span + small:not(:last-child) {
  margin-left: 20px;
}

.auxiliary-content {
  padding: 0 10px 10px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.tool-call {
  display: grid;
  gap: 8px;
  white-space: normal;
}

.tool-call + .tool-call {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
}

.tool-call-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #374151;
  white-space: normal;
}

.tool-call-name {
  min-width: 0;
  overflow-wrap: anywhere;
  font-weight: 700;
}

.tool-call-header small {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
}

.tool-call-section {
  display: grid;
  gap: 4px;
}

.tool-call-section-title {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.tool-call-text {
  color: #4b5563;
  white-space: pre-wrap;
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
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  padding: 14px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}

.chat-input :deep(.el-textarea__inner) {
  min-height: 76px !important;
}

.chat-actions {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

@media (max-width: 960px) {
  .agent-chat-page {
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

  .chat-input-panel {
    grid-template-columns: 1fr;
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
