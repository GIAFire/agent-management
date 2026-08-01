<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Collection,
  Delete,
  Document,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  SwitchButton,
  View
} from '@element-plus/icons-vue'
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  getKnowledgeBase,
  getKnowledgeMetrics,
  getKnowledgeTask,
  listRecentKnowledgeFailures,
  listKnowledgeBases,
  resubmitKnowledgeTask,
  updateKnowledgeBase
} from '@/axios/knowledge'
import { getUser } from '@/utils/auth'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const dialogMode = ref('create')
const rows = ref([])
const total = ref(0)
const metrics = ref({})
const recentFailures = ref([])
const current = ref(1)
const size = ref(12)
const alive = ref(true)
const taskPollers = new Map()
const deletingTasks = reactive({})

const deletionStorageKey = () => {
  const user = getUser() || {}
  const tenantIdentity = user.tenantId || user.tenant?.id || user.tenantCode || 'unknownTenant'
  const userIdentity = user.id || user.userId || user.username || 'unknownUser'
  return `knowledge_delete_tasks:${tenantIdentity}:${userIdentity}`
}

const query = reactive({
  keyword: '',
  status: ''
})

const form = reactive({
  id: '',
  knowledgeName: '',
  description: '',
  modelUrl: '',
  apiKey: '',
  embeddingModelName: '',
  embeddingDimension: 1024,
  metricType: 'COSINE',
  topK: 5,
  scoreThreshold: 0.5,
  status: 1
})

const isEdit = computed(() => dialogMode.value === 'edit')
const dialogTitle = computed(() => isEdit.value ? '编辑知识库' : '创建知识库')

const formRules = computed(() => ({
  knowledgeName: [
    { required: true, whitespace: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 1, max: 100, message: '名称长度应为 1–100 个字符', trigger: 'blur' }
  ],
  modelUrl: [
    { required: true, whitespace: true, message: '请输入 Embedding API 地址', trigger: 'blur' },
    { type: 'url', message: '请输入有效的 URL', trigger: 'blur' }
  ],
  apiKey: isEdit.value ? [] : [
    { required: true, whitespace: true, message: '请输入真实 API Key', trigger: 'blur' }
  ],
  embeddingModelName: [
    { required: true, whitespace: true, message: '请输入 Embedding 模型名', trigger: 'blur' }
  ],
  embeddingDimension: [
    { required: true, message: '请输入向量维度', trigger: 'change' }
  ],
  metricType: [
    { required: true, message: '请选择度量方式', trigger: 'change' }
  ],
  topK: [
    { required: true, message: '请输入 TopK', trigger: 'change' }
  ],
  scoreThreshold: [
    { required: true, message: '请输入相似度阈值', trigger: 'change' }
  ]
}))

const normalizePage = (data) => ({
  records: Array.isArray(data?.records) ? data.records : (Array.isArray(data) ? data : []),
  total: Number(data?.total ?? (Array.isArray(data) ? data.length : 0))
})

const normalizeStatus = (status) => {
  const value = String(status ?? '').toUpperCase()
  if (value === '2' || value === 'DELETING') {
    return 2
  }
  return value === '0' || value === 'DISABLED' ? 0 : 1
}

const statusMeta = (status) => {
  const value = normalizeStatus(status)
  if (value === 2) {
    return { text: '删除中', type: 'warning' }
  }
  if (value === 0) {
    return { text: '已停用', type: 'info' }
  }
  return { text: '已启用', type: 'success' }
}

const taskStatusMeta = (status) => {
  const value = String(status || '').toUpperCase()
  if (value === 'FAILED') return { text: '删除失败', type: 'danger' }
  if (value === 'SUCCEEDED') return { text: '删除完成', type: 'success' }
  if (value === 'RUNNING') return { text: '删除中', type: 'warning' }
  return { text: '等待删除', type: 'info' }
}

const formatTime = (row) => row.updatedAt || row.updateTime || row.createdAt || row.createTime || '-'

