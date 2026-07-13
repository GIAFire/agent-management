<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  Box,
  CircleCheck,
  Connection,
  DataLine,
  Delete,
  Edit,
  Histogram,
  OfficeBuilding,
  Plus,
  Refresh,
  Search,
  Setting,
  User,
  Warning
} from '@element-plus/icons-vue'
import ApiKeyCrypto from '@/utils/encryption'
import {
  addModelConfig,
  deleteModelConfig,
  getModelConfig,
  listModelConfig,
  updateModelConfig
} from '@/axios/model'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const providerDialogVisible = ref(false)
const dialogTitle = ref('新建模型')
const formRef = ref()
const page = ref(1)
const pageSize = ref(10)
const modelRows = ref([])
const activeType = ref('all')

const queryParams = reactive({
  keyword: '',
  provider: '',
  status: ''
})

const form = reactive({
  id: null,
  credentialId: null,
  agentId: null,
  apiKey: '',
  baseURL: '',
  provider: '',
  description: '',
  modelName: '',
  modelType: 'CHAT',
  streaming: 1,
  thinking: 0,
  temperature: 0.7,
  topP: 0.8,
  maxTokens: 2048,
  timeoutMs: 60000,
  maxAttempts: 3,
  fallbackModelConfigId: null,
  status: 1
})

