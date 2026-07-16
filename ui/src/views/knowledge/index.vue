<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Check,
  CircleCheck,
  Coin,
  Collection,
  DataLine,
  Document,
  DocumentAdd,
  Files,
  MoreFilled,
  Plus,
  Reading,
  Refresh,
  Search,
  Stopwatch,
  Tickets,
  TrendCharts,
  UploadFilled,
  View,
  Warning
} from '@element-plus/icons-vue'
import {
  createKnowledgeBase,
  indexKnowledgeDocument,
  listKnowledgeBases,
  listKnowledgeChunks,
  listKnowledgeDocuments,
  uploadKnowledgeDocument
} from '@/axios/knowledge'

const uploadRef = ref()
const loading = ref(false)
const creating = ref(false)
const uploading = ref(false)
const indexing = ref(false)
const loadingChunks = ref(false)
const createDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const retrievalDialogVisible = ref(false)
const chunkDrawerVisible = ref(false)
const selectedFile = ref(null)
const uploadedDocument = ref(null)
const indexResult = ref(null)
const chunks = ref([])
const knowledgeBases = ref([])
const documents = ref([])
const currentPage = ref(1)
const pageSize = ref(6)

const queryParams = reactive({
  keyword: '',
  backend: '',
  status: ''
})

const form = reactive({
  knowledgeBaseId: '',
  backendConfigId: '',
  workspaceFileId: '',
  language: 'zh-CN',
  chunkSize: 1000,
  chunkOverlap: 100,
  overwrite: true
})

const createForm = reactive({
  knowledgeName: '',
  collectionName: '',
  description: '',
  knowledgeBackendId: '',
  chunkStrategy: 'GENERAL',
  chunkSize: 1000,
  chunkOverlap: 100,
  retrieveTopK: 5,
  scoreThreshold: 0.5,
  rerankEnabled: 1,
  visibility: 'TENANT',
  status: 1
})

const retrievalForm = reactive({
  knowledgeBaseId: '',
  query: '',
  topK: 5,
  threshold: 0.5
})

const demoBases = [
  {
    id: 1,
    knowledgeName: '企业产品手册',
    collectionName: 'product_manual',
    description: '包含公司产品介绍、功能说明与使用指南等文档。',
    backend: 'RAGFlow',
    documents: 682,
    chunks: 48600,
    statusText: '就绪',
    status: 'ready',
    lastIndexedAt: '2024-05-20 14:32',
    tone: 'blue',
    icon: Reading,
    health: 99.4
  },
  {
    id: 2,
    knowledgeName: '合同法规库',
    collectionName: 'contract_policy',
    description: '公司合同模板、法律法规与合规政策文档集合。',
    backend: 'Elasticsearch',
    documents: 1204,
    chunks: 92300,
    statusText: '就绪',
    status: 'ready',
    lastIndexedAt: '2024-05-20 12:18',
    tone: 'green',
    icon: Coin,
    health: 98.9
  },
  {
    id: 3,
    knowledgeName: '客服知识库',
    collectionName: 'service_faq',
    description: '客服常见问题、解决方案与服务流程文档。',
    backend: 'RAGFlow',
    documents: 456,
    chunks: 36800,
    statusText: '索引中 68%',
    status: 'indexing',
    lastIndexedAt: '2024-05-20 11:04',
    tone: 'orange',
    icon: Stopwatch,
    health: 96.8
  },
  {
    id: 4,
    knowledgeName: '研发规范库',
    collectionName: 'dev_standard',
    description: '研发流程、编码规范与技术文档集合。',
    backend: 'Milvus',
    documents: 144,
    chunks: 6300,
    statusText: '就绪',
    status: 'ready',
    lastIndexedAt: '2024-05-20 09:41',
    tone: 'violet',
    icon: DataLine,
    health: 99.7
  }
]

const demoDocuments = [
  {
    id: 101,
    knowledgeBaseId: 3,
    documentName: '客服知识库 - 常见问题集.pdf',
    documentType: 'PDF',
    sizeBytes: 4218000,
    chunkCount: 12560,
    parseStatus: 'CHUNKING',
    createTime: '2024-05-20 14:28'
  },
  {
    id: 102,
    knowledgeBaseId: 2,
    documentName: '合同模板 - 采购合同范本.docx',
    documentType: 'DOCX',
    sizeBytes: 836000,
    chunkCount: 8432,
    parseStatus: 'READY',
    createTime: '2024-05-20 13:45'
  },
  {
    id: 103,
    knowledgeBaseId: 1,
    documentName: '产品更新日志 - 2024Q2.xlsx',
    documentType: 'XLSX',
    sizeBytes: 562000,
    chunkCount: 2104,
    parseStatus: 'READY',
    createTime: '2024-05-20 12:31'
  },
  {
    id: 104,
    knowledgeBaseId: 4,
    documentName: '前端代码规范.md',
    documentType: 'MD',
    sizeBytes: 186000,
    chunkCount: 712,
    parseStatus: 'READY',
    createTime: '2024-05-20 09:41'
  }
]