const formatNumber = value => Number(value || 0).toLocaleString('zh-CN')

const knowledgeMetricCards = computed(() => [
  { label: '知识库总数', value: formatNumber(metrics.value.totalKnowledgeBases), note: `已启用 ${formatNumber(metrics.value.enabledKnowledgeBases)}`, icon: Collection, tone: 'blue' },
  { label: '文档总数', value: formatNumber(metrics.value.totalDocuments), note: `今日新增 ${formatNumber(metrics.value.newDocumentsToday)}`, icon: Document, tone: 'violet' },
  { label: '可检索文档', value: formatNumber(metrics.value.readyDocuments), note: metrics.value.documentReadyRate == null ? '暂无文档' : `就绪率 ${metrics.value.documentReadyRate}%`, icon: View, tone: 'green' },
  { label: '已索引内容', value: formatNumber(metrics.value.totalChunks), note: `${formatNumber(metrics.value.totalTokens)} Token`, icon: Search, tone: 'amber' }
])

const loadKnowledgeOverview = async () => {
  const [metricResult, failureResult] = await Promise.all([
    getKnowledgeMetrics(),
    listRecentKnowledgeFailures(5)
  ])
  metrics.value = metricResult || {}
  recentFailures.value = Array.isArray(failureResult) ? failureResult : []
}

const retryRecentFailure = async task => {
  await resubmitKnowledgeTask(task.id)
  ElMessage.success('任务已重新提交')
  await loadKnowledgeOverview()
}

const resetForm = () => {
  Object.assign(form, {
    id: '',
    knowledgeName: '',
    description: '',
    modelUrl: '',
    apiKey: '',
    embeddingModelName: '',
    embeddingDimension: 1024,
    metricType: 'COSINE',
    topK: 5,
    scoreThreshold: 0.5,
    status: 1
  })
}

const loadRows = async () => {
  loading.value = true
  try {
    const data = await listKnowledgeBases({
      current: current.value,
      size: size.value,
      keyword: query.keyword.trim() || undefined,
      status: query.status === '' ? undefined : query.status
    })
    const page = normalizePage(data)
    rows.value = page.records
    total.value = page.total

    if (!rows.value.length && total.value > 0 && current.value > 1) {
      current.value -= 1
      await loadRows()
    }

    rows.value.forEach((row) => {
      const taskId = row.latestTaskId || row.deleteTaskId
      const taskStatus = row.latestTaskStatus || row.deleteTaskStatus
      const normalizedTaskStatus = String(taskStatus || '').toUpperCase()
      if (
        normalizeStatus(row.status) === 2 &&
        taskId &&
        ['PENDING', 'RUNNING', 'FAILED'].includes(normalizedTaskStatus)
      ) {
        deletingTasks[String(row.id)] = {
          id: String(taskId),
          status: taskStatus,
          progress: Number(row.latestTaskProgress || 0),
          errorMessage: row.latestTaskError || ''
        }
        if (['PENDING', 'RUNNING'].includes(normalizedTaskStatus)) {
          scheduleTaskPoll(taskId, row.id)
        }
      }
    })
  } finally {
    loading.value = false
  }
}

const loadPage = async () => {
  await Promise.all([loadRows(), loadKnowledgeOverview()])
}

const handleSearch = () => {
  current.value = 1
  loadRows()
}

