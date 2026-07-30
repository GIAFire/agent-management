<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Delete,
  Document,
  Files,
  Refresh,
  Search,
  Tickets,
  UploadFilled,
  View
} from '@element-plus/icons-vue'
import {
  createDocumentIndexTask,
  deleteKnowledgeDocument,
  getKnowledgeBase,
  getKnowledgeTask,
  listDocumentTasks,
  listKnowledgeChunks,
  listKnowledgeDocuments,
  resubmitKnowledgeTask,
  uploadKnowledgeDocument
} from '@/axios/knowledge'

const MAX_FILE_SIZE = 50 * 1024 * 1024
const ALLOWED_EXTENSIONS = ['pdf', 'doc', 'docx', 'txt', 'md']
const ACTIVE_DOCUMENT_STATUSES = ['PENDING', 'PROCESSING', 'PARSING', 'CHUNKING', 'EMBEDDING', 'INDEXING', 'DELETING']
const ACTIVE_TASK_STATUSES = ['PENDING', 'RUNNING']

const route = useRoute()
const router = useRouter()
const knowledgeBaseId = computed(() => String(route.params.knowledgeBaseId || ''))
const knowledgeBase = ref(null)
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const alive = ref(true)
const taskPollers = new Map()
const activeDocumentTasks = reactive({})
let routeGeneration = 0

const query = reactive({
  keyword: '',
  parseStatus: ''
})

const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref()
const selectedFile = ref(null)

const indexDialogVisible = ref(false)
const indexing = ref(false)
const indexDocument = ref(null)
const indexFormRef = ref()
const indexForm = reactive({
  chunkStrategy: 'CHARACTER',
  chunkSize: 1000,
  chunkOverlap: 100,
  chunkDelimiter: '\\n\\n'
})

const taskDrawerVisible = ref(false)
const taskDocument = ref(null)
const taskLoading = ref(false)
const taskRows = ref([])
const taskTotal = ref(0)
const taskCurrent = ref(1)
const taskSize = ref(10)

const chunkDrawerVisible = ref(false)
const chunkDocument = ref(null)
const chunkLoading = ref(false)
const chunkRows = ref([])
const chunkTotal = ref(0)
const chunkCurrent = ref(1)
const chunkSize = ref(10)

const isKnowledgeBaseActive = computed(() => {
  if (!knowledgeBase.value) return false
  const status = String(knowledgeBase.value.status ?? '').toUpperCase()
  return status === '1' || status === 'ENABLED' || status === 'ACTIVE'
})

const normalizePage = (data) => ({
  records: Array.isArray(data?.records) ? data.records : (Array.isArray(data) ? data : []),
  total: Number(data?.total ?? (Array.isArray(data) ? data.length : 0))
})

const parseStatusMeta = (status) => {
  const value = String(status || 'UPLOADED').toUpperCase()
  const map = {
    UPLOADED: { text: '已上传', type: 'info' },
    PENDING: { text: '等待处理', type: 'info' },
    PROCESSING: { text: '处理中', type: 'warning' },
    PARSING: { text: '解析中', type: 'warning' },
    CHUNKING: { text: '切片中', type: 'warning' },
    EMBEDDING: { text: '向量化中', type: 'warning' },
    INDEXING: { text: '入库中', type: 'warning' },
    READY: { text: '已就绪', type: 'success' },
    FAILED: { text: '处理失败', type: 'danger' },
    DELETING: { text: '删除中', type: 'warning' },
    DELETED: { text: '已删除', type: 'info' }
  }
  return map[value] || { text: value, type: 'info' }
}

const taskStatusMeta = (status) => {
  const value = String(status || '').toUpperCase()
  const map = {
    PENDING: { text: '等待执行', type: 'info' },
    RUNNING: { text: '执行中', type: 'warning' },
    SUCCEEDED: { text: '执行成功', type: 'success' },
    FAILED: { text: '执行失败', type: 'danger' }
  }
  return map[value] || { text: value || '-', type: 'info' }
}

const taskTypeText = (type) => {
  const map = {
    INDEX_DOCUMENT: '切片入库',
    DELETE_DOCUMENT: '删除文档',
    DELETE_KNOWLEDGE_BASE: '删除知识库'
  }
  return map[String(type || '').toUpperCase()] || type || '-'
}