const canUpload = computed(() => Boolean(selectedFile.value && String(form.knowledgeBaseId).trim()))

const canIndex = computed(() => Boolean(uploadedDocument.value?.id && String(form.backendConfigId).trim()))

const uploadTips = computed(() => {
  if (!selectedFile.value) {
    return '支持 txt、md、csv、json、html 等文本类文档'
  }
  return `${selectedFile.value.name} · ${formatBytes(selectedFile.value.size)}`
})

const normalizedDocuments = computed(() => {
  const rows = documents.value.length ? documents.value : demoDocuments
  return rows.map((row) => ({
    ...row,
    documentName: row.documentName || row.name || '未命名文档',
    documentType: row.documentType || 'UNKNOWN',
    sizeBytes: Number(row.sizeBytes || row.fileSize || 0),
    chunkCount: Number(row.chunkCount || 0),
    tokenCount: Number(row.tokenCount || 0),
    parseStatus: row.parseStatus || 'UPLOADED',
    createdAt: row.createTime || row.createdAt || row.updateTime || '-'
  }))
})

const knowledgeRows = computed(() => {
  const rows = knowledgeBases.value.length ? knowledgeBases.value : demoBases
  return rows.map((row, index) => normalizeKnowledgeBase(row, index))
})

const filteredKnowledgeRows = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  return knowledgeRows.value.filter((row) => {
    const matchKeyword = !keyword || [
      row.knowledgeName,
      row.collectionName,
      row.description,
      row.backend
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchBackend = !queryParams.backend || row.backend === queryParams.backend
    const matchStatus = !queryParams.status || row.status === queryParams.status
    return matchKeyword && matchBackend && matchStatus
  })
})

const pagedKnowledgeRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredKnowledgeRows.value.slice(start, start + pageSize.value)
})

const backendOptions = computed(() => {
  return [...new Set(knowledgeRows.value.map((row) => row.backend).filter(Boolean))]
})

const metrics = computed(() => {
  const totalBases = knowledgeRows.value.length
  const totalDocuments = knowledgeRows.value.reduce((sum, row) => sum + row.documents, 0)
  const totalChunks = knowledgeRows.value.reduce((sum, row) => sum + row.chunks, 0)
  const todaySearch = knowledgeBases.value.length ? Math.max(932, totalDocuments * 4) : 8932

  return [
    {
      label: '知识库',
      value: totalBases,
      delta: '较昨日 +2',
      icon: Collection,
      tone: 'blue'
    },
    {
      label: '文档总数',
      value: formatNumber(totalDocuments),
      delta: '较昨日 +156',
      icon: Document,
      tone: 'slate'
    },
    {
      label: '索引切片',
      value: formatCompact(totalChunks),
      delta: '较昨日 +8.7K',
      icon: Files,
      tone: 'indigo'
    },
    {
      label: '今日检索',
      value: formatNumber(todaySearch),
      delta: '较昨日 +1,285',
      icon: Search,
      tone: 'green'
    }
  ]
})

const recentDocuments = computed(() => normalizedDocuments.value.slice(0, 5))

const hotKeywords = [
  { text: '退款政策', count: 1285, tone: 'blue' },
  { text: '合同审批', count: 936, tone: 'green' },
  { text: 'API 限流', count: 712, tone: 'violet' },
  { text: '产品报价', count: 688, tone: 'orange' },
  { text: 'SLA 响应', count: 512, tone: 'slate' }
]

const normalizeKnowledgeBase = (row, index) => {
  const meta = parseJson(row.providerMetaJson)
  const relatedDocs = normalizedDocuments.value.filter((doc) => String(doc.knowledgeBaseId) === String(row.id))
  const documentsCount = Number(
    meta.documentCount ??
    meta.documents ??
    row.documentCount ??
    row.documents ??
    relatedDocs.length ??
    0
  )
  const chunksCount = Number(
    meta.chunkCount ??
    meta.chunks ??
    row.chunkCount ??
    relatedDocs.reduce((sum, doc) => sum + Number(doc.chunkCount || 0), 0) ??
    0
  )
  const backend = meta.backend || meta.storeType || row.backend || row.backendName || backendName(row)
  const rawStatus = row.status
  const stringStatus = typeof rawStatus === 'string' ? rawStatus : ''
  const isActive = stringStatus ? stringStatus !== 'disabled' : Number(rawStatus ?? 1) === 1
  const hasRunningDocument = relatedDocs.some((doc) => ['PENDING', 'PARSING', 'CHUNKING', 'EMBEDDING', 'UPLOADED']
    .includes(String(doc.parseStatus || '').toUpperCase()))
  const status = hasRunningDocument ? 'indexing' : stringStatus || (isActive ? 'ready' : 'disabled')

  return {
    id: row.id,
    knowledgeBackendId: row.knowledgeBackendId,
    knowledgeName: row.knowledgeName || row.name || `知识库 ${index + 1}`,
    collectionName: row.collectionName || row.collection || '-',
    description: row.description || '暂无描述',
    backend,
    documents: documentsCount,
    chunks: chunksCount,
    status,
    statusText: row.statusText || (status === 'indexing' ? '索引中' : status === 'ready' ? '就绪' : '停用'),
    lastIndexedAt: row.updateTime || row.updatedAt || row.createTime || row.lastIndexedAt || '-',
    icon: [Reading, Coin, Stopwatch, DataLine][index % 4],
    tone: row.tone || ['blue', 'green', 'orange', 'violet'][index % 4],
    health: Number(meta.health || row.health || (isActive ? 98.6 : 92.4))
  }
}

