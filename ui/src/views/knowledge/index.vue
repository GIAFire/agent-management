<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, DocumentAdd, Refresh, Search, UploadFilled, View } from '@element-plus/icons-vue'
import { indexKnowledgeDocument, listKnowledgeChunks, uploadKnowledgeDocument } from '@/axios/knowledge'

const uploadRef = ref()
const selectedFile = ref(null)
const uploading = ref(false)
const indexing = ref(false)
const loadingChunks = ref(false)
const uploadedDocument = ref(null)
const indexResult = ref(null)
const chunks = ref([])

const form = reactive({
  knowledgeBaseId: '',
  backendConfigId: '',
  workspaceFileId: '',
  language: 'zh-CN',
  chunkSize: 1000,
  chunkOverlap: 100,
  overwrite: true
})

const activeStep = computed(() => {
  if (indexResult.value) {
    return 3
  }
  if (uploadedDocument.value) {
    return 2
  }
  if (selectedFile.value) {
    return 1
  }
  return 0
})

const documentRows = computed(() => uploadedDocument.value ? [uploadedDocument.value] : [])

const canUpload = computed(() => {
  return Boolean(selectedFile.value && String(form.knowledgeBaseId).trim())
})

const canIndex = computed(() => {
  return Boolean(uploadedDocument.value?.id && String(form.backendConfigId).trim())
})

const uploadTips = computed(() => {
  if (!selectedFile.value) {
    return '选择 txt、md、csv、json、html 等文本文件'
  }
  return `${selectedFile.value.name} · ${formatBytes(selectedFile.value.size)}`
})

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

const handleUpload = async () => {
  if (!canUpload.value) {
    ElMessage.warning('请选择文件并填写知识库ID')
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
  } finally {
    uploading.value = false
  }
}

const handleIndex = async () => {
  if (!canIndex.value) {
    ElMessage.warning('请先上传文档并填写向量配置ID')
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
  } finally {
    indexing.value = false
  }
}

const loadChunks = async () => {
  const documentId = uploadedDocument.value?.id
  if (!documentId) {
    ElMessage.warning('请先上传文档')
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

const statusTagType = (status) => {
  const value = String(status || '').toUpperCase()
  if (value === 'READY' || value === 'UPLOADED') {
    return 'success'
  }
  if (value === 'FAILED') {
    return 'danger'
  }
  if (value === 'CHUNKING' || value === 'EMBEDDING' || value === 'PARSING') {
    return 'warning'
  }
  return 'info'
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

const formatId = (value) => value ?? '-'

const shortText = (value, length = 80) => {
  const text = String(value || '')
  return text.length > length ? `${text.slice(0, length)}...` : text
}
</script>

<template>
  <section class="knowledge-page">
    <div class="flow-header">
      <div class="flow-title">
        <h2>知识库切片入库</h2>
        <span>上传本地文档，按配置切片并写入向量库</span>
      </div>
      <el-button :icon="Refresh" @click="resetFlow">重置流程</el-button>
    </div>

    <div class="step-panel">
      <el-steps :active="activeStep" finish-status="success" simple>
        <el-step title="选择文件" :icon="DocumentAdd" />
        <el-step title="上传文档" :icon="UploadFilled" />
        <el-step title="切片入库" :icon="Check" />
        <el-step title="查看切片" :icon="View" />
      </el-steps>
    </div>

    <div class="workflow-grid">
      <section class="panel upload-panel">
        <div class="panel-title">
          <h3>文档上传</h3>
          <span>{{ uploadTips }}</span>
        </div>

        <el-form label-width="112px" class="knowledge-form">
          <el-form-item label="知识库ID" required>
            <el-input v-model="form.knowledgeBaseId" placeholder="ai_knowledge_base.id" clearable />
          </el-form-item>
          <el-form-item label="工作区文件ID">
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
          <div class="upload-text">拖拽文件到这里，或点击选择</div>
          <template #tip>
            <div class="upload-tip">当前后端支持文本类文档解析，PDF/DOCX 后续可接解析器。</div>
          </template>
        </el-upload>

        <div class="action-row">
          <el-button
            type="primary"
            :icon="UploadFilled"
            :loading="uploading"
            :disabled="!canUpload"
            @click="handleUpload"
          >
            上传文档
          </el-button>
        </div>
      </section>

      <section class="panel index-panel">
        <div class="panel-title">
          <h3>切片入库</h3>
          <span>调用 RagBackendFactory 写入向量库</span>
        </div>

        <el-form label-width="112px" class="knowledge-form">
          <el-form-item label="向量配置ID" required>
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

        <div class="result-strip">
          <div>
            <span>文档ID</span>
            <strong>{{ formatId(uploadedDocument?.id) }}</strong>
          </div>
          <div>
            <span>切片数</span>
            <strong>{{ indexResult?.chunkCount ?? uploadedDocument?.chunkCount ?? 0 }}</strong>
          </div>
          <div>
            <span>Token估算</span>
            <strong>{{ indexResult?.tokenCount ?? uploadedDocument?.tokenCount ?? 0 }}</strong>
          </div>
        </div>

        <div class="action-row">
          <el-button
            type="primary"
            :icon="Check"
            :loading="indexing"
            :disabled="!canIndex"
            @click="handleIndex"
          >
            切片并入库
          </el-button>
          <el-button :icon="Search" :disabled="!uploadedDocument?.id" :loading="loadingChunks" @click="loadChunks">
            查看切片
          </el-button>
        </div>
      </section>
    </div>

    <section class="panel">
      <div class="panel-title">
        <h3>上传文档记录</h3>
        <span>最近一次上传返回的数据</span>
      </div>
      <el-table :data="documentRows" stripe class="data-table" empty-text="暂无上传记录">
        <el-table-column prop="id" label="文档ID" width="110" />
        <el-table-column prop="documentName" label="文档名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="documentType" label="类型" width="90" />
        <el-table-column prop="sizeBytes" label="大小" width="110">
          <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
        </el-table-column>
        <el-table-column prop="parseStatus" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.parseStatus)">{{ row.parseStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceUri" label="存储路径" min-width="260" show-overflow-tooltip />
        <el-table-column prop="checksum" label="SHA-256" min-width="190">
          <template #default="{ row }">{{ shortText(row.checksum, 24) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>切片内容</h3>
        <span>共 {{ chunks.length }} 条</span>
      </div>
      <el-table
        v-loading="loadingChunks"
        :data="chunks"
        stripe
        class="data-table"
        empty-text="入库后可查看切片"
      >
        <el-table-column prop="chunkIndex" label="#" width="70" />
        <el-table-column prop="content" label="内容" min-width="360" show-overflow-tooltip>
          <template #default="{ row }">{{ shortText(row.content, 120) }}</template>
        </el-table-column>
        <el-table-column prop="tokenCount" label="Token" width="90" />
        <el-table-column prop="vectorId" label="向量ID" min-width="190" show-overflow-tooltip />
        <el-table-column prop="sectionTitle" label="标题" min-width="180" show-overflow-tooltip />
      </el-table>
    </section>
  </section>
</template>

<style scoped>
.knowledge-page {
  display: grid;
  gap: 14px;
}

.flow-header,
.step-panel,
.panel {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow);
}

.flow-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
}