const stageText = (stage) => {
  const map = {
    PENDING: '等待执行',
    READING: '读取文档',
    PARSING: '解析文档',
    CHUNKING: '生成切片',
    EMBEDDING: '生成向量',
    INDEXING: '写入向量库',
    SAVING: '保存切片',
    DELETING_VECTORS: '清理向量',
    DELETING_CHUNKS: '清理切片',
    DELETING_SOURCE: '清理源文件',
    FINISHED: '已完成'
  }
  return map[String(stage || '').toUpperCase()] || stage || '-'
}

const strategyText = (strategy) => {
  const map = {
    CHARACTER: '按字符',
    PARAGRAPH: '按段落',
    DELIMITER: '按指定字符'
  }
  return map[String(strategy || '').toUpperCase()] || '-'
}

const formatBytes = (bytes) => {
  const value = Number(bytes || 0)
  if (!value) return '0 B'
  if (value < 1024) return `${value} B`
  if (value < 1024 ** 2) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 ** 2).toFixed(1)} MB`
}

const formatTime = (row) => row.createdAt || row.createTime || row.updatedAt || row.updateTime || '-'

const fileExtension = (name) => {
  const index = String(name || '').lastIndexOf('.')
  return index < 0 ? '' : String(name).slice(index + 1).toLowerCase()
}

const canSubmitIndex = (row) => {
  const status = String(row?.parseStatus || '').toUpperCase()
  return (
    isKnowledgeBaseActive.value &&
    !activeDocumentTasks[String(row?.id)] &&
    ['UPLOADED', 'FAILED'].includes(status)
  )
}

const canDeleteDocument = (row) => {
  const status = String(row?.parseStatus || '').toUpperCase()
  return (
    isKnowledgeBaseActive.value &&
    !activeDocumentTasks[String(row?.id)] &&
    !ACTIVE_DOCUMENT_STATUSES.includes(status)
  )
}

const loadKnowledgeBase = async (generation = routeGeneration) => {
  const targetKnowledgeBaseId = knowledgeBaseId.value
  const data = await getKnowledgeBase(targetKnowledgeBaseId)
  if (
    generation === routeGeneration &&
    targetKnowledgeBaseId === knowledgeBaseId.value
  ) {
    knowledgeBase.value = data
  }
}

const hydrateRunningTasks = async (documents, generation = routeGeneration) => {
  const activeDocuments = documents.filter((row) =>
    ACTIVE_DOCUMENT_STATUSES.includes(String(row.parseStatus || '').toUpperCase())
  )
  await Promise.all(activeDocuments.map(async (row) => {
    try {
      const data = await listDocumentTasks(row.id, { current: 1, size: 1 })
      if (generation !== routeGeneration) return
      const latestTask = normalizePage(data).records[0]
      if (latestTask && ACTIVE_TASK_STATUSES.includes(String(latestTask.status || '').toUpperCase())) {
        scheduleTaskPoll(latestTask.id, row.id)
      }
    } catch {
      // The main document request already reports errors; task hydration is best effort.
    }
  }))
}

const loadDocuments = async (generation = routeGeneration) => {
  const targetKnowledgeBaseId = knowledgeBaseId.value
  loading.value = true
  try {
    const data = await listKnowledgeDocuments(targetKnowledgeBaseId, {
      current: current.value,
      size: size.value,
      keyword: query.keyword.trim() || undefined,
      parseStatus: query.parseStatus || undefined
    })
    if (
      generation !== routeGeneration ||
      targetKnowledgeBaseId !== knowledgeBaseId.value
    ) {
      return
    }
    const page = normalizePage(data)
    rows.value = page.records
    total.value = page.total
    if (!rows.value.length && total.value > 0 && current.value > 1) {
      current.value -= 1
      await loadDocuments(generation)
      return
    }
    await hydrateRunningTasks(rows.value, generation)
  } finally {
    if (generation === routeGeneration) {
      loading.value = false
    }
  }
}

const reloadPage = async (generation = routeGeneration) => {
  await Promise.all([
    loadKnowledgeBase(generation),
    loadDocuments(generation)
  ])
}

const handleSearch = () => {
  current.value = 1
  loadDocuments()
}

const resetSearch = () => {
  query.keyword = ''
  query.parseStatus = ''
  current.value = 1
  loadDocuments()
}

const openUpload = async () => {
  if (!isKnowledgeBaseActive.value) {
    ElMessage.warning('知识库已停用或正在删除，不能上传文档')
    return
  }
  selectedFile.value = null
  uploadDialogVisible.value = true
  await nextTick()
  uploadRef.value?.clearFiles()
}

const validateFile = (file) => {
  const extension = fileExtension(file.name)
  if (!ALLOWED_EXTENSIONS.includes(extension)) {
    ElMessage.error('仅支持 PDF、DOC、DOCX、TXT、MD 文件')
    return false
  }
  if (Number(file.size) > MAX_FILE_SIZE) {
    ElMessage.error('单个文件不能超过 50 MB')
    return false
  }
  if (Number(file.size) <= 0) {
    ElMessage.error('不能上传空文件')
    return false
  }
  return true
}

const handleFileChange = (uploadFile) => {
  const raw = uploadFile?.raw
  if (!raw || !validateFile(raw)) {
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    return
  }
  selectedFile.value = raw
}

const handleFileRemove = () => {
  selectedFile.value = null
}

const handleFileExceed = (files) => {
  const file = files[0]
  uploadRef.value?.clearFiles()
  if (!file || !validateFile(file)) return
  uploadRef.value?.handleStart(file)
}

const submitUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择一个文件')
    return
  }
  if (!validateFile(selectedFile.value)) return

  const generation = routeGeneration
  const targetKnowledgeBaseId = knowledgeBaseId.value
  uploading.value = true
  try {
    await uploadKnowledgeDocument(targetKnowledgeBaseId, selectedFile.value)
    if (generation !== routeGeneration) return
    uploadDialogVisible.value = false
    ElMessage.success('文档上传成功，请提交切片入库任务')
    await loadDocuments()
  } finally {
    uploading.value = false
  }
}

const escapeDelimiterForInput = (value) => {
  return String(value ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n')
    .replace(/\t/g, '\\t')
}

const decodeDelimiterInput = (rawValue) => {
  const raw = String(rawValue ?? '')
  if (!raw.trim()) {
    return { error: '指定字符切片必须填写分隔标识' }
  }

  let decoded = ''
  for (let index = 0; index < raw.length; index += 1) {
    const currentCharacter = raw[index]
    if (currentCharacter !== '\\') {
      decoded += currentCharacter
      continue
    }

    index += 1
    if (index >= raw.length) {
      return { error: '分隔标识包含未完成的转义' }
    }

    const escapedCharacter = raw[index]
    const escapeMap = {
      n: '\n',
      r: '\r',
      t: '\t',
      '\\': '\\'
    }
    if (!Object.prototype.hasOwnProperty.call(escapeMap, escapedCharacter)) {
      return { error: '分隔标识仅支持 \\n、\\r、\\t、\\\\ 转义' }
    }
    decoded += escapeMap[escapedCharacter]
  }

  if (decoded.length < 1 || decoded.length > 32) {
    return { error: '分隔标识解码后的长度应为 1–32 个字符' }
  }
  return { value: decoded }
}

const openIndexDialog = async (row) => {
  if (!canSubmitIndex(row)) {
    ElMessage.warning('仅已上传或处理失败的文档可以提交切片任务')
    return
  }
  indexDocument.value = row
  Object.assign(indexForm, {
    chunkStrategy: row.chunkStrategy || 'CHARACTER',
    chunkSize: Number(row.chunkSize || 1000),
    chunkOverlap: Number(row.chunkOverlap ?? 100),
    chunkDelimiter: row.chunkDelimiter == null
      ? '\\n\\n'
      : escapeDelimiterForInput(row.chunkDelimiter)
  })
  indexDialogVisible.value = true
  await nextTick()
  indexFormRef.value?.clearValidate()
}

const validateChunkForm = () => {
  if (indexForm.chunkStrategy === 'DELIMITER') {
    const delimiterResult = decodeDelimiterInput(indexForm.chunkDelimiter)
    if (delimiterResult.error) {
      ElMessage.error(delimiterResult.error)
      return false
    }
    return true
  }

  const chunkSizeValue = Number(indexForm.chunkSize)
  const overlapValue = Number(indexForm.chunkOverlap)
  if (chunkSizeValue < 200 || chunkSizeValue > 4000) {
    ElMessage.error('切片长度应为 200–4000 个字符')
    return false
  }
  if (overlapValue < 0 || overlapValue > 500 || overlapValue >= chunkSizeValue) {
    ElMessage.error('重叠长度应为 0–500，且必须小于切片长度')
    return false
  }
  return true
}

const submitIndexTask = async () => {
  if (!validateChunkForm()) return
  const generation = routeGeneration
  const documentId = indexDocument.value?.id
  if (!documentId) return
  indexing.value = true
  try {
    const payload = {
      chunkStrategy: indexForm.chunkStrategy
    }
    if (indexForm.chunkStrategy === 'DELIMITER') {
      payload.chunkDelimiter = indexForm.chunkDelimiter
    } else {
      payload.chunkSize = Number(indexForm.chunkSize)
      payload.chunkOverlap = Number(indexForm.chunkOverlap)
    }
    const task = await createDocumentIndexTask(documentId, payload)
    if (generation !== routeGeneration) return
    indexDialogVisible.value = false
    ElMessage.success('切片入库任务已提交')
    scheduleTaskPoll(task.id, documentId)
    await loadDocuments(generation)
  } finally {
    indexing.value = false
  }
}

const handleDeleteDocument = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除文档“${row.documentName}”吗？系统将异步清理其向量、切片和源文件，完成后同名文件才可重新上传。`,
      '删除文档',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  const generation = routeGeneration
  const task = await deleteKnowledgeDocument(row.id)
  if (generation !== routeGeneration) return
  ElMessage.success('文档删除任务已提交')
  scheduleTaskPoll(task.id, row.id)
  await loadDocuments()
}