const backendName = (row) => {
  if (row.knowledgeBackendId) {
    return `Backend #${row.knowledgeBackendId}`
  }
  return 'RAGFlow'
}

const parseJson = (value) => {
  if (!value || typeof value !== 'string') {
    return {}
  }
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

const beforeUpload = () => false

const handleFileChange = (file) => {
  selectedFile.value = file.raw
  uploadedDocument.value = null
  indexResult.value = null
  chunks.value = []
}

const handleFileRemove = () => {
  selectedFile.value = null
  uploadedDocument.value = null
  indexResult.value = null
  chunks.value = []
}

const resetFlow = () => {
  selectedFile.value = null
  uploadedDocument.value = null
  indexResult.value = null
  chunks.value = []
  uploadRef.value?.clearFiles()
}

const resetCreateForm = () => {
  Object.assign(createForm, {
    knowledgeName: '',
    collectionName: '',
    description: '',
    knowledgeBackendId: '',
    chunkStrategy: 'GENERAL',
    chunkSize: 1000,
    chunkOverlap: 100,
    retrieveTopK: 5,
    scoreThreshold: 0.5,
    rerankEnabled: 1,
    visibility: 'TENANT',
    status: 1
  })
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const openUploadDialog = (row = null) => {
  resetFlow()
  form.knowledgeBaseId = row?.id || form.knowledgeBaseId || ''
  form.backendConfigId = row?.raw?.knowledgeBackendId || row?.knowledgeBackendId || ''
  uploadDialogVisible.value = true
}

const openRetrievalDialog = () => {
  retrievalForm.knowledgeBaseId = filteredKnowledgeRows.value[0]?.id || ''
  retrievalForm.query = ''
  retrievalForm.topK = 5
  retrievalForm.threshold = 0.5
  retrievalDialogVisible.value = true
}

const handleCreateKnowledgeBase = async () => {
  if (!createForm.knowledgeName.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  creating.value = true
  try {
    await createKnowledgeBase({
      ...createForm,
      knowledgeBackendId: createForm.knowledgeBackendId || null
    })
    ElMessage.success('知识库创建成功')
    createDialogVisible.value = false
    await loadDashboard()
  } finally {
    creating.value = false
  }
}

const handleUpload = async () => {
  if (!canUpload.value) {
    ElMessage.warning('请选择文档并指定知识库')
    return
  }

  uploading.value = true
  try {
    uploadedDocument.value = await uploadKnowledgeDocument({
      file: selectedFile.value,
      knowledgeBaseId: form.knowledgeBaseId,
      workspaceFileId: form.workspaceFileId,
      language: form.language
    })
    indexResult.value = null
    chunks.value = []
    ElMessage.success('文档上传成功')
    await loadDashboard()
  } finally {
    uploading.value = false
  }
}

const handleIndex = async () => {
  if (!canIndex.value) {
    ElMessage.warning('请先上传文档并填写向量配置 ID')
    return
  }

  indexing.value = true
  try {
    indexResult.value = await indexKnowledgeDocument({
      documentId: uploadedDocument.value.id,
      backendConfigId: form.backendConfigId,
      chunkSize: Number(form.chunkSize),
      chunkOverlap: Number(form.chunkOverlap),
      overwrite: form.overwrite
    })
    ElMessage.success('切片入库成功')
    await loadChunks()
    await loadDashboard()
  } finally {
    indexing.value = false
  }
}

const loadChunks = async (document = uploadedDocument.value) => {
  const documentId = document?.id
  if (!documentId) {
    ElMessage.warning('请先选择文档')
    return
  }

  loadingChunks.value = true
  try {
    const data = await listKnowledgeChunks(documentId)
    chunks.value = Array.isArray(data) ? data : []
  } finally {
    loadingChunks.value = false
  }
}

const openChunkDrawer = async (document) => {
  uploadedDocument.value = document
  chunkDrawerVisible.value = true
  await loadChunks(document)
}

const submitRetrievalTest = () => {
  if (!retrievalForm.knowledgeBaseId || !retrievalForm.query.trim()) {
    ElMessage.warning('请选择知识库并输入检索内容')
    return
  }
  ElMessage.success('检索测试参数已就绪')
}

const loadDashboard = async () => {
  loading.value = true
  try {
    const [baseResult, documentResult] = await Promise.allSettled([
      listKnowledgeBases(),
      listKnowledgeDocuments()
    ])
    knowledgeBases.value = baseResult.status === 'fulfilled' && Array.isArray(baseResult.value)
      ? baseResult.value
      : []
    documents.value = documentResult.status === 'fulfilled' && Array.isArray(documentResult.value)
      ? documentResult.value
      : []
  } finally {
    loading.value = false
  }
}

const statusTagClass = (status) => {
  if (status === 'ready') {
    return 'ready'
  }
  if (status === 'indexing') {
    return 'indexing'
  }
  return 'disabled'
}

const documentStatusType = (status) => {
  const value = String(status || '').toUpperCase()
  if (value === 'READY') {
    return 'success'
  }
  if (value === 'FAILED') {
    return 'danger'
  }
  if (['PENDING', 'PARSING', 'CHUNKING', 'EMBEDDING', 'UPLOADED'].includes(value)) {
    return 'warning'
  }
  return 'info'
}

const formatDocumentStatus = (status) => {
  const map = {
    PENDING: '待处理',
    UPLOADED: '已上传',
    PARSING: '解析中',
    CHUNKING: '切片中',
    EMBEDDING: '向量化',
    READY: '就绪',
    FAILED: '失败'
  }
  return map[String(status || '').toUpperCase()] || status || '-'
}

const formatBytes = (value) => {
  const size = Number(value || 0)
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const formatNumber = (value) => Number(value || 0).toLocaleString('en-US')

const formatCompact = (value) => {
  const number = Number(value || 0)
  if (number >= 1000000) {
    return `${(number / 1000000).toFixed(1)}M`
  }
  if (number >= 1000) {
    return `${(number / 1000).toFixed(number >= 10000 ? 0 : 1)}K`
  }
  return String(number)
}

const shortText = (value, length = 96) => {
  const text = String(value || '')
  return text.length > length ? `${text.slice(0, length)}...` : text
}

watch(
  () => [queryParams.keyword, queryParams.backend, queryParams.status, pageSize.value],
  () => {
    currentPage.value = 1
  }
)

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="knowledge-console">
    <div class="knowledge-hero">
      <div>
        <h2>知识库</h2>
        <p>管理企业文档、切片与向量索引，为智能体提供稳定可靠的知识检索能力。</p>
      </div>
      <div class="hero-actions">
        <el-button size="large" :icon="Search" @click="openRetrievalDialog">检索测试</el-button>
        <el-button size="large" type="primary" :icon="Plus" @click="openCreateDialog">新建知识库</el-button>
      </div>
    </div>

    <div class="knowledge-metrics">
      <article v-for="item in metrics" :key="item.label" class="knowledge-metric">
        <div class="metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.delta }} <i>↑</i></small>
        </div>
      </article>
    </div>

    <div class="knowledge-dashboard">
      <section class="kb-list-panel">
        <div class="panel-head">
          <div>
            <h3>知识库列表</h3>
            <p>共 {{ filteredKnowledgeRows.length }} 个知识库</p>
          </div>
          <div class="kb-filter-bar">
            <el-input
              v-model="queryParams.keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索知识库名称或描述..."
            />
            <el-select v-model="queryParams.backend" clearable placeholder="全部后端">
              <el-option
                v-for="backend in backendOptions"
                :key="backend"
                :label="backend"
                :value="backend"
              />
            </el-select>
            <el-select v-model="queryParams.status" clearable placeholder="全部状态">
              <el-option label="就绪" value="ready" />
              <el-option label="索引中" value="indexing" />
              <el-option label="停用" value="disabled" />
            </el-select>
            <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
          </div>
        </div>

        <div class="kb-list">
          <article v-for="row in pagedKnowledgeRows" :key="row.id" class="kb-row">
            <header class="kb-card-head">
              <div class="kb-icon" :class="row.tone">
                <el-icon><component :is="row.icon" /></el-icon>
              </div>
              <div class="kb-main">
                <div class="kb-title-line">
                  <h4>{{ row.knowledgeName }}</h4>
                  <span>{{ row.backend }}</span>
                </div>
                <p>{{ row.description }}</p>
              </div>
              <span class="kb-status" :class="statusTagClass(row.status)">
                <el-icon><component :is="row.status === 'indexing' ? Warning : CircleCheck" /></el-icon>
                {{ row.statusText }}
              </span>
            </header>
            <div class="kb-card-stats">
              <div class="kb-stat">
                <strong>{{ formatNumber(row.documents) }}</strong>
                <span>文档</span>
              </div>
              <div class="kb-stat">
                <strong>{{ formatCompact(row.chunks) }}</strong>
                <span>切片</span>
              </div>
              <div class="kb-time">
                <span>最后索引</span>
                <strong>{{ row.lastIndexedAt }}</strong>
              </div>
            </div>
            <footer class="kb-card-actions">
              <el-button @click="openUploadDialog(row)">管理文档</el-button>
              <el-dropdown trigger="click">
                <button class="more-button" type="button" aria-label="更多操作">
                  <el-icon><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openUploadDialog(row)">上传文档</el-dropdown-item>
                    <el-dropdown-item @click="openRetrievalDialog">检索测试</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </footer>
          </article>
        </div>

        <div class="kb-list-footer">
          <span>共 {{ filteredKnowledgeRows.length }} 个</span>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            layout="prev, pager, next, sizes"
            :total="filteredKnowledgeRows.length"
            :page-sizes="[6, 12, 24]"
          />
        </div>
      </section>

      <aside class="knowledge-side">
        <section class="side-panel">
          <div class="side-head">
            <h3>热门检索词</h3>
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="keyword-list">
            <span v-for="keyword in hotKeywords" :key="keyword.text" :class="keyword.tone">
              {{ keyword.text }}
              <strong>{{ formatNumber(keyword.count) }}</strong>
            </span>
          </div>
        </section>

        <section class="side-panel">
          <div class="side-head">
            <h3>最近文档</h3>
            <el-button link type="primary">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
          </div>
          <div class="document-list compact">
            <div v-for="doc in recentDocuments" :key="doc.id" class="document-row">
              <span class="document-type">{{ doc.documentType }}</span>
              <div class="document-main">
                <strong>{{ doc.documentName }}</strong>
                <small>{{ formatBytes(doc.sizeBytes) }}</small>
              </div>
              <el-tag :type="documentStatusType(doc.parseStatus)" effect="light">
                {{ formatDocumentStatus(doc.parseStatus) }}
              </el-tag>
              <el-button text type="primary" :icon="View" @click="openChunkDrawer(doc)">切片</el-button>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      title="新建知识库"
      width="720px"
      destroy-on-close
      class="knowledge-dialog"
    >
      <el-form label-width="112px" class="knowledge-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="知识库名称" required>
              <el-input v-model="createForm.knowledgeName" placeholder="如：企业产品手册" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="集合名称">
              <el-input v-model="createForm.collectionName" placeholder="如：product_manual" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="后端配置 ID">
              <el-input v-model="createForm.knowledgeBackendId" placeholder="可为空" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可见范围">
              <el-select v-model="createForm.visibility">
                <el-option label="租户" value="TENANT" />
                <el-option label="智能体" value="AGENT" />
                <el-option label="私有" value="PRIVATE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="切片策略">
              <el-select v-model="createForm.chunkStrategy">
                <el-option label="通用" value="GENERAL" />
                <el-option label="段落" value="PARAGRAPH" />
                <el-option label="Token" value="TOKEN" />
                <el-option label="问答" value="QA" />
                <el-option label="表格" value="TABLE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="返回数量">
              <el-input-number v-model="createForm.retrieveTopK" :min="1" :max="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="切片大小">
              <el-input-number v-model="createForm.chunkSize" :min="100" :max="8000" :step="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="切片重叠">
              <el-input-number v-model="createForm.chunkOverlap" :min="0" :max="2000" :step="50" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="说明">
              <el-input
                v-model="createForm.description"
                type="textarea"
                :rows="3"
                placeholder="描述用途和内容范围"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateKnowledgeBase">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="uploadDialogVisible"
      title="文档入库"
      width="820px"
      destroy-on-close
      class="knowledge-dialog"
      @closed="resetFlow"
    >
      <div class="step-panel">
        <el-steps :active="uploadedDocument ? indexResult ? 3 : 2 : selectedFile ? 1 : 0" finish-status="success" simple>
          <el-step title="选择文档" :icon="DocumentAdd" />
          <el-step title="上传文档" :icon="UploadFilled" />
          <el-step title="切片入库" :icon="Check" />
          <el-step title="查看切片" :icon="View" />
        </el-steps>
      </div>

      <div class="upload-dialog-grid">
        <section>
          <div class="dialog-section-title">
            <h4>上传配置</h4>
            <span>{{ uploadTips }}</span>
          </div>
          <el-form label-width="110px" class="knowledge-form compact">
            <el-form-item label="知识库" required>
              <el-select v-model="form.knowledgeBaseId" filterable placeholder="选择知识库">
                <el-option
                  v-for="row in knowledgeRows"
                  :key="row.id"
                  :label="row.knowledgeName"
                  :value="row.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="工作区文件 ID">
              <el-input v-model="form.workspaceFileId" placeholder="可为空" clearable />
            </el-form-item>
            <el-form-item label="文档语言">
              <el-select v-model="form.language">
                <el-option label="中文" value="zh-CN" />
                <el-option label="英文" value="en-US" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-upload
            ref="uploadRef"
            class="knowledge-upload"
            drag
            :limit="1"
            :auto-upload="false"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="upload-icon">
              <UploadFilled />
            </el-icon>
            <div class="upload-text">拖拽文件到此处，或点击选择</div>
          </el-upload>
          <el-button
            type="primary"
            :icon="UploadFilled"
            :loading="uploading"
            :disabled="!canUpload"
            @click="handleUpload"
          >
            上传文档
          </el-button>
        </section>

        <section>
          <div class="dialog-section-title">
            <h4>索引配置</h4>
            <span>上传成功后写入向量后端</span>
          </div>
          <el-form label-width="110px" class="knowledge-form compact">
            <el-form-item label="向量配置 ID" required>
              <el-input v-model="form.backendConfigId" placeholder="ai_knowledge_backend_config.id" clearable />
            </el-form-item>
            <el-form-item label="切片大小">
              <el-input-number v-model="form.chunkSize" :min="100" :max="8000" :step="100" />
            </el-form-item>
            <el-form-item label="切片重叠">
              <el-input-number v-model="form.chunkOverlap" :min="0" :max="2000" :step="50" />
            </el-form-item>
            <el-form-item label="覆盖旧切片">
              <el-switch v-model="form.overwrite" />
            </el-form-item>
          </el-form>
          <div class="index-result-strip">
            <div>
              <span>文档 ID</span>
              <strong>{{ uploadedDocument?.id || '-' }}</strong>
            </div>
            <div>
              <span>切片数</span>
              <strong>{{ indexResult?.chunkCount ?? uploadedDocument?.chunkCount ?? 0 }}</strong>
            </div>
            <div>
              <span>Token</span>
              <strong>{{ indexResult?.tokenCount ?? uploadedDocument?.tokenCount ?? 0 }}</strong>
            </div>
          </div>
          <div class="dialog-actions">
            <el-button
              type="primary"
              :icon="Check"
              :loading="indexing"
              :disabled="!canIndex"
              @click="handleIndex"
            >
              切片并入库
            </el-button>
            <el-button :icon="View" :disabled="!uploadedDocument?.id" :loading="loadingChunks" @click="openChunkDrawer(uploadedDocument)">
              查看切片
            </el-button>
          </div>
        </section>
      </div>
    </el-dialog>

    <el-dialog
      v-model="retrievalDialogVisible"
      title="检索测试"
      width="640px"
      destroy-on-close
      class="knowledge-dialog"
    >
      <el-form label-width="96px" class="knowledge-form">
        <el-form-item label="知识库">
          <el-select v-model="retrievalForm.knowledgeBaseId" filterable placeholder="选择知识库">
            <el-option
              v-for="row in knowledgeRows"
              :key="row.id"
              :label="row.knowledgeName"
              :value="row.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="检索内容">
          <el-input v-model="retrievalForm.query" type="textarea" :rows="4" placeholder="输入需要测试的查询" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Top K">
              <el-input-number v-model="retrievalForm.topK" :min="1" :max="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阈值">
              <el-input-number v-model="retrievalForm.threshold" :min="0" :max="1" :step="0.05" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="retrievalDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Search" @click="submitRetrievalTest">开始测试</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="chunkDrawerVisible" title="切片内容" size="58%">
      <el-table
        v-loading="loadingChunks"
        :data="chunks"
        stripe
        class="chunk-table"
        empty-text="暂无切片"
      >
        <el-table-column prop="chunkIndex" label="#" width="70" />
        <el-table-column prop="content" label="内容" min-width="360" show-overflow-tooltip>
          <template #default="{ row }">{{ shortText(row.content, 140) }}</template>
        </el-table-column>
        <el-table-column prop="tokenCount" label="Token" width="90" />
        <el-table-column prop="vectorId" label="向量 ID" min-width="190" show-overflow-tooltip />
        <el-table-column prop="sectionTitle" label="标题" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-drawer>
  </section>
</template>

<style scoped>
.knowledge-console {
  display: grid;
  min-height: calc(100vh - 115px);
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 18px;
  padding-bottom: 28px;
}

.knowledge-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.knowledge-hero h2 {
  margin: 14px 0 8px;
  color: #071f40;
  font-size: 34px;
  font-weight: 850;
  letter-spacing: 0;
}

.knowledge-hero p {
  margin: 0;
  color: #526b87;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

.knowledge-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.knowledge-metric,
.kb-list-panel,
.side-panel {
  border: 1px solid #d7e5f8;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 38px rgba(48, 94, 151, 0.08);
}

.knowledge-metric {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 10px 22px;
}

.knowledge-metric span {
  display: block;
  color: #667d99;
  font-size: 13px;
}

.knowledge-metric strong {
  display: block;
  margin-top: 8px;
  color: #0a2547;
  font-size: 30px;
  font-weight: 850;
  line-height: 1;
}

.knowledge-metric small {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  color: #22a86b;
  font-size: 12px;
  font-weight: 750;
}

.knowledge-metric i {
  font-style: normal;
}

.knowledge-dashboard {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(760px, 1fr) minmax(330px, 0.36fr);
  align-self: stretch;
  align-items: stretch;
  gap: 18px;
  min-height: 0;
}

.kb-list-panel {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-color: #d9e4f2;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(34, 67, 112, 0.06);
}

.panel-head,
.side-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-head {
  align-items: center;
  padding: 18px 18px 24px;
  border-bottom: 0;
}

.panel-head h3,
.side-head h3 {
  margin: 0;
  color: #0a2547;
  font-size: 17px;
  font-weight: 800;
}

.panel-head p {
  margin: 6px 0 0;
  color: #6d819b;
  font-size: 12px;
}

.kb-filter-bar {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
}

.kb-filter-bar .el-input {
  width: 250px;
}

.kb-filter-bar .el-select {
  width: 128px;
}

.kb-filter-bar .el-button {
  height: 34px;
  border-radius: 5px;
  font-weight: 800;
}

.kb-filter-bar :deep(.el-input__wrapper),
.kb-filter-bar :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 5px;
  box-shadow: 0 0 0 1px #d7e1ee inset;
}

.kb-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px 20px;
  padding: 0 18px 18px;
}

.kb-row {
  position: relative;
  display: grid;
  min-height: 160px;
  grid-template-rows: auto minmax(78px, 1fr) auto;
  padding: 18px;
  border: 1px solid #e0eaf6;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(42, 72, 108, 0.05);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.kb-row:hover {
  border-color: #a9c9ff;
  box-shadow: 0 12px 28px rgba(47, 117, 255, 0.08);
  transform: translateY(-1px);
}

.kb-card-head {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 14px;
}

.kb-icon,
.metric-icon {
  display: grid;
  width: 54px;
  height: 54px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 13px;
  color: #2f75ff;
  background: #ecf4ff;
  font-size: 28px;
}

.metric-icon {
  width: 58px;
  height: 58px;
}

.kb-icon.green,
.metric-icon.green {
  color: #13a66f;
  background: #e9f9f0;
}

.kb-icon.orange {
  color: #e18a12;
  background: #fff4e2;
}

.kb-icon.violet {
  color: #7c5cff;
  background: #f1edff;
}

.metric-icon.slate {
  color: #53677f;
  background: #edf3f8;
}

.metric-icon.indigo {
  color: #4d7cff;
  background: #eef3ff;
}

.kb-main {
  min-width: 0;
  padding-right: 88px;
}

.kb-title-line {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
}

.kb-title-line h4 {
  min-width: 0;
  overflow: hidden;
  margin: 0;
  color: #0a2547;
  font-size: 15px;
  font-weight: 820;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.kb-row:hover .kb-title-line h4 {
  color: #0b63f6;
}

.kb-title-line span {
  flex: 0 0 auto;
  padding: 3px 7px;
  border-radius: 6px;
  color: #2f75ff;
  background: #eaf2ff;
  font-size: 11px;
  font-weight: 800;
}

.kb-main p {
  display: -webkit-box;
  overflow: hidden;
  margin: 10px 0 0;
  color: #6d819b;
  font-size: 12px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.kb-card-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  align-self: end;
  border: 1px solid #e1ebf6;
  border-radius: 8px;
  background: #f8fbff;
}

.kb-card-stats > div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border-right: 1px solid #e1ebf6;
}

.kb-card-stats > div:last-child {
  border-right: 0;
}

.kb-stat strong,
.kb-time strong {
  color: #203957;
  font-size: 15px;
}

.kb-stat span,
.kb-time span {
  color: #7e94ad;
  font-size: 12px;
}

.kb-time {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
}

.kb-time strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-status {
  position: absolute;
  top: 18px;
  right: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 82px;
  height: 24px;
  border: 1px solid transparent;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 800;
}

.kb-status.ready {
  border-color: #bce8cc;
  color: #168354;
  background: #eaf8ef;
}

.kb-status.indexing {
  border-color: #ffdca6;
  color: #c56a1c;
  background: #fff5e8;
}

.kb-status.disabled {
  border-color: #d9e2ec;
  color: #6d819b;
  background: #f4f7fb;
}

.more-button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 0;
  border-radius: 8px;
  color: #405874;
  background: transparent;
  cursor: pointer;
}

.more-button:hover {
  background: #edf4ff;
  color: #2f75ff;
}

.kb-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid #e5edf6;
}

.kb-card-actions .el-button {
  height: 32px;
  border-radius: 6px;
  font-weight: 800;
}

.kb-list-footer {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 18px 16px;
  color: #53637b;
  font-size: 14px;
  font-weight: 700;
}

.kb-list-footer :deep(.el-pagination) {
  --el-pagination-button-bg-color: #ffffff;
  --el-pagination-hover-color: #0b63f6;
}

.kb-list-footer :deep(.el-pager li),
.kb-list-footer :deep(.btn-prev),
.kb-list-footer :deep(.btn-next) {
  border: 1px solid #d9e4f2;
  border-radius: 5px;
  box-shadow: none;
}

.kb-list-footer :deep(.el-pager li.is-active) {
  border-color: #0b63f6;
  color: #0b63f6;
  background: #ffffff;
}

.knowledge-side {
  display: grid;
  grid-template-rows: minmax(150px, 0.42fr) minmax(260px, 0.58fr);
  align-content: stretch;
  gap: 18px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding: 18px;
}

.side-head {
  align-items: center;
}

.keyword-list {
  display: flex;
  flex-wrap: wrap;
  flex: 1 1 auto;
  gap: 10px;
  align-content: flex-start;
  min-height: 0;
  margin-top: 18px;
  overflow-y: auto;
  padding-right: 4px;
}

.keyword-list span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 9px;
  color: #2f75ff;
  background: #edf4ff;
  font-weight: 800;
}