.flow-title {
  min-width: 0;
}

.flow-title h2,
.panel-title h3 {
  margin: 0;
  color: var(--ink);
  font-weight: 760;
  letter-spacing: 0;
}

.flow-title h2 {
  font-size: 20px;
}

.flow-title span,
.panel-title span {
  display: block;
  margin-top: 4px;
  color: var(--subtle);
  font-size: 12px;
}

.step-panel {
  padding: 12px;
}

.workflow-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
}

.panel {
  min-width: 0;
  overflow: hidden;
}

.panel-title {
  min-height: 62px;
  padding: 14px 18px 10px;
  border-bottom: 1px solid var(--border);
}

.panel-title h3 {
  font-size: 16px;
}

.knowledge-form {
  padding: 18px 18px 0;
}

.knowledge-form .el-select,
.knowledge-form .el-input {
  width: 100%;
}

.knowledge-form :deep(.el-input-number) {
  width: 180px;
}

.knowledge-upload {
  padding: 0 18px 18px;
}

.knowledge-upload :deep(.el-upload-dragger) {
  border-radius: 8px;
}

.upload-icon {
  margin-top: 8px;
  color: var(--primary);
  font-size: 42px;
}

.upload-text {
  color: var(--ink);
  font-weight: 650;
}

.upload-tip {
  margin-top: 8px;
  color: var(--subtle);
  font-size: 12px;
}

.result-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0 18px 18px;
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
}

.result-strip div {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-right: 1px solid var(--border);
  background: var(--surface-muted);
}

.result-strip div:last-child {
  border-right: 0;
}

.result-strip span {
  color: var(--subtle);
  font-size: 12px;
}

.result-strip strong {
  color: var(--ink);
  font-size: 18px;
}

.action-row {
  display: flex;
  gap: 10px;
  padding: 0 18px 18px;
}

.data-table {
  width: 100%;
}

@media (max-width: 1080px) {
  .workflow-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .flow-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .result-strip {
    grid-template-columns: 1fr;
  }

  .result-strip div {
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }

  .result-strip div:last-child {
    border-bottom: 0;
  }
}
</style>