const stopTaskPoller = (key) => {
  const poller = taskPollers.get(key)
  if (poller?.timerId) {
    window.clearTimeout(poller.timerId)
  }
  taskPollers.delete(key)
  if (
    poller?.documentId &&
    activeDocumentTasks[String(poller.documentId)] === key
  ) {
    delete activeDocumentTasks[String(poller.documentId)]
  }
}

const clearTaskPollers = () => {
  taskPollers.forEach((poller) => {
    if (poller.timerId) {
      window.clearTimeout(poller.timerId)
    }
  })
  taskPollers.clear()
  Object.keys(activeDocumentTasks).forEach((documentId) => {
    delete activeDocumentTasks[documentId]
  })
}

const scheduleTaskPoll = (taskId, documentId) => {
  const key = String(taskId)
  if (!taskId || taskPollers.has(key)) return
  const generation = routeGeneration
  const poller = {
    timerId: null,
    consecutiveFailures: 0,
    documentId: String(documentId)
  }
  taskPollers.set(key, poller)
  activeDocumentTasks[String(documentId)] = key

  const scheduleNext = (delay) => {
    if (
      !alive.value ||
      generation !== routeGeneration ||
      !taskPollers.has(key)
    ) {
      return
    }
    poller.timerId = window.setTimeout(poll, delay)
  }

  const poll = async () => {
    if (
      !alive.value ||
      generation !== routeGeneration ||
      !taskPollers.has(key)
    ) {
      stopTaskPoller(key)
      return
    }
    poller.timerId = null
    try {
      const task = await getKnowledgeTask(taskId, { skipErrorMessage: true })
      if (
        generation !== routeGeneration ||
        !taskPollers.has(key)
      ) {
        return
      }
      poller.consecutiveFailures = 0
      const index = taskRows.value.findIndex((item) => String(item.id) === key)
      if (index >= 0) {
        taskRows.value.splice(index, 1, task)
      }

      const status = String(task?.status || '').toUpperCase()
      if (status === 'SUCCEEDED') {
        stopTaskPoller(key)
        ElMessage.success(`${taskTypeText(task.taskType)}任务执行成功`)
        await loadDocuments(generation)
        if (taskDrawerVisible.value && String(taskDocument.value?.id) === String(documentId)) {
          await loadTasks()
        }
        return
      }
      if (status === 'FAILED') {
        stopTaskPoller(key)
        ElMessage.error(task.errorMessage || `${taskTypeText(task.taskType)}任务执行失败`)
        await loadDocuments(generation)
        if (taskDrawerVisible.value && String(taskDocument.value?.id) === String(documentId)) {
          await loadTasks()
        }
        return
      }
    } catch (error) {
      const statusCode = Number(error?.response?.status || 0)
      const taskMissing = String(error?.message || '').includes('知识任务不存在')
      if ([401, 403, 404].includes(statusCode) || taskMissing) {
        stopTaskPoller(key)
        return
      }
      poller.consecutiveFailures += 1
      const retryDelay = Math.min(
        2500 * (2 ** Math.min(poller.consecutiveFailures - 1, 4)),
        30000
      )
      scheduleNext(retryDelay)
      return
    }

    scheduleNext(2500)
  }
  poll()
}