const openCreate = async () => {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async (row) => {
  dialogMode.value = 'edit'
  resetForm()
  const data = await getKnowledgeBase(row.id)
  Object.assign(form, {
    id: String(data.id),
    knowledgeName: data.knowledgeName || '',
    description: data.description || '',
    modelUrl: data.modelUrl || '',
    apiKey: '',
    embeddingModelName: data.embeddingModelName || '',
    embeddingDimension: Number(data.embeddingDimension || 1024),
    metricType: data.metricType || 'COSINE',
    topK: Number(data.topK || 5),
    scoreThreshold: Number(data.scoreThreshold ?? 0.5),
    status: normalizeStatus(data.status)
  })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const buildPayload = () => {
  const payload = {
    knowledgeName: form.knowledgeName.trim(),
    description: form.description.trim(),
    modelUrl: form.modelUrl.trim(),
    topK: Number(form.topK),
    scoreThreshold: Number(form.scoreThreshold)
  }

  if (form.apiKey.trim()) {
    payload.apiKey = form.apiKey.trim()
  }

  if (isEdit.value) {
    payload.status = Number(form.status)
  } else {
    payload.embeddingModelName = form.embeddingModelName.trim()
    payload.embeddingDimension = Number(form.embeddingDimension)
    payload.metricType = form.metricType
  }
  return payload
}

const submitForm = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateKnowledgeBase(form.id, buildPayload())
      ElMessage.success('知识库已更新')
    } else {
      await createKnowledgeBase(buildPayload())
      ElMessage.success('知识库创建成功')
    }
    dialogVisible.value = false
    await loadPage()
  } finally {
    submitting.value = false
  }
}

const openDocuments = (row) => {
  router.push(`/agent/knowledge/${row.id}/documents`)
}