.keyword-list .green {
  color: #168354;
  background: #eaf8ef;
}

.keyword-list .violet {
  color: #7056e8;
  background: #f1edff;
}

.keyword-list .orange {
  color: #c56a1c;
  background: #fff5e8;
}

.keyword-list .slate {
  color: #53677f;
  background: #edf3f8;
}

.keyword-list strong {
  font-size: 12px;
}

.document-list {
  display: grid;
  flex: 1 1 auto;
  align-content: start;
  min-height: 0;
  margin-top: 12px;
  overflow-y: auto;
  padding-right: 4px;
}

.document-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 78px 64px;
  align-items: center;
  gap: 12px;
  min-height: 58px;
  border-bottom: 1px solid #e3edf8;
}

.document-row:last-child {
  border-bottom: 0;
}

.document-list.compact .document-row {
  grid-template-columns: 42px minmax(0, 1fr) auto auto;
  align-items: center;
  padding: 12px 0;
}

.document-main {
  min-width: 0;
}

.document-list.compact .document-row > .el-tag,
.document-list.compact .document-row > .el-button {
  flex: 0 0 auto;
}

.document-list.compact .document-row > .el-button {
  padding-right: 0;
  padding-left: 0;
}

.document-type {
  display: grid;
  width: 42px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  color: #2f75ff;
  background: #edf4ff;
  font-size: 11px;
  font-weight: 850;
}

