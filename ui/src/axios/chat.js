import config from '@/axios/axiosConfig'
import { getToken, getTokenType } from '@/utils/auth'

const isObject = (value) => typeof value === 'object' && value !== null
const LONG_NUMBER_TEXT = /^\d{16,19}$/
const SESSION_ID_PLACEHOLDER = '__CHAT_SESSION_ID_PLACEHOLDER__'

const stringifyChatRequest = (data = {}) => {
  const sessionId = data.sessionId == null ? '' : String(data.sessionId)

  if (!LONG_NUMBER_TEXT.test(sessionId)) {
    return JSON.stringify(data)
  }

  return JSON.stringify({
    ...data,
    sessionId: SESSION_ID_PLACEHOLDER
  }).replace(`"${SESSION_ID_PLACEHOLDER}"`, sessionId)
}

const normalizeStreamEvent = (eventName, eventId, parsedData, rawData) => {
  const dataObject = isObject(parsedData) ? parsedData : null
  const delta = dataObject
    ? dataObject.delta ?? dataObject.content ?? ''
    : String(parsedData)

  return {
    id: eventId || dataObject?.seq || null,
    sseEvent: eventName,
    eventName,
    eventType: dataObject?.eventType || eventName,
    runId: dataObject?.runId ?? null,
    seq: dataObject?.seq ?? null,
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

export const chatStream = async (data, handlers = {}, options = {}) => {
  const token = getToken()
  const headers = {
    Accept: 'text/event-stream',
    'Content-Type': 'application/json'
  }

  if (token) {
    headers.Authorization = `${getTokenType()} ${token}`
  }

  // 使用 fetch 读取 ReadableStream，避免 axios 把流式响应合并成一次性结果。
  const response = await fetch(`${config.baseURL}/agent/chat/chatStream`, {
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