const openTaskDrawer = async (row) => {
  taskDocument.value = row
  taskCurrent.value = 1
  taskDrawerVisible.value = true
  await loadTasks()
}

const loadTasks = async () => {
  if (!taskDocument.value?.id) return
  const generation = routeGeneration
  const documentId = String(taskDocument.value.id)
  taskLoading.value = true
  try {
    const data = await listDocumentTasks(documentId, {
      current: taskCurrent.value,
      size: taskSize.value
    })
    if (
      generation !== routeGeneration ||
      documentId !== String(taskDocument.value?.id || '')
    ) {
      return
    }
    const page = normalizePage(data)
    taskRows.value = page.records
    taskTotal.value = page.total
    taskRows.value.forEach((task) => {
      if (ACTIVE_TASK_STATUSES.includes(String(task.status || '').toUpperCase())) {
        scheduleTaskPoll(task.id, task.documentId || taskDocument.value.id)
      }
    })
  } finally {
    if (generation === routeGeneration) {
      taskLoading.value = false
    }
  }
}

const retryTask = async (task) => {
  const generation = routeGeneration
  const newTask = await resubmitKnowledgeTask(task.id)
  if (generation !== routeGeneration) return
  ElMessage.success('任务已重新提交，原失败记录将保留')
  scheduleTaskPoll(newTask.id, newTask.documentId || task.documentId)
  await Promise.all([loadTasks(), loadDocuments()])
}