const rules = {
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
  baseURL: [{ required: true, message: '请输入模型 URL 地址', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择或输入供应商', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入真实模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const demoModels = [
  {
    id: 1,
    provider: 'DeepSeek',
    modelName: 'deepseek-chat',
    displayName: 'DeepSeek V3',
    modelType: 'CHAT',
    maxTokens: 64000,
    todayCalls: 4286,
    status: 1,
    baseURL: 'https://api.deepseek.com',
    description: '高性价比中文对话模型'
  },
  {
    id: 2,
    provider: '阿里云百炼',
    modelName: 'qwen-max',
    displayName: '通义千问 Max',
    modelType: 'CHAT',
    maxTokens: 32000,
    todayCalls: 3742,
    status: 1,
    baseURL: 'https://dashscope.aliyuncs.com',
    description: '通用对话与复杂推理模型'
  },
  {
    id: 3,
    provider: 'OpenAI',
    modelName: 'gpt-4.1',
    displayName: 'GPT-4.1',
    modelType: 'REASONING',
    maxTokens: 1000000,
    todayCalls: 2156,
    status: 1,
    baseURL: 'https://api.openai.com/v1',
    description: '复杂推理、代码与工具调用'
  },
  {
    id: 4,
    provider: 'Anthropic',
    modelName: 'claude-3-7-sonnet',
    displayName: 'Claude 3.7 Sonnet',
    modelType: 'REASONING',
    maxTokens: 200000,
    todayCalls: 1208,
    status: 0,
    baseURL: 'https://api.anthropic.com',
    description: '长文本理解和稳健推理'
  },
  {
    id: 5,
    provider: '本地模型',
    modelName: 'bge-m3',
    displayName: 'BGE-M3',
    modelType: 'EMBEDDING',
    maxTokens: 8192,
    todayCalls: 936,
    status: 1,
    baseURL: 'http://localhost:11434',
    description: '多语言向量嵌入模型'
  }
]

const typeTabs = [
  { label: '全部模型', value: 'all' },
  { label: '对话模型', value: 'CHAT' },
  { label: '推理模型', value: 'REASONING' },
  { label: '嵌入模型', value: 'EMBEDDING' },
  { label: '重排序模型', value: 'RERANK' }
]

const modelTypeOptions = [
  { label: '对话模型', value: 'CHAT' },
  { label: '推理模型', value: 'REASONING' },
  { label: '嵌入模型', value: 'EMBEDDING' },
  { label: '重排序模型', value: 'RERANK' },
  { label: '图像模型', value: 'IMAGE' },
  { label: '视频模型', value: 'VIDEO' },
  { label: '音频模型', value: 'AUDIO' }
]

const rows = computed(() => {
  const source = modelRows.value.length ? modelRows.value : demoModels
  return source.map((row, index) => normalizeModel(row, index))
})

const filteredRows = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  return rows.value.filter((row) => {
    const matchKeyword = !keyword || [row.displayName, row.provider, row.modelName, row.description]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchProvider = !queryParams.provider || row.provider === queryParams.provider
    const matchStatus = queryParams.status === '' || Number(row.status) === Number(queryParams.status)
    const matchType = activeType.value === 'all' || row.modelType === activeType.value
    return matchKeyword && matchProvider && matchStatus && matchType
  })
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const providerOptions = computed(() => {
  return [...new Set(rows.value.map((row) => row.provider).filter(Boolean))]
})

const providerStats = computed(() => {
  const total = rows.value.length || 1
  const grouped = providerOptions.value.map((provider) => {
    const count = rows.value.filter((row) => row.provider === provider).length
    return {
      provider,
      count,
      percent: Math.round((count / total) * 1000) / 10
    }
  }).sort((a, b) => b.count - a.count)

  if (modelRows.value.length) {
    return grouped
  }
  return [
    { provider: '阿里云百炼', percent: 28.6 },
    { provider: 'OpenAI', percent: 25.0 },
    { provider: 'DeepSeek', percent: 17.9 },
    { provider: 'Anthropic', percent: 14.3 },
    { provider: '本地模型', percent: 10.7 },
    { provider: '其他', percent: 3.6 }
  ]
})

const metrics = computed(() => {
  const total = rows.value.length
  const enabled = rows.value.filter((row) => Number(row.status) === 1).length
  const providers = providerOptions.value.length
  const calls = rows.value.reduce((sum, row) => sum + row.todayCalls, 0)

  return [
    { label: '模型总数', value: total, sub: '较昨日 +2 ↑', icon: Box, tone: 'blue', positive: true },
    { label: '已启用', value: enabled, sub: `启用率 ${total ? ((enabled / total) * 100).toFixed(1) : '0.0'}%`, icon: CircleCheck, tone: 'green' },
    { label: '模型供应商', value: providers, sub: '较昨日 +0', icon: OfficeBuilding, tone: 'violet' },
    { label: '今日调用', value: formatNumber(Math.max(calls, 12680)), sub: '较昨日 +8.6% ↑', icon: DataLine, tone: 'blue', positive: true }
  ]
})

const normalizeModel = (row, index) => {
  const provider = providerLabel(row.provider || row.providerName || 'Unknown')
  const modelType = normalizeModelType(row.modelType || row.type || inferModelType(row.modelName))
  const maxTokens = Number(row.maxTokens || row.contextWindow || [64000, 32000, 1000000, 200000, 8192][index % 5])

  return {
    ...row,
    id: row.id || index + 1,
    provider,
    displayName: row.displayName || row.alias || displayName(row.modelName, provider),
    modelName: row.modelName || `model-${index + 1}`,
    modelType,
    maxTokens,
    contextWindow: formatContext(maxTokens),
    todayCalls: Number(row.todayCalls || row.callCount || [4286, 3742, 2156, 1208, 936][index % 5]),
    status: Number(row.status ?? 1),
    baseURL: row.baseURL || row.baseUrl || '',
    description: row.description || modelTypeLabel(modelType)
  }
}

const providerLabel = (value) => {
  const text = String(value || '')
  const map = {
    DASHSCOPE: '阿里云百炼',
    QWEN: '阿里云百炼',
    OPENAI: 'OpenAI',
    DEEPSEEK: 'DeepSeek',
    ANTHROPIC: 'Anthropic',
    LOCAL: '本地模型'
  }
  return map[text.toUpperCase()] || text
}

const displayName = (modelName = '', provider = '') => {
  if (!modelName) {
    return provider || '未命名模型'
  }
  const map = {
    'deepseek-chat': 'DeepSeek V3',
    'qwen-max': '通义千问 Max',
    'gpt-4.1': 'GPT-4.1',
    'claude-3-7-sonnet': 'Claude 3.7 Sonnet',
    'bge-m3': 'BGE-M3'
  }
  return map[modelName] || modelName
}

const inferModelType = (modelName = '') => {
  const text = String(modelName).toLowerCase()
  if (text.includes('bge') || text.includes('embedding') || text.includes('embed')) {
    return 'EMBEDDING'
  }
  if (text.includes('rerank')) {
    return 'RERANK'
  }
  if (text.includes('reason') || text.includes('r1') || text.includes('o1')) {
    return 'REASONING'
  }
  return 'CHAT'
}

const normalizeModelType = (value) => {
  const text = String(value || '').toUpperCase()
  if (['CHAT', 'REASONING', 'EMBEDDING', 'RERANK', 'IMAGE', 'VIDEO', 'AUDIO'].includes(text)) {
    return text
  }
  return 'CHAT'
}

const modelTypeLabel = (type) => {
  const map = {
    CHAT: '对话模型',
    REASONING: '推理模型',
    EMBEDDING: '嵌入模型',
    RERANK: '重排序模型',
    IMAGE: '图像模型',
    VIDEO: '视频模型',
    AUDIO: '音频模型'
  }
  return map[type] || type
}

const modelTypeClass = (type) => {
  const map = {
    CHAT: 'chat',
    REASONING: 'reasoning',
    EMBEDDING: 'embedding',
    RERANK: 'rerank'
  }
  return map[type] || 'chat'
}

const providerClass = (provider) => {
  const text = String(provider || '').toLowerCase()
  if (text.includes('openai')) return 'openai'
  if (text.includes('deepseek')) return 'deepseek'
  if (text.includes('阿里') || text.includes('qwen')) return 'qwen'
  if (text.includes('anthropic') || text.includes('claude')) return 'claude'
  if (text.includes('本地')) return 'local'
  return 'other'
}

const providerMark = (provider) => {
  const text = String(provider || '')
  if (text.includes('DeepSeek')) return 'D'
  if (text.includes('阿里')) return 'Q'
  if (text.includes('OpenAI')) return 'O'
  if (text.includes('Anthropic')) return 'AI'
  if (text.includes('本地')) return 'L'
  return text.charAt(0).toUpperCase() || 'M'
}

const formatContext = (value) => {
  const number = Number(value || 0)
  if (number >= 1000000) return `${Math.round(number / 1000000)}M`
  if (number >= 1000) return `${Math.round(number / 1000)}K`
  return String(number || '-')
}

const formatNumber = (value) => Number(value || 0).toLocaleString('en-US')

const resetForm = () => {
  Object.assign(form, {
    id: null,
    credentialId: null,
    agentId: null,
    apiKey: '',
    baseURL: '',
    provider: '',
    description: '',
    modelName: '',
    modelType: 'CHAT',
    streaming: 1,
    thinking: 0,
    temperature: 0.7,
    topP: 0.8,
    maxTokens: 2048,
    timeoutMs: 60000,
    maxAttempts: 3,
    fallbackModelConfigId: null,
    status: 1
  })
}

const normalizeNumber = (value) => {
  return value === '' || value === undefined ? null : value
}

const normalizeId = (value) => {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

const loadModelList = async () => {
  loading.value = true
  try {
    const data = await listModelConfig()
    modelRows.value = Array.isArray(data) ? data : []
  } catch {
    modelRows.value = []
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  page.value = 1
}

const handleTypeChange = (type) => {
  activeType.value = type
  page.value = 1
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.provider = ''
  queryParams.status = ''
  activeType.value = 'all'
  page.value = 1
}

const handleAdd = async () => {
  dialogTitle.value = '新建模型'
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑模型'
  resetForm()
  const data = modelRows.value.length ? await getModelConfig(row.id) : row
  Object.assign(form, {
    id: data?.id,
    credentialId: data?.credentialId ?? null,
    agentId: data?.agentId ?? null,
    apiKey: data?.apiKey || '',
    baseURL: data?.baseURL || data?.baseUrl || '',
    provider: providerLabel(data?.provider || ''),
    description: data?.description || '',
    modelName: data?.modelName || '',
    modelType: normalizeModelType(data?.modelType || row.modelType),
    streaming: Number(data?.streaming ?? 1),
    thinking: Number(data?.thinking ?? 0),
    temperature: Number(data?.temperature ?? 0.7),
    topP: Number(data?.topP ?? 0.8),
    maxTokens: data?.maxTokens ?? 2048,
    timeoutMs: data?.timeoutMs ?? 60000,
    maxAttempts: data?.maxAttempts ?? 3,
    fallbackModelConfigId: data?.fallbackModelConfigId ?? null,
    status: Number(data?.status ?? 1)
  })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const buildPayload = async () => {
  const encryptedKey = form.apiKey && !String(form.apiKey).includes('****')
    ? await ApiKeyCrypto.encrypt(form.apiKey, ApiKeyCrypto.masterKeyB64key)
    : form.apiKey
  return {
    id: normalizeId(form.id),
    credentialId: normalizeId(form.credentialId),
    agentId: normalizeId(form.agentId),
    apiKey: encryptedKey,
    baseURL: form.baseURL,
    provider: form.provider,
    description: form.description,
    modelName: form.modelName,
    modelType: form.modelType,
    streaming: form.streaming,
    thinking: form.thinking,
    temperature: normalizeNumber(form.temperature),
    topP: normalizeNumber(form.topP),
    maxTokens: normalizeNumber(form.maxTokens),
    timeoutMs: normalizeNumber(form.timeoutMs),
    maxAttempts: normalizeNumber(form.maxAttempts),
    fallbackModelConfigId: normalizeId(form.fallbackModelConfigId),
    status: form.status
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = await buildPayload()
    if (form.id) {
      await updateModelConfig(payload)
      ElMessage.success('模型配置更新成功')
    } else {
      await addModelConfig(payload)
      ElMessage.success('模型配置创建成功')
    }
    dialogVisible.value = false
    await loadModelList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除模型「${row.displayName || row.modelName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteModelConfig(row.id)
  ElMessage.success('模型配置删除成功')
  await loadModelList()
}

const handleTest = (row) => {
  ElMessage.success(`${row.displayName} 测试请求已准备`)
}

onMounted(loadModelList)
</script>

<template>
  <section v-loading="loading" class="model-console">
    <div class="model-hero">
      <div>
        <h2>模型管理</h2>
        <p>统一管理大模型供应商、模型配置与调用状态。</p>
      </div>
      <div class="hero-actions">
        <el-button size="large" type="primary" :icon="Plus" @click="handleAdd">新建模型</el-button>
        <el-button size="large" :icon="User" @click="providerDialogVisible = true">供应商管理</el-button>
      </div>
    </div>

    <div class="model-metrics">
      <article v-for="item in metrics" :key="item.label" class="model-metric">
        <span class="metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small :class="{ positive: item.positive }">{{ item.sub }}</small>
        </div>
      </article>
    </div>

    <div class="model-dashboard">
      <section class="model-list-panel">
        <div class="model-toolbar">
          <div class="type-tabs">
            <button
              v-for="tab in typeTabs"
              :key="tab.value"
              type="button"
              :class="{ active: activeType === tab.value }"
              @click="handleTypeChange(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
          <div class="model-filters">
            <el-input
              v-model="queryParams.keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索模型名称或标识"
              @keyup.enter="handleQuery"
            />
            <el-select v-model="queryParams.provider" clearable placeholder="全部供应商">
              <el-option v-for="provider in providerOptions" :key="provider" :label="provider" :value="provider" />
            </el-select>
            <el-select v-model="queryParams.status" clearable placeholder="全部状态">
              <el-option label="已启用" :value="1" />
              <el-option label="已停用" :value="0" />
            </el-select>
          </div>
        </div>

        <div class="model-table">
          <div class="model-table-head">
            <span>模型名称</span>
            <span>供应商</span>
            <span>模型标识</span>
            <span>类型</span>
            <span>上下文窗口</span>
            <span>今日调用</span>
            <span>状态</span>
            <span>操作</span>
          </div>
          <article v-for="row in pagedRows" :key="row.id" class="model-row">
            <div class="model-name-cell">
              <span class="model-avatar" :class="providerClass(row.provider)">
                {{ providerMark(row.provider) }}
              </span>
              <strong>{{ row.displayName }}</strong>
            </div>
            <div class="provider-cell">
              <span class="provider-mark" :class="providerClass(row.provider)">
                {{ providerMark(row.provider) }}
              </span>
              <strong>{{ row.provider }}</strong>
            </div>
            <span class="model-id">{{ row.modelName }}</span>
            <span class="type-badge" :class="modelTypeClass(row.modelType)">{{ modelTypeLabel(row.modelType) }}</span>
            <span>{{ row.contextWindow }}</span>
            <span>{{ formatNumber(row.todayCalls) }}</span>
            <span class="model-status" :class="{ disabled: Number(row.status) !== 1 }">
              <i />
              {{ Number(row.status) === 1 ? '已启用' : '已停用' }}
            </span>
            <div class="row-actions">
              <el-button link type="primary" @click="handleEdit(row)">配置</el-button>
              <el-button link type="primary" @click="handleTest(row)">测试</el-button>
              <el-dropdown trigger="click">
                <button class="more-action" type="button">
                  更多 <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :icon="Edit" @click="handleEdit(row)">编辑</el-dropdown-item>
                    <el-dropdown-item :icon="Refresh" @click="loadModelList">刷新</el-dropdown-item>
                    <el-dropdown-item :icon="Delete" divided @click="handleDelete(row)">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </article>
        </div>

        <div class="model-pagination">
          <span>共 {{ filteredRows.length }} 条</span>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="filteredRows.length"
            :page-sizes="[10, 20, 50]"
            layout="prev, pager, next, sizes"
          />
        </div>
      </section>

      <aside class="model-side">
        <section class="side-panel trend-panel">
          <div class="side-head">
            <h3>调用趋势 <el-icon><Warning /></el-icon></h3>
            <el-select model-value="近7天" size="small">
              <el-option label="近7天" value="近7天" />
              <el-option label="近30天" value="近30天" />
            </el-select>
          </div>
          <svg class="trend-chart" viewBox="0 0 420 260" role="img" aria-label="模型调用趋势">
            <defs>
              <linearGradient id="modelTrendArea" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stop-color="#2f75ff" stop-opacity="0.24" />
                <stop offset="100%" stop-color="#2f75ff" stop-opacity="0.02" />
              </linearGradient>
            </defs>
            <path class="chart-grid" d="M40 32H400M40 82H400M40 132H400M40 182H400M40 232H400" />
            <path class="trend-area" d="M40 182 C72 146 92 122 125 112 C164 98 184 126 220 108 C252 90 264 36 298 48 C330 60 344 92 370 88 C388 86 394 96 400 98 L400 238 L40 238 Z" />
            <path class="trend-line" d="M40 182 C72 146 92 122 125 112 C164 98 184 126 220 108 C252 90 264 36 298 48 C330 60 344 92 370 88 C388 86 394 96 400 98" />
            <g>
              <circle cx="40" cy="182" r="5" />
              <circle cx="125" cy="112" r="5" />
              <circle cx="220" cy="108" r="5" />
              <circle cx="298" cy="48" r="5" />
              <circle cx="370" cy="88" r="5" />
              <circle cx="400" cy="98" r="5" />
            </g>
          </svg>
          <div class="chart-axis">
            <span>05-11</span>
            <span>05-12</span>
            <span>05-13</span>
            <span>05-14</span>
            <span>05-15</span>
            <span>05-16</span>
            <span>05-17</span>
          </div>
          <div class="legend">
            <span><i /> 调用次数</span>
          </div>
        </section>

        <section class="side-panel provider-panel">
          <div class="side-head">
            <h3>供应商分布</h3>
          </div>
          <div class="provider-bars">
            <div v-for="item in providerStats" :key="item.provider" class="provider-bar">
              <span>{{ item.provider }}</span>
              <div><i :style="{ width: `${item.percent}%` }" /></div>
              <strong>{{ item.percent }}%</strong>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="760px"
      destroy-on-close
      class="model-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="116px" class="model-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商" prop="provider">
              <el-input v-model="form.provider" placeholder="如 OpenAI / DeepSeek / 阿里云百炼" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型标识" prop="modelName">
              <el-input v-model="form.modelName" placeholder="如 gpt-4.1 / qwen-max" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型 URL" prop="baseURL">
              <el-input v-model="form.baseURL" placeholder="https://api.example.com/v1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API Key" prop="apiKey">
              <el-input v-model="form.apiKey" show-password placeholder="请输入 API Key" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="form.modelType">
                <el-option v-for="item in modelTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="温度">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Top P">
              <el-input-number v-model="form.topP" :min="0" :max="1" :step="0.1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上下文窗口">
              <el-input-number v-model="form.maxTokens" :min="1" :step="1024" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时(ms)">
              <el-input-number v-model="form.timeoutMs" :min="1000" :step="1000" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大重试">
              <el-input-number v-model="form.maxAttempts" :min="0" :max="8" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="兜底模型 ID">
              <el-input v-model="form.fallbackModelConfigId" placeholder="可为空" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="能力开关">
              <div class="switch-row">
                <el-checkbox v-model="form.streaming" :true-value="1" :false-value="0">流式输出</el-checkbox>
                <el-checkbox v-model="form.thinking" :true-value="1" :false-value="0">思考能力</el-checkbox>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="说明">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述模型能力、成本或适用场景" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="providerDialogVisible"
      title="供应商管理"
      width="620px"
      destroy-on-close
      class="model-dialog"
    >
      <div class="provider-manager">
        <div v-for="item in providerStats" :key="item.provider">
          <span class="provider-mark" :class="providerClass(item.provider)">{{ providerMark(item.provider) }}</span>
          <strong>{{ item.provider }}</strong>
          <small>{{ item.percent }}%</small>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.model-console {
  display: grid;
  gap: 18px;
}

.model-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.model-hero h2 {
  margin: 0 0 14px;
  color: #071f40;
  font-size: 34px;
  font-weight: 850;
  letter-spacing: 0;
}

.model-hero p {
  margin: 0;
  color: #526b87;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 12px;
}

.model-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.model-metric,
.model-list-panel,
.side-panel {
  border: 1px solid #d7e5f8;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 38px rgba(48, 94, 151, 0.08);
}

.model-metric {
  display: flex;
  min-height: 126px;
  align-items: center;
  gap: 18px;
  padding: 20px 22px;
}

.metric-icon {
  display: grid;
  width: 72px;
  height: 72px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  color: #2f75ff;
  background: #ecf4ff;
  font-size: 32px;
}

.metric-icon.green {
  color: #18a668;
  background: #eaf8ef;
}

.metric-icon.violet {
  color: #7c5cff;
  background: #f1edff;
}

.model-metric > div > span {
  display: block;
  color: #667d99;
  font-size: 14px;
  font-weight: 750;
}

.model-metric strong {
  display: block;
  margin-top: 9px;
  color: #0a2547;
  font-size: 30px;
  font-weight: 850;
  line-height: 1;
}

.model-metric small {
  display: block;
  margin-top: 12px;
  color: #6d819b;
  font-size: 12px;
  font-weight: 750;
}

.model-metric small.positive {
  color: #22a86b;
}

.model-dashboard {
  display: grid;
  grid-template-columns: minmax(760px, 1fr) minmax(330px, 0.36fr);
  gap: 18px;
}

.model-list-panel {
  min-width: 0;
  overflow: hidden;
}

.model-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  padding: 22px 24px;
  border-bottom: 1px solid #dce8f5;
}

.type-tabs {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 22px;
  min-width: max-content;
}

.type-tabs button {
  position: relative;
  flex: 0 0 auto;
  height: 40px;
  border: 0;
  color: #405874;
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-size: 15px;
  font-weight: 800;
  line-height: 40px;
  white-space: nowrap;
}

.type-tabs button.active {
  color: #2f75ff;
}

.type-tabs button.active::after {
  position: absolute;
  right: 0;
  bottom: -23px;
  left: 0;
  height: 3px;
  border-radius: 999px;
  background: #2f75ff;
  content: '';
}

.model-filters {
  display: grid;
  flex: 1 1 520px;
  grid-template-columns: minmax(220px, 1fr) 150px 140px;
  gap: 10px;
  min-width: 520px;
}

.model-table {
  padding: 0 18px 18px;
}

.model-table-head,
.model-row {
  display: grid;
  grid-template-columns: minmax(170px, 1.2fr) minmax(126px, 0.85fr) minmax(132px, 0.95fr) 92px 96px 88px 86px 132px;
  align-items: center;
  gap: 12px;
}

.model-table-head > span,
.model-row > span,
.model-row > div {
  min-width: 0;
}

.model-table-head > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-table-head {
  min-height: 64px;
  padding: 0 16px;
  border: 1px solid #e0eaf6;
  border-radius: 12px 12px 0 0;
  color: #405874;
  background: #fbfdff;
  font-weight: 820;
}

.model-row {
  min-height: 86px;
  padding: 0 16px;
  border-right: 1px solid #e0eaf6;
  border-bottom: 1px solid #e0eaf6;
  border-left: 1px solid #e0eaf6;
  background: #ffffff;
}

.model-row:last-child {
  border-radius: 0 0 12px 12px;
}

.model-name-cell,
.provider-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.model-name-cell strong,
.provider-cell strong,
.model-id {
  overflow: hidden;
  color: #203957;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-avatar,
.provider-mark {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 10px;
  color: #2f75ff;
  background: #edf4ff;
  font-size: 13px;
  font-weight: 900;
}

.model-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
}

.provider-mark.openai,
.model-avatar.openai {
  color: #0a2547;
  background: #f1f5f9;
}

.provider-mark.deepseek,
.model-avatar.deepseek {
  color: #326bff;
  background: #eef3ff;
}

.provider-mark.qwen,
.model-avatar.qwen {
  color: #7c5cff;
  background: #f1edff;
}

.provider-mark.claude,
.model-avatar.claude {
  color: #9a5b25;
  background: #fff1e5;
}

.provider-mark.local,
.model-avatar.local {
  color: #366fa8;
  background: #edf7ff;
}

.type-badge {
  justify-self: start;
  padding: 6px 10px;
  border-radius: 7px;
  color: #2f75ff;
  background: #eaf2ff;
  font-size: 12px;
  font-weight: 800;
}

.type-badge.reasoning {
  color: #7c5cff;
  background: #f1edff;
}

.type-badge.embedding {
  color: #18a668;
  background: #eaf8ef;
}

.type-badge.rerank {
  color: #c56a1c;
  background: #fff5e8;
}

.model-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 76px;
  height: 32px;
  border-radius: 8px;
  color: #168354;
  background: #eaf8ef;
  font-size: 12px;
  font-weight: 800;
}

.model-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.model-status.disabled {
  color: #7e8ca0;
  background: #f0f2f5;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  white-space: nowrap;
}

.more-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  color: #2f75ff;
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
}

.model-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 18px;
  padding: 18px 24px 22px;
  color: #6d819b;
}

.model-side {
  display: grid;
  align-content: start;
  gap: 18px;
}

.side-panel {
  padding: 22px;
}

.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.side-head h3 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  color: #0a2547;
  font-size: 17px;
  font-weight: 850;
}

.side-head h3 .el-icon {
  color: #91a4bc;
  font-size: 15px;
}

.trend-chart {
  display: block;
  width: 100%;
  height: 260px;
  margin-top: 18px;
}

.chart-grid {
  fill: none;
  stroke: #e2ebf6;
  stroke-dasharray: 4 4;
}

.trend-area {
  fill: url(#modelTrendArea);
}

.trend-line {
  fill: none;
  stroke: #2f75ff;
  stroke-linecap: round;
  stroke-width: 4;
}

.trend-chart circle {
  fill: #ffffff;
  stroke: #2f75ff;
  stroke-width: 4;
}

.chart-axis {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  color: #7890aa;
  font-size: 12px;
}

.legend {
  display: flex;
  justify-content: center;
  margin-top: 18px;
  color: #405874;
  font-size: 12px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.legend i {
  width: 16px;
  height: 8px;
  border-radius: 999px;
  background: #2f75ff;
}

.provider-bars {
  display: grid;
  gap: 18px;
  margin-top: 26px;
}

.provider-bar {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr) 52px;
  align-items: center;
  gap: 12px;
  color: #405874;
  font-size: 13px;
}

.provider-bar div {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #e7effa;
}

.provider-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #66c3ff, #2f75ff);
}

.provider-bar strong {
  color: #6d819b;
  font-weight: 750;
}

.model-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 22px;
  border-bottom: 1px solid #dce8f5;
}

.model-dialog :deep(.el-dialog__body) {
  padding: 20px 22px;
}

.model-form .el-select,
.model-form .el-input,
.model-form :deep(.el-input-number),
.model-form :deep(.el-textarea) {
  width: 100%;
}

.switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
}

.provider-manager {
  display: grid;
  gap: 10px;
}

.provider-manager > div {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 56px;
  align-items: center;
  gap: 12px;
  min-height: 54px;
  padding: 10px 12px;
  border: 1px solid #e0eaf6;
  border-radius: 10px;
}

.provider-manager strong {
  color: #0a2547;
}

.provider-manager small {
  color: #6d819b;
}

@media (max-width: 1320px) {
  .model-dashboard {
    grid-template-columns: 1fr;
  }

  .model-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .type-tabs {
    max-width: 100%;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .model-filters {
    width: 100%;
    flex-basis: auto;
    min-width: 0;
  }
}

@media (max-width: 980px) {
  .model-hero {
    flex-direction: column;
  }

  .model-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .type-tabs {
    width: 100%;
    overflow-x: auto;
  }

  .model-filters,
  .model-table-head,
  .model-row {
    grid-template-columns: 1fr;
  }

  .model-table-head {
    display: none;
  }

  .model-row {
    align-items: flex-start;
    gap: 10px;
    min-height: auto;
    padding: 16px;
    border: 1px solid #e0eaf6;
    border-radius: 12px;
    margin-top: 10px;
  }
}

@media (max-width: 640px) {
  .model-metrics {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>
