<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
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
  listKnowledgeBases,
  updateKnowledgeBase
} from '@/axios/knowledge'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const dialogMode = ref('create')
const rows = ref([])
const total = ref(0)
const metrics = ref({})
const current = ref(1)
const size = ref(8)
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
  return value === '0' || value === 'DISABLED' ? 0 : 1
}

const statusMeta = (status) => {
  const value = normalizeStatus(status)
  if (value === 0) {
    return { text: '已停用', type: 'info' }
  }
  return { text: '已启用', type: 'success' }
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
  metrics.value = await getKnowledgeMetrics() || {}
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
    payload.metricType = 'COSINE'
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
      `${action}知识库“${row.knowledgeName}”吗？${nextStatus === 0 ? '停用后将停止上传、后台处理和检索。' : ''}`,
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

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除知识库“${row.knowledgeName}”吗？系统将同步解绑智能体并清理所有文档、切片、源文件与向量。此操作不可撤销。`,
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

  loading.value = true
  try {
    await deleteKnowledgeBase(row.id)
    ElMessage.success('知识库及其数据已删除')
    await loadPage()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadPage()
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
            <el-dropdown class="management-card-menu" trigger="click">
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
            <div>
              <b>最近更新</b>
              <small>{{ formatTime(row) }}</small>
            </div>
            <div>
              <el-button @click="openDocuments(row)">文档</el-button>
              <el-button type="primary" plain @click="openEdit(row)">配置</el-button>
            </div>
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
        :page-sizes="[8, 16, 32]"
        layout="total, prev, pager, next, sizes"
        @current-change="loadRows"
        @size-change="handleSearch"
      />
    </main>

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
          <span>仅支持 OpenAI 兼容接口</span>
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
          <el-form-item label="度量方式">
            <el-input model-value="COSINE" disabled />
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
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
  gap: 18px;
}

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