.document-row strong,
.document-row small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-row strong {
  color: #0a2547;
  font-size: 13px;
}

.document-row small {
  margin-top: 5px;
  color: #7890aa;
  font-size: 12px;
}

.knowledge-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 22px;
  border-bottom: 1px solid #dce8f5;
}

.knowledge-dialog :deep(.el-dialog__body) {
  padding: 20px 22px;
}

.knowledge-form .el-select,
.knowledge-form .el-input,
.knowledge-form :deep(.el-input-number),
.knowledge-form :deep(.el-textarea) {
  width: 100%;
}

.knowledge-form.compact {
  padding-right: 8px;
}

.step-panel {
  margin-bottom: 18px;
  padding: 12px;
  border: 1px solid #dbe7f6;
  border-radius: 10px;
  background: #f8fbff;
}

.upload-dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.dialog-section-title h4 {
  margin: 0;
  color: #0a2547;
  font-size: 16px;
}

.dialog-section-title span {
  display: block;
  margin-top: 6px;
  color: #6d819b;
  font-size: 12px;
}

.knowledge-upload {
  margin-bottom: 14px;
}

.knowledge-upload :deep(.el-upload-dragger) {
  border-radius: 10px;
}

.upload-icon {
  margin-top: 8px;
  color: #2f75ff;
  font-size: 42px;
}

