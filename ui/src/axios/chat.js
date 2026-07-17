import config from '@/axios/axiosConfig'
import { get, post } from '@/axios/request'
import { getToken, getTokenType } from '@/utils/auth'

const isObject = (value) => typeof value === 'object' && value !== null

const isPlainObject = (value) => {
  return Object.prototype.toString.call(value) === '[object Object]'
}

const isIdKey = (key) => {
  return key === 'id' || key === 'ids' || key.endsWith('Id') || key.endsWith('Ids') || key.endsWith('ID') || key.endsWith('IDs')
}

const stringifyRequestIds = (value, key = '') => {
  if (isIdKey(key)) {
    if (Array.isArray(value)) {
      return value.map((item) => stringifyRequestIds(item, key))
    }
    return value === '' || value === undefined || value === null ? value : String(value)
  }

  if (Array.isArray(value)) {
    return value.map((item) => stringifyRequestIds(item))
  }

  if (!isPlainObject(value)) {
    return value
  }

  return Object.entries(value).reduce((result, [entryKey, entryValue]) => {
    result[entryKey] = stringifyRequestIds(entryValue, entryKey)
    return result
  }, {})
}

const stringifyChatRequest = (data = {}) => {
  return JSON.stringify(stringifyRequestIds(data))
}

const readRawId = (rawData, key) => {
  const match = String(rawData || '').match(new RegExp(`"${key}"\\s*:\\s*"?([0-9]+)"?`))
  return match ? match[1] : null
}

const normalizeId = (value, rawData, key) => {
  return readRawId(rawData, key) ?? (value === undefined || value === null ? null : String(value))
}

const normalizeStreamEvent = (eventName, eventId, parsedData, rawData) => {
  const dataObject = isObject(parsedData) ? parsedData : null
  const delta = dataObject
    ? dataObject.delta ?? dataObject.content ?? ''
    : String(parsedData)

  return {
    id: eventId || normalizeId(dataObject?.seq, rawData, 'seq'),
    sseEvent: eventName,
    eventName,
    eventType: dataObject?.eventType || eventName,
    runId: normalizeId(dataObject?.runId, rawData, 'runId'),
    seq: dataObject?.seq ?? null,
    sourcePath: dataObject?.sourcePath ?? null,
    subAgentName: dataObject?.subAgentName ?? null,
    subAgentInstanceId: normalizeId(dataObject?.subAgentInstanceId, rawData, 'subAgentInstanceId'),
    subAgentTaskId: normalizeId(dataObject?.subAgentTaskId, rawData, 'subAgentTaskId'),
    usageToken: dataObject?.usageToken ?? null,
    usageTime: dataObject?.usageTime ?? null,
    payload: dataObject?.payload ?? null,
    delta,
    data: parsedData,
    rawData,
    receivedAt: Date.now()
  }
}

const isTextDeltaEvent = (streamEvent) => {
  const sseEvent = String(streamEvent.sseEvent || '')
  return sseEvent === 'message_delta'
}

const isStreamDoneEvent = (streamEvent) => {
  return streamEvent.sseEvent === 'agent_end' ||
    streamEvent.sseEvent === 'request_stop' ||
    streamEvent.sseEvent === 'exceed_max_iters' ||
    streamEvent.sseEvent === 'require_user_confirm' ||
    streamEvent.sseEvent === 'require_external_execution'
}

// 解析单个 SSE 事件块，后端返回的数据形态是 data: {"eventType":"TEXT_BLOCK_DELTA","delta":"..."}。
const dispatchSseBlock = (block, handlers) => {
  if (!block.trim()) {
    return false
  }

  let eventName = 'message'
  let eventId = ''
  const dataLines = []

  block.split(/\r?\n/).forEach((line) => {
    if (!line || line.startsWith(':')) {
      return
    }

    const index = line.indexOf(':')
    const field = index >= 0 ? line.slice(0, index) : line
    let value = index >= 0 ? line.slice(index + 1) : ''

    if (value.startsWith(' ')) {
      value = value.slice(1)
    }

    if (field === 'event') {
      eventName = value
    }
    if (field === 'id') {
      eventId = value
    }
    if (field === 'data') {
      dataLines.push(value)
    }
  })

  if (!dataLines.length) {
    return false
  }

  const rawData = dataLines.join('\n')
  let parsedData = rawData

  try {
    parsedData = JSON.parse(rawData)
  } catch {
    parsedData = rawData
  }

  const streamEvent = normalizeStreamEvent(eventName, eventId, parsedData, rawData)
  const content = streamEvent.delta

  handlers.onEvent?.(streamEvent)

  if (isStreamDoneEvent(streamEvent)) {
    handlers.onDone?.(streamEvent)
    return true
  }

  if (streamEvent.sseEvent === 'error') {
    handlers.onError?.(content || '对话请求失败', streamEvent)
    return true
  }

  if (isTextDeltaEvent(streamEvent)) {
    handlers.onMessage?.(content || '', streamEvent)
  }

  return false
}

// 网络分片可能把一个 SSE 事件拆开，保留最后一个未完整结束的块等待下次拼接。
const consumeSseBuffer = (buffer, handlers) => {
  const blocks = buffer.split(/\r?\n\r?\n/)
  const rest = blocks.pop() || ''
  let completed = false

  for (const block of blocks) {
    if (dispatchSseBlock(block, handlers)) {
      completed = true
      break
    }
  }

  return { rest, completed }
}

const postStream = async (url, data, handlers = {}, options = {}) => {
  const token = getToken()
  const headers = {
    Accept: 'text/event-stream',
    'Content-Type': 'application/json'
  }

  if (token) {
    headers.Authorization = `${getTokenType()} ${token}`
  }

  // 使用 fetch 读取 ReadableStream，避免 axios 把流式响应合并成一次性结果。
  const response = await fetch(`${config.baseURL}${url}`, {
    method: 'POST',
    headers,
    body: stringifyChatRequest(data),
    signal: options.signal
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `对话请求失败：${response.status}`)
  }

  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  handlers.onOpen?.()

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  // TextDecoder 的 stream 模式可以正确处理跨 chunk 的中文字符。
  while (true) {
    const { done, value } = await reader.read()

    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const result = consumeSseBuffer(buffer, handlers)
    buffer = result.rest
    if (result.completed) {
      await reader.cancel()
      return
    }
  }

  buffer += decoder.decode()
  if (buffer.trim()) {
    dispatchSseBlock(buffer, handlers)
  }
}

export const chatStream = async (data, handlers = {}, options = {}) => {
  return postStream('/agent/chat/chatStream', data, handlers, options)
}

export const createAgentSession = (data) => {
  return post('/agent/agentSession/create', data)
}

export const pageAgentSessions = (params) => {
  return get('/agent/agentSession/page', params)
}

export const deleteAgentSession = (id) => {
  return get(`/agent/agentSession/delete/${id}`)
}

export const pageAgentMessages = (params) => {
  return get('/agent/agentMessage/page', params)
}

export const userConfirmStream = async (data, handlers = {}, options = {}) => {
  return postStream('/agent/chat/userConfirm', data, handlers, options)
}

export const externalExecutionStream = async (data, handlers = {}, options = {}) => {
  return postStream('/agent/chat/externalExecution', data, handlers, options)
}