const openChunkDrawer = async (row) => {
  chunkDocument.value = row
  chunkCurrent.value = 1
  chunkDrawerVisible.value = true
  await loadChunks()
}

const loadChunks = async () => {
  if (!chunkDocument.value?.id) return
  const generation = routeGeneration
  const documentId = String(chunkDocument.value.id)
  chunkLoading.value = true
  try {
    const data = await listKnowledgeChunks(documentId, {
      current: chunkCurrent.value,
      size: chunkSize.value
    })
    if (
      generation !== routeGeneration ||
      documentId !== String(chunkDocument.value?.id || '')
    ) {
      return
    }
    const page = normalizePage(data)
    chunkRows.value = page.records
    chunkTotal.value = page.total
  } finally {
    if (generation === routeGeneration) {
      chunkLoading.value = false
    }
  }
}

const handleTaskSizeChange = () => {
  taskCurrent.value = 1
  loadTasks()
}

const handleChunkSizeChange = () => {
  chunkCurrent.value = 1
  loadChunks()
}

const resetRouteState = () => {
  clearTaskPollers()
  knowledgeBase.value = null
  rows.value = []
  total.value = 0
  current.value = 1
  size.value = 10
  query.keyword = ''
  query.parseStatus = ''
  loading.value = false

  uploadDialogVisible.value = false
  uploading.value = false
  selectedFile.value = null
  uploadRef.value?.clearFiles()

  indexDialogVisible.value = false
  indexing.value = false
  indexDocument.value = null
  Object.assign(indexForm, {
    chunkStrategy: 'CHARACTER',
    chunkSize: 1000,
    chunkOverlap: 100,
    chunkDelimiter: '\\n\\n'
  })

  taskDrawerVisible.value = false
  taskDocument.value = null
  taskRows.value = []
  taskTotal.value = 0
  taskCurrent.value = 1
  taskSize.value = 10
  taskLoading.value = false

  chunkDrawerVisible.value = false
  chunkDocument.value = null
  chunkRows.value = []
  chunkTotal.value = 0
  chunkCurrent.value = 1
  chunkSize.value = 10
  chunkLoading.value = false
}

onMounted(async () => {
  await reloadPage()
})

watch(knowledgeBaseId, async (nextKnowledgeBaseId, previousKnowledgeBaseId) => {
  if (nextKnowledgeBaseId === previousKnowledgeBaseId) return
  routeGeneration += 1
  const generation = routeGeneration
  resetRouteState()
  if (nextKnowledgeBaseId) {
    await reloadPage(generation)
  }
})

onBeforeUnmount(() => {
  alive.value = false
  routeGeneration += 1
  clearTaskPollers()
})
</script>

