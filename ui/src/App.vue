<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref } from 'vue'
import {
  ArrowDown,
  ArrowRight,
  CircleCheck,
  Close,
  Delete,
  Promotion,
  RefreshRight,
  Warning,
} from '@element-plus/icons-vue'

const SSE_ENDPOINT = '/agent/chat/chatSseEmitter'

const messages = reactive([])
const draft = ref('')
const conversationId = ref(createConversationId())
const activeSource = ref(null)
const activeAssistantId = ref(null)
const messageList = ref(null)
const composerInput = ref(null)

const isStreaming = computed(() => Boolean(activeSource.value))
const canSend = computed(() => draft.value.trim().length > 0 && !isStreaming.value)
const statusText = computed(() => (isStreaming.value ? '生成中' : '就绪'))
const statusIcon = computed(() => (isStreaming.value ? Warning : CircleCheck))

const suggestions = [
  '查询用户列表，并说明你调用了哪些工具',
  '帮我分析一下最近的订单角色权限',
  '用简短步骤说明这个 Agent 能做什么',
]

function createConversationId() {
  return `conv-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function nowLabel() {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date())
}

function appendMessage(message) {
  messages.push({
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    createdAt: nowLabel(),
    raw: '',
    content: '',
    reasoning: '',
    reasoningCollapsed: false,
    status: 'done',
    error: '',
    ...message,
  })
  scrollToBottom()
  return messages[messages.length - 1]
}

function sendSuggestion(text) {
  if (isStreaming.value) return
  draft.value = text
  sendMessage()
}

function sendMessage() {
  const content = draft.value.trim()
  if (!content || isStreaming.value) return

  appendMessage({
    role: 'user',
    content,
  })

  const assistant = appendMessage({
    role: 'assistant',
    status: 'streaming',
  })

  draft.value = ''
  resizeComposer()
  openSse(assistant, content)
}

function openSse(assistant, content) {
  const params = new URLSearchParams({
    conversationId: conversationId.value,
    msg: content,
  })
  const source = new EventSource(`${SSE_ENDPOINT}?${params.toString()}`)

  activeSource.value = source
  activeAssistantId.value = assistant.id

  source.addEventListener('message', (event) => {
    appendChunk(assistant, event.data || '')
  })

  source.onerror = () => {
    const hasOutput = assistant.raw.trim().length > 0
    closeSource()

    if (assistant.status === 'stopped') return

    if (hasOutput) {
      assistant.status = 'done'
    } else {
      assistant.status = 'error'
      assistant.error = 'SSE 连接中断，请确认后端服务已启动。'
    }
  }
}

function appendChunk(assistant, chunk) {
  assistant.raw += chunk
  const parsed = splitReasoning(assistant.raw)
  assistant.reasoning = parsed.reasoning
  assistant.content = parsed.answer
  scrollToBottom()
}

function splitReasoning(raw) {
  const openTag = '<think>'
  const closeTag = '</think>'
  const lower = raw.toLowerCase()
  let cursor = 0
  let answer = ''
  let reasoning = ''
  let thinking = false

  while (cursor < raw.length) {
    const openIndex = lower.indexOf(openTag, cursor)
    if (openIndex === -1) {
      answer += raw.slice(cursor)
      break
    }

    answer += raw.slice(cursor, openIndex)
    const contentStart = openIndex + openTag.length
    const closeIndex = lower.indexOf(closeTag, contentStart)

    if (closeIndex === -1) {
      reasoning += raw.slice(contentStart)
      thinking = true
      break
    }

    reasoning += raw.slice(contentStart, closeIndex)
    cursor = closeIndex + closeTag.length
  }

  return {
    answer: normalizeOutput(answer),
    reasoning: normalizeOutput(reasoning),
    thinking,
  }
}

function normalizeOutput(text) {
  return text.replace(/^\s+/, '').replace(/\s+$/, '')
}

function stopStreaming() {
  const assistant = messages.find((item) => item.id === activeAssistantId.value)
  if (assistant) {
    assistant.status = 'stopped'
    assistant.error = '已停止生成。'
  }
  closeSource()
}

function closeSource() {
  if (activeSource.value) {
    activeSource.value.close()
  }
  activeSource.value = null
  activeAssistantId.value = null
}

function resetConversation() {
  if (isStreaming.value) stopStreaming()
  conversationId.value = createConversationId()
  messages.splice(0, messages.length)
  draft.value = ''
  resizeComposer()
  composerInput.value?.focus()
}

function clearMessages() {
  if (isStreaming.value) stopStreaming()
  messages.splice(0, messages.length)
  composerInput.value?.focus()
}

function toggleReasoning(message) {
  message.reasoningCollapsed = !message.reasoningCollapsed
}

function shouldShowReasoning(message) {
  return message.role === 'assistant' && (message.reasoning || message.status === 'streaming')
}

function reasoningLabel(message) {
  if (message.reasoning) {
    return `${message.reasoning.length} 字`
  }
  if (message.status === 'streaming') {
    return '等待中'
  }
  return '无内容'
}

function reasoningText(message) {
  if (message.reasoning) return message.reasoning
  if (message.status === 'streaming') return '正在等待模型返回推理片段...'
  return '当前响应没有独立的推理片段。'
}

function statusLabel(message) {
  if (message.status === 'streaming') return '生成中'
  if (message.status === 'stopped') return '已停止'
  if (message.status === 'error') return '异常'
  return '完成'
}

function resizeComposer() {
  nextTick(() => {
    const input = composerInput.value
    if (!input) return
    input.style.height = 'auto'
    input.style.height = `${Math.min(input.scrollHeight, 180)}px`
  })
}

function scrollToBottom() {
  nextTick(() => {
    if (!messageList.value) return
    messageList.value.scrollTop = messageList.value.scrollHeight
  })
}

onBeforeUnmount(() => {
  closeSource()
})
</script>

<template>
  <div class="agent-page">
    <header class="topbar">
      <div>
        <p class="eyebrow">Spring AI Agent</p>
        <h1>Agent 聊天工作台</h1>
      </div>

      <div class="topbar-actions">
        <span class="connection-state" :class="{ active: isStreaming }">
          <component :is="statusIcon" aria-hidden="true" />
          {{ statusText }}
        </span>
        <button class="icon-button" type="button" title="新会话" @click="resetConversation">
          <RefreshRight aria-hidden="true" />
        </button>
        <button class="icon-button" type="button" title="清空消息" @click="clearMessages">
          <Delete aria-hidden="true" />
        </button>
      </div>
    </header>

    <main class="chat-shell">
      <section class="chat-panel" aria-label="Agent 对话">
        <div ref="messageList" class="message-list">
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-mark">AI</div>
            <h2>开始一次 Agent 对话</h2>
            <div class="suggestion-row">
              <button
                v-for="suggestion in suggestions"
                :key="suggestion"
                type="button"
                class="suggestion-chip"
                @click="sendSuggestion(suggestion)"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="message.role"
          >
            <div class="avatar" aria-hidden="true">
              {{ message.role === 'user' ? '我' : 'AI' }}
            </div>

            <div class="message-stack">
              <div class="message-meta">
                <span>{{ message.role === 'user' ? '你' : 'Agent' }}</span>
                <span>{{ message.createdAt }}</span>
                <span v-if="message.role === 'assistant'">{{ statusLabel(message) }}</span>
              </div>

              <div class="message-bubble">
                <p v-if="message.role === 'user'" class="plain-text">{{ message.content }}</p>

                <template v-else>
                  <section v-if="shouldShowReasoning(message)" class="reasoning-block">
                    <button
                      type="button"
                      class="reasoning-header"
                      :aria-expanded="!message.reasoningCollapsed"
                      @click="toggleReasoning(message)"
                    >
                      <component
                        :is="message.reasoningCollapsed ? ArrowRight : ArrowDown"
                        aria-hidden="true"
                      />
                      <span>推理过程</span>
                      <small>{{ reasoningLabel(message) }}</small>
                    </button>

                    <Transition name="reasoning">
                      <pre v-if="!message.reasoningCollapsed" class="reasoning-text">{{ reasoningText(message) }}</pre>
                    </Transition>
                  </section>

                  <div v-if="message.content" class="answer-text">{{ message.content }}</div>
                  <div v-else-if="message.status === 'streaming'" class="typing-indicator" aria-label="生成中">
                    <span />
                    <span />
                    <span />
                  </div>
                  <p v-if="message.error" class="message-error">{{ message.error }}</p>
                </template>
              </div>
            </div>
          </article>
        </div>

        <form class="composer" @submit.prevent="sendMessage">
          <div class="conversation-field">
            <label for="conversation-id">会话 ID</label>
            <input id="conversation-id" v-model="conversationId" :disabled="isStreaming" />
          </div>

          <div class="composer-input-row">
            <textarea
              ref="composerInput"
              v-model="draft"
              rows="1"
              placeholder="输入消息，和 Agent 对话"
              :disabled="isStreaming"
              @input="resizeComposer"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <button
              v-if="isStreaming"
              class="icon-button stop-button"
              type="button"
              title="停止生成"
              @click="stopStreaming"
            >
              <Close aria-hidden="true" />
            </button>
            <button class="send-button" type="submit" title="发送" :disabled="!canSend">
              <Promotion aria-hidden="true" />
              <span>发送</span>
            </button>
          </div>
        </form>
      </section>
    </main>
  </div>
</template>
