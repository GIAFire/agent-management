import config from '@/axios/axiosConfig'
import { getToken, getTokenType } from '@/utils/auth'

// 解析单个 SSE 事件块，后端返回的数据形态是 data: {"content":"..."}。
const dispatchSseBlock = (block, handlers) => {
  if (!block.trim()) {
    return
  }

  let eventName = 'message'
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

  const content = typeof parsedData === 'object' && parsedData !== null
    ? parsedData.content
    : String(parsedData)

  if (eventName === 'done' || content === '[DONE]') {
    handlers.onDone?.()
    return
  }

  if (eventName === 'error') {
    handlers.onError?.(content || '对话请求失败')
    return
  }

  handlers.onMessage?.(content || '')
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
    body: JSON.stringify(data),
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