<template>
  <section class="documents-page">
    <header class="documents-hero">
      <el-button class="back-button" :icon="ArrowLeft" circle @click="router.push('/agent/knowledge')" />
      <div class="hero-copy">
        <span class="hero-kicker">KNOWLEDGE DOCUMENTS</span>
        <h2>{{ knowledgeBase?.knowledgeName || '知识库文档' }}</h2>
        <p>{{ knowledgeBase?.description || '上传源文档，并显式提交切片入库任务。' }}</p>
        <div class="base-meta" v-if="knowledgeBase">
          <el-tag :type="isKnowledgeBaseActive ? 'success' : 'info'">
            {{ isKnowledgeBaseActive ? '已启用' : '不可操作' }}
          </el-tag>
          <span>{{ knowledgeBase.embeddingModelName }}</span>
          <span>{{ knowledgeBase.embeddingDimension }} 维 · {{ knowledgeBase.metricType }}</span>
          <span>TopK {{ knowledgeBase.topK }} · 阈值 {{ knowledgeBase.scoreThreshold }}</span>
        </div>
      </div>
      <div class="hero-actions">
        <el-button :icon="Refresh" @click="reloadPage">刷新</el-button>
        <el-button
          type="primary"
          :icon="UploadFilled"
          :disabled="!isKnowledgeBaseActive"
          @click="openUpload"
        >
          上传文档
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="!isKnowledgeBaseActive"
      title="知识库已停用或正在删除，当前仅允许查看已有文档、任务和切片。"
      type="warning"
      :closable="false"
      show-icon
    />

    <div class="query-bar">
      <el-form class="query-form" inline @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="文档名称"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="query.parseStatus" clearable placeholder="全部状态">
            <el-option label="已上传" value="UPLOADED" />
            <el-option label="等待处理" value="PENDING" />
            <el-option label="解析中" value="PARSING" />
            <el-option label="切片中" value="CHUNKING" />
            <el-option label="向量化中" value="EMBEDDING" />
            <el-option label="入库中" value="INDEXING" />
            <el-option label="已就绪" value="READY" />
            <el-option label="处理失败" value="FAILED" />
            <el-option label="删除中" value="DELETING" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-panel">
      <div class="table-toolbar">
        <div class="table-title">
          <h2>文档列表</h2>
          <span>共 {{ total }} 个文档；上传后需单独提交切片入库任务</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe class="data-table">
        <el-table-column label="文档" min-width="260">
          <template #default="{ row }">
            <div class="document-cell">
              <span><el-icon><Document /></el-icon></span>
              <span>
                <strong>{{ row.documentName }}</strong>
                <small>{{ String(row.documentType || fileExtension(row.documentName)).toUpperCase() }} · {{ formatBytes(row.sizeBytes) }}</small>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="125">
          <template #default="{ row }">
            <el-tooltip
              :disabled="!row.errorMessage"
              :content="row.errorMessage"
              placement="top"
            >
              <el-tag :type="parseStatusMeta(row.parseStatus).type">
                {{ parseStatusMeta(row.parseStatus).text }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="切片配置" min-width="180">
          <template #default="{ row }">
            <div v-if="row.chunkStrategy" class="chunk-config">
              <strong>{{ strategyText(row.chunkStrategy) }}</strong>
              <span v-if="row.chunkStrategy === 'DELIMITER'">
                标识 {{ escapeDelimiterForInput(row.chunkDelimiter) }}
              </span>
              <span v-else>{{ row.chunkSize }} / 重叠 {{ row.chunkOverlap }}</span>
            </div>
            <span v-else class="muted-text">尚未配置</span>
          </template>
        </el-table-column>
        <el-table-column label="切片数" width="105" align="center">
          <template #default="{ row }">{{ Number(row.chunkCount || 0) }}</template>
        </el-table-column>
        <el-table-column label="Token 数" width="115" align="center">
          <template #default="{ row }">{{ Number(row.tokenCount || 0) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :icon="Tickets"
              :disabled="!canSubmitIndex(row)"
              @click="openIndexDialog(row)"
            >
              切片入库
            </el-button>
            <el-button link type="primary" :icon="Files" @click="openTaskDrawer(row)">任务</el-button>
            <el-button
              link
              type="primary"
              :icon="View"
              :disabled="String(row.parseStatus).toUpperCase() !== 'READY'"
              @click="openChunkDrawer(row)"
            >
              切片
            </el-button>
            <el-button
              link
              type="danger"
              :icon="Delete"
              :disabled="!canDeleteDocument(row)"
              @click="handleDeleteDocument(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无文档，请先上传源文件" />
        </template>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadDocuments"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <el-dialog v-model="uploadDialogVisible" title="上传知识文档" width="600px" destroy-on-close>
      <el-alert
        title="同一知识库中不允许同名文档。如需替换，请先等待原文档删除完成。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-upload
        ref="uploadRef"
        class="document-uploader"
        drag
        action="#"
        accept=".pdf,.doc,.docx,.txt,.md"
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :on-exceed="handleFileExceed"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">
            单次上传一个文件，仅支持 PDF、DOC、DOCX、TXT、MD，最大 50 MB。扫描版或加密 PDF 不支持 OCR。
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="submitUpload">
          上传
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="indexDialogVisible" title="提交切片入库任务" width="650px" destroy-on-close>
      <div class="selected-document">
        <el-icon><Document /></el-icon>
        <span>
          <strong>{{ indexDocument?.documentName }}</strong>
          <small>本次提交会快照到任务；执行成功后才保存为文档当前配置。</small>
        </span>
      </div>
      <el-form ref="indexFormRef" :model="indexForm" label-position="top" class="index-form">
        <el-form-item label="切片策略">
          <el-radio-group v-model="indexForm.chunkStrategy">
            <el-radio-button value="CHARACTER">按字符</el-radio-button>
            <el-radio-button value="PARAGRAPH">按段落</el-radio-button>
            <el-radio-button value="DELIMITER">按指定字符</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="indexForm.chunkStrategy !== 'DELIMITER'">
          <div class="index-grid">
            <el-form-item :label="indexForm.chunkStrategy === 'PARAGRAPH' ? '每片最大字符数' : '切片字符数'">
              <el-input-number
                v-model="indexForm.chunkSize"
                :min="200"
                :max="4000"
                :step="100"
                controls-position="right"
              />
              <span class="field-help">范围 200–4000，默认 1000</span>
            </el-form-item>
            <el-form-item label="重叠字符数">
              <el-input-number
                v-model="indexForm.chunkOverlap"
                :min="0"
                :max="500"
                :step="10"
                controls-position="right"
              />
              <span class="field-help">范围 0–500，且小于切片长度</span>
            </el-form-item>
          </div>
        </template>

        <el-form-item v-else label="分隔标识">
          <el-input
            v-model="indexForm.chunkDelimiter"
            maxlength="64"
            placeholder="例如：\n\n、\t、###"
          />
          <span class="field-help">
            按字面值切分，支持 \n、\r、\t、\\ 转义；标识不会写入切片，空片段会被忽略。单片超过 4000 字符则任务失败。
          </span>
        </el-form-item>

        <el-alert
          title="提交后不会自动重试。失败原因会直接展示，你可以从任务记录中手动重新提交。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="indexDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="indexing" @click="submitIndexTask">提交任务</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="taskDrawerVisible" size="760px" destroy-on-close>
      <template #header>
        <div class="drawer-heading">
          <strong>任务记录</strong>
          <span>{{ taskDocument?.documentName }}</span>
        </div>
      </template>
      <el-table v-loading="taskLoading" :data="taskRows" stripe>
        <el-table-column label="任务" min-width="135">
          <template #default="{ row }">
            <div class="task-name">
              <strong>{{ taskTypeText(row.taskType) }}</strong>
              <span>#{{ row.id }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="taskStatusMeta(row.status).type">{{ taskStatusMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(row.progress || 0)"
              :status="String(row.status).toUpperCase() === 'FAILED' ? 'exception' : (String(row.status).toUpperCase() === 'SUCCEEDED' ? 'success' : undefined)"
              :stroke-width="7"
            />
            <small class="stage-text">{{ stageText(row.stage) }}</small>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="165">
          <template #default="{ row }">{{ formatTime(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="95" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="String(row.status).toUpperCase() === 'FAILED'"
              link
              type="danger"
              :disabled="!isKnowledgeBaseActive"
              @click="retryTask(row)"
            >
              重新提交
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无任务记录" /></template>
      </el-table>

      <div v-for="task in taskRows.filter((item) => item.errorMessage)" :key="`error-${task.id}`" class="task-error">
        <strong>{{ taskTypeText(task.taskType) }}失败原因</strong>
        <p>{{ task.errorMessage }}</p>
      </div>

      <div class="drawer-pagination">
        <el-pagination
          v-model:current-page="taskCurrent"
          v-model:page-size="taskSize"
          :total="taskTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadTasks"
          @size-change="handleTaskSizeChange"
        />
      </div>
    </el-drawer>

    <el-drawer v-model="chunkDrawerVisible" size="850px" destroy-on-close>
      <template #header>
        <div class="drawer-heading">
          <strong>文档切片</strong>
          <span>{{ chunkDocument?.documentName }} · 共 {{ chunkTotal }} 片</span>
        </div>
      </template>
      <el-table v-loading="chunkLoading" :data="chunkRows" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="chunk-content">
              <strong>切片正文</strong>
              <pre>{{ row.content }}</pre>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="chunkIndex" label="序号" width="80" />
        <el-table-column label="内容预览" min-width="330" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content }}</template>
        </el-table-column>
        <el-table-column prop="pageNo" label="页码" width="80">
          <template #default="{ row }">{{ row.pageNo ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="sectionTitle" label="章节" min-width="130" show-overflow-tooltip />
        <el-table-column prop="tokenCount" label="Token" width="90" />
        <template #empty><el-empty description="暂无切片" /></template>
      </el-table>
      <div class="drawer-pagination">
        <el-pagination
          v-model:current-page="chunkCurrent"
          v-model:page-size="chunkSize"
          :total="chunkTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadChunks"
          @size-change="handleChunkSizeChange"
        />
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.documents-page {
  display: grid;
  gap: 18px;
  padding: 22px 0 28px;
}

