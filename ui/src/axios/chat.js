import config from '@/axios/axiosConfig'
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
  return streamEvent.eventType === 'TEXT_BLOCK_DELTA' ||
    streamEvent.sseEvent === 'message_delta' ||
    (!isObject(streamEvent.data) && Boolean(streamEvent.delta))
}

// 解析单个 SSE 事件块，后端返回的数据形态是 data: {"eventType":"TEXT_BLOCK_DELTA","delta":"..."}。
const dispatchSseBlock = (block, handlers) => {
  if (!block.trim()) {
    return
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
    return
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

  if (eventName === 'done' || streamEvent.eventType === 'DONE' || content === '[DONE]') {
    handlers.onDone?.(streamEvent)
    return
  }

  if (eventName === 'error' || streamEvent.eventType === 'ERROR') {
    handlers.onError?.(content || '对话请求失败', streamEvent)
    return
  }

  if (isTextDeltaEvent(streamEvent)) {
    handlers.onMessage?.(content || '', streamEvent)
  }
}

// 网络分片可能把一个 SSE 事件拆开，保留最后一个未完整结束的块等待下次拼接。
const consumeSseBuffer = (buffer, handlers) => {
  const blocks = buffer.split(/\r?\n\r?\n/)
  const rest = blocks.pop() || ''

  blocks.forEach((block) => dispatchSseBlock(block, handlers))

  return rest
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
    buffer = consumeSseBuffer(buffer, handlers)
  }

  buffer += decoder.decode()
  if (buffer.trim()) {
    dispatchSseBlock(buffer, handlers)
  }
}

export const chatStream = async (data, handlers = {}, options = {}) => {
  return postStream('/agent/chat/chatStream', data, handlers, options)
}

export const userConfirmStream = async (data, handlers = {}, options = {}) => {
  return postStream('/agent/chat/userConfirm', data, handlers, options)
}

export const externalExecutionStream = async (data, handlers = {}, options = {}) => {
  return postStream('/agent/chat/externalExecution', data, handlers, options)
}
