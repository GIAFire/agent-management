<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatLineRound, Close, Delete, Plus, Promotion } from '@element-plus/icons-vue'
import { chatStream } from '@/axios/chat'

const route = useRoute()
const sessions = ref([])
const activeSessionId = ref('')
const inputMessage = ref('')
const streaming = ref(false)
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

const TYPEWRITER_CHARS_PER_FRAME = 1

const agentInfo = reactive({
  id: null,
  name: '',
  key: ''
})

const agentId = computed(() => Number(route.params.agentId))
const storageKey = computed(() => `agent_chat_sessions_${route.params.agentId || 'unknown'}`)

const activeSession = computed(() => {
  return sessions.value.find((item) => item.id === activeSessionId.value) || null
})

const activeMessages = computed(() => {
  return activeSession.value?.messages || []
})

const canSend = computed(() => {
  return Boolean(inputMessage.value.trim()) && !streaming.value && Number.isFinite(agentId.value)
})

const messageText = (message) => {
  return message.displayContent ?? message.content ?? ''
}

const nowText = () => {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const createSessionId = () => {
  return `${route.params.agentId || 'agent'}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
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

const scrollToBottom = async () => {
  await nextTick()
  const container = messageListRef.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

const scheduleScrollToBottom = () => {
  if (scrollFrame) {
    return
  }

  // 滚动跟随浏览器绘制节奏，减少连续 token 到来时的布局抖动。
  scrollFrame = window.requestAnimationFrame(() => {
    scrollFrame = null
    const container = messageListRef.value
    if (container) {
      container.scrollTop = container.scrollHeight
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
  const session = {
    id: createSessionId(),
    title: getDefaultTitle(),
    updatedAt: nowText(),
    messages: []
  }

  sessions.value.unshift(session)
  activeSessionId.value = session.id
  saveSessions()
  scrollToBottom()
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
        ? session.messages.map((message) => ({
          ...message,
          displayContent: message.displayContent ?? message.content ?? ''
        }))
        : []
    }))
    : []

  if (!sessions.value.length) {
    createSession()
    return
  }

  activeSessionId.value = sessions.value[0].id
  scrollToBottom()
}

const selectSession = (session) => {
  activeSessionId.value = session.id
  scrollToBottom()
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

const appendMessage = (role, content, extra = {}) => {
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
  scrollToBottom()
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
  if (!Number.isFinite(agentId.value)) {
    ElMessage.error('智能体ID无效')
    return
  }

  inputMessage.value = ''
  appendMessage('user', content)
  const assistantMessage = appendMessage('assistant', '', { pending: true, typing: true })
  const session = activeSession.value

  resetTypewriter()
  streaming.value = true
  abortController.value = new AbortController()

  try {
    await chatStream(
      {
        agentId: agentId.value,
        sessionId: session.id,
        content
      },
      {
        onMessage: (delta) => {
          pushTypewriterText(assistantMessage, session, delta)
        },
        onError: (message) => {
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
        onDone: () => {
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

      <div ref="messageListRef" class="message-list">
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
            <div class="message-content">
              {{ messageText(message) || (message.pending ? '思考中...' : '') }}
            </div>
          </div>
        </div>
        <div v-if="!activeMessages.length" class="empty-chat">
          暂无消息
        </div>
      </div>

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

.message-bubble {
  max-width: min(720px, 78%);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px 14px;
  background: var(--surface);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05);
}

.message-row.user .message-bubble {
  border-color: var(--primary);
  color: #ffffff;
  background: var(--primary);
}

.message-row.error .message-bubble {
  border-color: #f56c6c;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
  color: rgba(255, 255, 255, 0.72);
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
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
}
</style>