.documents-hero {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: start;
  gap: 14px;
}

.back-button {
  margin-top: 22px;
}

.hero-copy {
  min-width: 0;
}

.hero-kicker {
  color: #2f75ff;
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.12em;
}

.hero-copy h2 {
  margin: 5px 0 7px;
  color: #071f40;
  font-size: 28px;
}

.hero-copy > p {
  max-width: 760px;
  margin: 0;
  overflow: hidden;
  color: #5d718c;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.base-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  margin-top: 12px;
  color: #607590;
  font-size: 12px;
}

.hero-actions {
  display: flex;
  gap: 8px;
  margin-top: 22px;
}

.document-cell,
.selected-document {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.document-cell > span:first-child,
.selected-document > .el-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 10px;
  color: #2f75ff;
  background: #eaf2ff;
  font-size: 20px;
}

.document-cell strong,
.document-cell small,
.selected-document strong,
.selected-document small,
.chunk-config strong,
.chunk-config span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-cell strong,
.selected-document strong,
.chunk-config strong {
  color: #183553;
  font-size: 13px;
}

.document-cell small,
.selected-document small,
.chunk-config span,
.muted-text {
  margin-top: 5px;
  color: #7588a1;
  font-size: 12px;
}

.document-uploader {
  margin-top: 18px;
}