.upload-text {
  color: #0a2547;
  font-weight: 750;
}

.index-result-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  margin: 8px 0 16px;
  border: 1px solid #dbe7f6;
  border-radius: 10px;
}

.index-result-strip div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-right: 1px solid #dbe7f6;
  background: #f8fbff;
}

.index-result-strip div:last-child {
  border-right: 0;
}

.index-result-strip span {
  color: #6d819b;
  font-size: 12px;
}

.index-result-strip strong {
  color: #0a2547;
  font-size: 18px;
}

.dialog-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chunk-table {
  width: 100%;
}

@media (max-width: 1320px) {
  .knowledge-dashboard {
    grid-template-columns: 1fr;
  }

  .knowledge-dashboard,
  .kb-list-panel,
  .knowledge-side {
    min-height: 0;
  }

  .knowledge-side {
    grid-template-rows: none;
  }

  .panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .kb-filter-bar {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}

@media (max-width: 980px) {
  .knowledge-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .knowledge-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .upload-dialog-grid {
    grid-template-columns: 1fr;
  }

  .kb-filter-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .kb-filter-bar .el-input,
  .kb-filter-bar .el-select,
  .kb-filter-bar .el-button {
    width: 100%;
  }

  .document-row {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .document-row > .el-tag,
  .document-row > .el-button {
    grid-column: 2;
    justify-self: start;
  }
}

@media (max-width: 640px) {
  .knowledge-hero h2 {
    font-size: 28px;
  }

  .knowledge-metrics {
    grid-template-columns: 1fr;
  }

  .kb-list {
    grid-template-columns: 1fr;
    padding: 0 12px 14px;
  }

  .kb-list-footer {
    align-items: flex-start;
    flex-direction: column;
    padding-right: 12px;
    padding-left: 12px;
  }

  .kb-card-head {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .kb-icon {
    width: 48px;
    height: 48px;
  }

  .kb-main {
    padding-right: 0;
  }

  .kb-status {
    position: static;
    grid-column: 1 / -1;
    justify-self: start;
  }

  .kb-card-stats {
    grid-template-columns: 1fr;
  }

  .kb-card-stats > div {
    border-right: 0;
    border-bottom: 1px solid #e1ebf6;
  }

  .kb-card-stats > div:last-child {
    border-bottom: 0;
  }
}
</style>