const toggleStatus = async (row) => {
  const nextStatus = normalizeStatus(row.status) === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `${action}知识库“${row.knowledgeName}”吗？${nextStatus === 0 ? '停用后将停止上传、任务和检索。' : ''}`,
      `${action}确认`,
      {
        type: 'warning',
        confirmButtonText: action,
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  await updateKnowledgeBase(row.id, {
    knowledgeName: row.knowledgeName,
    description: row.description || '',
    modelUrl: row.modelUrl,
    topK: Number(row.topK || 5),
    scoreThreshold: Number(row.scoreThreshold ?? 0.5),
    status: nextStatus
  })
  ElMessage.success(`知识库已${action}`)
  await loadPage()
}

const rememberDeletionTask = (knowledgeBaseId, task) => {
  if (!task?.id) return
  deletingTasks[String(knowledgeBaseId)] = task
  try {
    const storageKey = deletionStorageKey()
    const saved = JSON.parse(localStorage.getItem(storageKey) || '{}')
    saved[String(knowledgeBaseId)] = String(task.id)
    localStorage.setItem(storageKey, JSON.stringify(saved))
  } catch {
    // Storage may be unavailable in restricted browsers; in-memory polling still works.
  }
}

const forgetDeletionTask = (knowledgeBaseId) => {
  try {
    const storageKey = deletionStorageKey()
    const saved = JSON.parse(localStorage.getItem(storageKey) || '{}')
    delete saved[String(knowledgeBaseId)]
    localStorage.setItem(storageKey, JSON.stringify(saved))
  } catch {
    // Ignore storage failures.
  }
}

const stopTaskPoller = (timerKey) => {
  const poller = taskPollers.get(timerKey)
  if (poller?.timerId) {
    window.clearTimeout(poller.timerId)
  }
  taskPollers.delete(timerKey)
}

const clearTaskPollers = () => {
  taskPollers.forEach((poller) => {
    if (poller.timerId) {
      window.clearTimeout(poller.timerId)
    }
  })
  taskPollers.clear()
}

const scheduleTaskPoll = (taskId, knowledgeBaseId) => {
  const timerKey = `${taskId}:${knowledgeBaseId}`
  if (taskPollers.has(timerKey)) return
  const poller = { timerId: null, consecutiveFailures: 0 }
  taskPollers.set(timerKey, poller)

  const scheduleNext = (delay) => {
    if (!alive.value || !taskPollers.has(timerKey)) return
    poller.timerId = window.setTimeout(poll, delay)
  }

  const poll = async () => {
    if (!alive.value) {
      stopTaskPoller(timerKey)
      return
    }
    poller.timerId = null
    try {
      const task = await getKnowledgeTask(taskId, { skipErrorMessage: true })
      if (!alive.value || !taskPollers.has(timerKey)) {
        return
      }
      poller.consecutiveFailures = 0
      deletingTasks[String(knowledgeBaseId)] = task
      const status = String(task?.status || '').toUpperCase()
      if (status === 'SUCCEEDED') {
        stopTaskPoller(timerKey)
        forgetDeletionTask(knowledgeBaseId)
        delete deletingTasks[String(knowledgeBaseId)]
        ElMessage.success('知识库及其数据已删除')
        await loadRows()
        return
      }
      if (status === 'FAILED') {
        stopTaskPoller(timerKey)
        forgetDeletionTask(knowledgeBaseId)
        ElMessage.error(task.errorMessage || '知识库删除失败，可手动重新提交')
        await loadRows()
        return
      }
    } catch (error) {
      const statusCode = Number(error?.response?.status || 0)
      const taskMissing = String(error?.message || '').includes('知识任务不存在')
      if ([401, 403, 404].includes(statusCode) || taskMissing) {
        stopTaskPoller(timerKey)
        if (statusCode === 404 || taskMissing) {
          forgetDeletionTask(knowledgeBaseId)
          delete deletingTasks[String(knowledgeBaseId)]
        }
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

const restoreDeletionTasks = () => {
  try {
    localStorage.removeItem('knowledge_delete_tasks')
    const saved = JSON.parse(localStorage.getItem(deletionStorageKey()) || '{}')
    Object.entries(saved).forEach(([knowledgeBaseId, taskId]) => {
      deletingTasks[knowledgeBaseId] = { id: taskId, status: 'PENDING', progress: 0 }
      scheduleTaskPoll(taskId, knowledgeBaseId)
    })
  } catch {
    // Ignore malformed or unavailable storage.
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除知识库“${row.knowledgeName}”吗？系统将异步解绑智能体并清理所有文档、切片、源文件与向量。此操作不可撤销。`,
      '删除知识库',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  const task = await deleteKnowledgeBase(row.id)
  rememberDeletionTask(row.id, task)
  ElMessage.success('删除任务已提交')
  scheduleTaskPoll(task.id, row.id)
  await loadPage()
}

const retryDeleteTask = async (row) => {
  const oldTask = deletingTasks[String(row.id)]
  if (!oldTask?.id) return
  const task = await resubmitKnowledgeTask(oldTask.id)
  rememberDeletionTask(row.id, task)
  ElMessage.success('删除任务已重新提交')
  scheduleTaskPoll(task.id, row.id)
}

const taskFor = (row) => deletingTasks[String(row.id)]

onMounted(async () => {
  restoreDeletionTasks()
  await loadPage()
})

onBeforeUnmount(() => {
  alive.value = false
  clearTaskPollers()
})
</script>

<template>
  <section v-loading="loading" class="knowledge-page management-page">
    <div class="knowledge-metrics management-metrics">
      <article v-for="item in knowledgeMetricCards" :key="item.label" class="management-metric-card">
        <div class="management-metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <small>{{ item.label }}</small>
          <strong>{{ item.value }}</strong>
          <p>{{ item.note }}</p>
        </div>
      </article>
    </div>

    <div class="knowledge-content-grid management-content-grid">
    <main class="table-panel management-panel">
      <div class="table-toolbar management-panel-title">
        <div class="table-title">
          <h2>知识库列表</h2>
          <span class="management-panel-note">共 {{ total }} 个知识库</span>
        </div>
        <div class="management-filter-bar">
          <el-input
            v-model="query.keyword"
            clearable
            :prefix-icon="Search"
            placeholder="知识库名称或描述"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="query.status" clearable placeholder="全部状态" @change="handleSearch">
            <el-option label="已启用" :value="1" />
            <el-option label="已停用" :value="0" />
            <el-option label="删除中" :value="2" />
          </el-select>
          <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreate">创建知识库</el-button>
        </div>
      </div>

      <div v-if="rows.length" class="knowledge-list">
        <article
          v-for="row in rows"
          :key="row.id"
          class="knowledge-card management-data-card"
          :class="{ 'is-deleting': normalizeStatus(row.status) === 2 }"
        >
          <header>
            <span class="knowledge-mark"><el-icon><Collection /></el-icon></span>
            <div class="knowledge-heading">
              <div>
                <h4>{{ row.knowledgeName }}</h4>
                <el-tag :type="statusMeta(row.status).type" effect="light">
                  {{ statusMeta(row.status).text }}
                </el-tag>
              </div>
              <p>{{ row.description || '暂无描述' }}</p>
            </div>
            <el-dropdown
              v-if="normalizeStatus(row.status) !== 2"
              class="management-card-menu"
              trigger="click"
            >
              <button class="management-card-menu-button" type="button" aria-label="知识库操作">
                <el-icon class="management-card-menu-icon"><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="SwitchButton" @click="toggleStatus(row)">
                    {{ normalizeStatus(row.status) === 1 ? '停用' : '启用' }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    :icon="Delete"
                    :disabled="normalizeStatus(row.status) !== 1"
                    divided
                    class="danger-item"
                    @click="handleDelete(row)"
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </header>

          <div class="knowledge-facts">
            <span><small>Embedding 模型</small><b>{{ row.embeddingModelName || '-' }}</b></span>
            <span><small>向量规格</small><b>{{ row.embeddingDimension }} 维 · {{ row.metricType }}</b></span>
            <span><small>检索配置</small><b>TopK {{ row.topK }} · 阈值 {{ row.scoreThreshold }}</b></span>
            <span><small>知识文档</small><b>{{ formatNumber(row.documentCount) }} 篇</b></span>
          </div>

          <footer>
            <template v-if="normalizeStatus(row.status) !== 2">
              <div>
                <b>最近更新</b>
                <small>{{ formatTime(row) }}</small>
              </div>
              <div>
                <el-button @click="openDocuments(row)">文档</el-button>
                <el-button type="primary" plain @click="openEdit(row)">配置</el-button>
              </div>
            </template>
            <template v-else>
              <div class="delete-summary">
                <b>{{ taskStatusMeta(taskFor(row)?.status).text }}</b>
                <small :title="taskFor(row)?.errorMessage">
                  {{ taskFor(row)?.errorMessage || '正在清理知识库关联资源' }}
                </small>
              </div>
              <div class="delete-controls">
                <el-progress
                  class="delete-progress"
                  :percentage="Number(taskFor(row)?.progress || 0)"
                  :status="String(taskFor(row)?.status).toUpperCase() === 'FAILED' ? 'exception' : undefined"
                  :stroke-width="6"
                />
                <el-button
                  v-if="String(taskFor(row)?.status).toUpperCase() === 'FAILED'"
                  link
                  type="danger"
                  @click="retryDeleteTask(row)"
                >
                  重新提交
                </el-button>
              </div>
            </template>
          </footer>
        </article>
      </div>
      <el-empty v-else description="暂无符合条件的知识库">
        <el-button type="primary" @click="openCreate">创建知识库</el-button>
      </el-empty>

      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        class="management-pagination"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, prev, pager, next, sizes"
        @current-change="loadRows"
        @size-change="handleSearch"
      />
    </main>

    <aside class="knowledge-side-column management-side-column">
      <section class="knowledge-side-panel management-side-card">
        <header><div><h3>处理异常</h3><p>最近失败的知识任务</p></div></header>
        <article v-for="task in recentFailures" :key="task.id" class="failure-task">
          <div class="failure-task-title">
            <span><el-icon><Document /></el-icon></span>
            <div><b>{{ task.documentName || task.knowledgeBaseName }}</b><small>{{ task.knowledgeBaseName }}</small></div>
          </div>
          <p :title="task.errorMessage">{{ task.errorMessage || '任务执行失败' }}</p>
          <div class="failure-task-meta">
            <span>{{ task.stage || task.taskType }} · {{ task.finishedAt ? String(task.finishedAt).replace('T', ' ') : '-' }}</span>
            <el-button v-if="task.retryable" link type="danger" @click="retryRecentFailure(task)">重新提交</el-button>
          </div>
        </article>
        <el-empty v-if="!recentFailures.length" description="暂无处理异常" :image-size="64" />
      </section>
    </aside>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close>
      <el-alert
        v-if="!isEdit"
        class="create-alert"
        title="创建时将实时验证 Embedding 服务和向量维度，并初始化向量集合。"
        type="info"
        :closable="false"
        show-icon
      />
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
        class="knowledge-form"
      >
        <div class="form-grid">
          <el-form-item label="知识库名称" prop="knowledgeName">
            <el-input v-model="form.knowledgeName" maxlength="100" show-word-limit placeholder="例如：产品使用手册" />
          </el-form-item>
          <el-form-item label="状态" v-if="isEdit">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="描述" class="full-field">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="说明知识库的内容范围和使用场景"
            />
          </el-form-item>
        </div>

        <div class="form-section-title">
          <strong>Embedding 配置</strong>
          <span>仅支持 OpenAI 兼容接口；API Key 只写不回显。</span>
        </div>
        <div class="form-grid">
          <el-form-item label="API 地址" prop="modelUrl" class="full-field">
            <el-input v-model="form.modelUrl" placeholder="https://example.com/v1" />
          </el-form-item>
          <el-form-item label="API Key" prop="apiKey" class="full-field">
            <el-input
              v-model="form.apiKey"
              type="password"
              show-password
              autocomplete="new-password"
              :placeholder="isEdit ? '留空表示继续使用原 API Key' : '请输入真实 API Key'"
            />
          </el-form-item>
          <el-form-item label="模型名称" prop="embeddingModelName">
            <el-input
              v-model="form.embeddingModelName"
              :disabled="isEdit"
              placeholder="例如：text-embedding-3-small"
            />
          </el-form-item>
          <el-form-item label="向量维度" prop="embeddingDimension">
            <el-input-number
              v-model="form.embeddingDimension"
              :disabled="isEdit"
              :min="1"
              :max="65535"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="度量方式" prop="metricType">
            <el-select v-model="form.metricType" :disabled="isEdit">
              <el-option label="COSINE（推荐）" value="COSINE" />
              <el-option label="IP" value="IP" />
              <el-option label="L2" value="L2" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-section-title">
          <strong>检索配置</strong>
          <span>可随时调整，无需重新切片入库。</span>
        </div>
        <div class="form-grid">
          <el-form-item label="TopK" prop="topK">
            <el-input-number v-model="form.topK" :min="1" :max="20" controls-position="right" />
          </el-form-item>
          <el-form-item label="相似度阈值" prop="scoreThreshold">
            <el-input-number
              v-model="form.scoreThreshold"
              :min="0"
              :max="1"
              :step="0.05"
              :precision="2"
              controls-position="right"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEdit ? '保存修改' : '验证并创建' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.knowledge-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.knowledge-metrics article {
  padding: 18px;
  border: 1px solid #dce8f5;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(48, 94, 151, 0.06);
}

.knowledge-metrics small,
.knowledge-metrics strong,
.knowledge-metrics span { display: block; }
.knowledge-metrics small { color: #71849e; font-size: 12px; }
.knowledge-metrics strong { margin: 6px 0; color: #0a2547; font-size: 25px; }
.knowledge-metrics span { color: #8293a8; font-size: 12px; }

.knowledge-content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  align-items: start;
  gap: 18px;
}

.knowledge-main,
.knowledge-side-column { min-width: 0; }
.knowledge-main { display: grid; gap: 18px; }

.knowledge-side-panel {
  overflow: hidden;
  border: 1px solid #dce8f5;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(48, 94, 151, 0.06);
}

.knowledge-side-panel > header {
  padding: 18px;
  border-bottom: 1px solid #e8eff7;
}

.knowledge-side-panel h3,
.knowledge-side-panel p { margin: 0; }
.knowledge-side-panel h3 { color: #0a2547; font-size: 16px; }
.knowledge-side-panel header p { margin-top: 4px; color: #71849e; font-size: 12px; }

.failure-task { padding: 15px 0; border-bottom: 1px solid #edf2f7; }
.failure-task:last-of-type { border-bottom: 0; }
.failure-task-title { display: grid; grid-template-columns: 34px minmax(0, 1fr); align-items: center; gap: 10px; }
.failure-task-title > span { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; color: #d94b4b; background: #fff0f0; }
.failure-task-title b,
.failure-task-title small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.failure-task-title b { color: #17324d; font-size: 13px; }
.failure-task-title small { margin-top: 3px; color: #8293a8; font-size: 11px; }
.failure-task > p { overflow: hidden; margin: 10px 0 6px; color: #bd3e3e; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.failure-task-meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.failure-task-meta > span { overflow: hidden; color: #8b99aa; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; }

.knowledge-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.knowledge-card {
  overflow: hidden;
  min-width: 0;
  border: 1px solid #e5eaf3;
  border-radius: 16px;
  background: #fff;
}

.knowledge-card.is-deleting {
  border-color: #f0d6ae;
}

.knowledge-card > header,
.knowledge-card > footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.knowledge-card > header {
  align-items: flex-start;
  padding: 17px 52px 12px 17px;
}

.knowledge-mark {
  display: grid;
  height: 42px;
  flex: 0 0 42px;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(145deg, #4f82ff, #6954d9);
  font-size: 20px;
}

.knowledge-heading {
  min-width: 0;
  flex: 1;
}

.knowledge-heading > div {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.knowledge-heading h4 {
  overflow: hidden;
  color: #172033;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-heading p {
  overflow: hidden;
  margin-top: 6px;
  color: #8a95a8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 0 17px 16px;
}

.knowledge-facts span {
  min-width: 0;
  padding: 10px 11px;
  border-radius: 10px;
  background: #f7f8fb;
}

.knowledge-facts small,
.knowledge-facts b {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-facts small {
  margin-bottom: 4px;
  color: #929cad;
  font-size: 11px;
}

.knowledge-facts b {
  color: #384258;
  font-size: 12px;
}

.knowledge-card > footer {
  min-height: 65px;
  padding: 12px 17px;
  border-top: 1px solid #eef1f6;
  background: #fbfcfe;
}

.knowledge-card > footer > div:first-child {
  min-width: 0;
}

.knowledge-card > footer b,
.knowledge-card > footer small {
  display: block;
}

.knowledge-card > footer b {
  color: #44516a;
  font-size: 12px;
}

.knowledge-card > footer small {
  overflow: hidden;
  margin-top: 3px;
  color: #949dae;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-summary {
  flex: 1;
}

.delete-controls {
  display: flex;
  min-width: 112px;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.delete-progress {
  width: 112px;
}

:deep(.danger-item) {
  color: #d95050;
}

.create-alert {
  margin-bottom: 18px;
}

.knowledge-form {
  max-height: 66vh;
  overflow-y: auto;
  padding-right: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.full-field {
  grid-column: 1 / -1;
}

.form-section-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 6px 0 16px;
  padding-top: 16px;
  border-top: 1px solid #e2ebf6;
}

.form-section-title strong {
  color: #0a2547;
  font-size: 15px;
}

.form-section-title span {
  color: #7a8da7;
  font-size: 12px;
}

.form-grid :deep(.el-input-number),
.form-grid :deep(.el-select) {
  width: 100%;
}

@media (max-width: 760px) {
  .knowledge-metrics,
  .knowledge-content-grid,
  .knowledge-list {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .full-field {
    grid-column: auto;
  }
}

@media (min-width: 761px) and (max-width: 1180px) {
  .knowledge-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .knowledge-content-grid { grid-template-columns: 1fr; }
  .knowledge-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>

<style scoped src="../../assets/management-page.css"></style>