.document-uploader :deep(.el-upload-dragger) {
  padding: 34px 20px;
}

.selected-document {
  margin-bottom: 20px;
  padding: 12px;
  border: 1px solid #dce8f7;
  border-radius: 11px;
  background: #f8fbff;
}

.index-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.index-form :deep(.el-input-number) {
  width: 100%;
}

.field-help {
  display: block;
  width: 100%;
  margin-top: 6px;
  color: #7a8da7;
  font-size: 12px;
  line-height: 1.5;
}

.drawer-heading strong,
.drawer-heading span {
  display: block;
}

.drawer-heading strong {
  color: #0a2547;
  font-size: 17px;
}

.drawer-heading span {
  max-width: 620px;
  margin-top: 4px;
  overflow: hidden;
  color: #72859d;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-name strong,
.task-name span {
  display: block;
}

.task-name strong {
  color: #173653;
  font-size: 13px;
}

.task-name span,
.stage-text {
  margin-top: 4px;
  color: #7a8da7;
  font-size: 11px;
}

.task-error {
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid #ffd6d6;
  border-radius: 9px;
  color: #a73535;
  background: #fff6f6;
}

.task-error strong {
  font-size: 13px;
}

.task-error p {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.drawer-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.chunk-content {
  padding: 6px 24px 14px 62px;
}

.chunk-content strong {
  color: #24415e;
  font-size: 13px;
}

.chunk-content pre {
  max-height: 360px;
  overflow: auto;
  margin: 10px 0 0;
  padding: 14px;
  border-radius: 8px;
  color: #304b67;
  background: #f5f8fc;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 800px) {
  .documents-hero {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .hero-actions {
    grid-column: 2;
    margin-top: 0;
  }

  .index-grid {
    grid-template-columns: 1fr;
  }
}
</style>
