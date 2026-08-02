<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Connection,
  Delete,
  Edit,
  Hide,
  Key,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  Setting,
  TrendCharts,
  View,
  Warning
} from '@element-plus/icons-vue'
import {
  addModelConfig,
  deleteModelConfig,
  getModelAnalytics,
  getModelConfig,
  getModelMetrics,
  pageModelCallLogs,
  pageModelConfig,
  testModelConfig,
  updateModelConfig
} from '@/axios/model'

const loading = ref(false)
const submitting = ref(false)
const testing = ref(false)
const drawerVisible = ref(false)
const logVisible = ref(false)
const showApiKey = ref(false)
const formRef = ref()
const rows = ref([])
const total = ref(0)
const metricsData = ref({})
const analytics = ref({ trend: [], providerDistribution: [] })
const trendDays = ref(7)

const query = reactive({
  current: 1,
  size: 8,
  keyword: '',
  status: ''
})

const createForm = () => ({
  id: null,
  configName: '',
  providerName: '',
  protocol: 'openaiCompatible',
  baseURL: '',
  apiKey: '',
  removeApiKey: false,
  description: '',
  modelName: '',
  streaming: 1,
  thinking: 0,
  temperature: 0.7,
  topP: 1,
  maxTokens: 4096,
  timeoutMs: 60000,
  thinkingBudget: null,
  maxAttempts: 1,
  status: 1,
  headers: []
})

const form = reactive(createForm())

const logs = reactive({
  loading: false,
  model: null,
  records: [],
  total: 0,
  current: 1,
  size: 10,
  callSource: '',
  status: ''
})

const protocols = [
  { value: 'openaiCompatible', label: 'OpenAI 兼容协议' },
  { value: 'dashscope', label: 'DashScope' },
  { value: 'anthropic', label: 'Anthropic' },
  { value: 'ollama', label: 'Ollama' }
]

const rules = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  providerName: [{ required: true, message: '请输入模型供应商', trigger: 'blur' }],
  protocol: [{ required: true, message: '请选择接口协议', trigger: 'change' }],
  baseURL: [
    { required: true, message: '请输入 Base URL', trigger: 'blur' },
    { type: 'url', message: '请输入合法的 HTTP/HTTPS 地址', trigger: 'blur' }
  ],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  maxTokens: [{ required: true, message: '请输入最大输出 Token', trigger: 'change' }],
  timeoutMs: [{ required: true, message: '请输入超时时间', trigger: 'change' }],
  maxAttempts: [{ required: true, message: '请输入最大尝试次数', trigger: 'change' }]
}

const providerOptions = computed(() => (
  (analytics.value.providerDistribution || []).map((item) => item.providerName)
))

const drawerTitle = computed(() => form.id ? '编辑模型配置' : '新建模型配置')

const metricCards = computed(() => [
  {
    label: '模型配置',
    value: formatNumber(metricsData.value.total),
    note: `已启用 ${formatNumber(metricsData.value.enabled)}`,
    icon: Setting,
    tone: 'blue'
  },
  {
    label: '今日调用',
    value: formatNumber(metricsData.value.todayCalls),
    note: changeText(metricsData.value.callChangePercent, '较昨日同期'),
    icon: TrendCharts,
    tone: 'violet'
  },
  {
    label: '调用成功率',
    value: metricsData.value.successRate == null ? '--' : `${metricsData.value.successRate}%`,
    note: `失败 ${formatNumber(metricsData.value.failedCalls)} 次`,
    icon: Connection,
    tone: 'green'
  },
  {
    label: '平均耗时',
    value: metricsData.value.averageDurationMs == null
      ? '--'
      : formatDuration(metricsData.value.averageDurationMs),
    note: durationChangeText(metricsData.value.averageDurationChangeMs),
    icon: Warning,
    tone: 'amber'
  }
])

const trendChart = computed(() => {
  const values = (analytics.value.trend || []).map((item) => Number(item.calls || 0))
  if (!values.length) {
    return { points: '', area: '', max: 0 }
  }
  const width = 420
  const height = 150
  const paddingX = 14
  const paddingY = 16
  const max = Math.max(...values, 1)
  const usableWidth = width - paddingX * 2
  const usableHeight = height - paddingY * 2
  const points = values.map((value, index) => {
    const x = values.length === 1
      ? width / 2
      : paddingX + (index / (values.length - 1)) * usableWidth
    const y = height - paddingY - (value / max) * usableHeight
    return `${x.toFixed(1)},${y.toFixed(1)}`
  })
  return {
    points: points.join(' '),
    area: `${paddingX},${height - paddingY} ${points.join(' ')} ${width - paddingX},${height - paddingY}`,
    max
  }
})

const trendLabels = computed(() => {
  const data = analytics.value.trend || []
  if (!data.length) return []
  const indexes = [...new Set([0, Math.floor((data.length - 1) / 2), data.length - 1])]
  return indexes.map((index) => ({
    index,
    text: String(data[index]?.date || '').slice(5)
  }))
})

const loadDashboard = async () => {
  loading.value = true
  try {
    const [metricResult, analyticsResult, pageResult] = await Promise.all([
      getModelMetrics(),
      getModelAnalytics(trendDays.value),
      pageModelConfig({
        current: query.current,
        size: query.size,
        keyword: query.keyword || undefined,
        status: query.status === '' ? undefined : query.status
      })
    ])
    metricsData.value = metricResult || {}
    analytics.value = analyticsResult || { trend: [], providerDistribution: [] }
    rows.value = pageResult?.records || []
    total.value = Number(pageResult?.total || 0)
  } finally {
    loading.value = false
  }
}

const loadPage = async () => {
  loading.value = true
  try {
    const result = await pageModelConfig({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status === '' ? undefined : query.status
    })
    rows.value = result?.records || []
    total.value = Number(result?.total || 0)
  } finally {
    loading.value = false
  }
}

const reloadAnalytics = async () => {
  analytics.value = await getModelAnalytics(trendDays.value) || {
    trend: [],
    providerDistribution: []
  }
}

const resetForm = () => {
  Object.assign(form, createForm())
  showApiKey.value = false
}

const openCreate = async () => {
  resetForm()
  drawerVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async (row) => {
  resetForm()
  const detail = await getModelConfig(row.id)
  Object.assign(form, {
    ...createForm(),
    ...detail,
    id: detail?.id || row.id,
    apiKey: detail?.apiKey || '',
    headers: (detail?.headers || []).map((header) => ({
      ...header,
      remove: false,
      visible: false
    }))
  })
  drawerVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const addHeader = () => {
  form.headers.push({
    id: null,
    headerName: '',
    headerValue: '',
    remove: false,
    visible: false
  })
}

const removeHeader = (header, index) => {
  if (header.id) {
    header.remove = true
    return
  }
  form.headers.splice(index, 1)
}

const restoreHeader = (header) => {
  header.remove = false
}

const clearApiKey = () => {
  form.apiKey = ''
  form.removeApiKey = true
}

const buildPayload = () => ({
  id: form.id,
  configName: form.configName.trim(),
  providerName: form.providerName.trim(),
  protocol: form.protocol,
  baseURL: form.baseURL.trim(),
  apiKey: form.removeApiKey ? null : form.apiKey,
  removeApiKey: form.removeApiKey,
  description: form.description?.trim() || '',
  modelName: form.modelName.trim(),
  streaming: Number(form.streaming),
  thinking: Number(form.thinking),
  temperature: Number(form.temperature),
  topP: Number(form.topP),
  maxTokens: Number(form.maxTokens),
  timeoutMs: Number(form.timeoutMs),
  thinkingBudget: Number(form.thinking) === 1 && form.thinkingBudget
    ? Number(form.thinkingBudget)
    : null,
  maxAttempts: Number(form.maxAttempts),
  status: Number(form.status),
  headers: form.headers.map(({ visible, ...header }) => header)
})

const submitForm = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateModelConfig(buildPayload())
      ElMessage.success('模型配置已更新，后续调用立即生效')
    } else {
      await addModelConfig(buildPayload())
      ElMessage.success('模型配置创建成功')
    }
    drawerVisible.value = false
    await loadDashboard()
  } finally {
    submitting.value = false
  }
}

const runTest = async (source) => {
  let payload
  if (source === 'form') {
    await formRef.value?.validate()
    payload = buildPayload()
  } else {
    payload = { id: source.id }
  }
  testing.value = true
  try {
    const result = await testModelConfig(payload)
    if (result?.success) {
      ElMessage.success(`连接成功，耗时 ${formatDuration(result.durationMs)}`)
    } else {
      ElMessage.error(result?.errorMessage || '模型测试失败')
    }
    await loadDashboard()
  } finally {
    testing.value = false
  }
}

const removeModel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除模型配置“${row.configName}”吗？有关联记录时将保留历史调用快照。`,
      '删除模型配置',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  await deleteModelConfig(row.id)
  ElMessage.success('模型配置已删除')
  if (rows.value.length === 1 && query.current > 1) {
    query.current -= 1
  }
  await loadDashboard()
}

const openLogs = async (row) => {
  logs.model = row
  logs.current = 1
  logs.callSource = ''
  logs.status = ''
  logVisible.value = true
  await loadLogs()
}

const loadLogs = async () => {
  if (!logs.model?.id) return
  logs.loading = true
  try {
    const result = await pageModelCallLogs({
      current: logs.current,
      size: logs.size,
      modelConfigId: logs.model.id,
      callSource: logs.callSource || undefined,
      status: logs.status || undefined
    })
    logs.records = result?.records || []
    logs.total = Number(result?.total || 0)
  } finally {
    logs.loading = false
  }
}

const search = () => {
  query.current = 1
  loadPage()
}

const protocolLabel = (value) => (
  protocols.find((item) => item.value === value)?.label || value || '--'
)

const formatNumber = (value) => Number(value || 0).toLocaleString('zh-CN')

const formatDuration = (value) => {
  const duration = Number(value || 0)
  return duration >= 1000 ? `${(duration / 1000).toFixed(2)} s` : `${Math.round(duration)} ms`
}

const changeText = (value, prefix) => {
  if (value == null) return `${prefix} --`
  const number = Number(value)
  return `${prefix} ${number > 0 ? '+' : ''}${number}%`
}

const durationChangeText = (value) => {
  if (value == null) return '较昨日同期 --'
  const number = Number(value)
  if (number === 0) return '较昨日同期持平'
  return `较昨日同期 ${number > 0 ? '增加' : '减少'} ${formatDuration(Math.abs(number))}`
}

const statusType = (status) => ({
  SUCCESS: 'success',
  FAILED: 'danger',
  RUNNING: 'warning',
  CANCELLED: 'info'
}[status] || 'info')

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="model-page management-page">
    <div class="metric-grid management-metrics">
      <article v-for="item in metricCards" :key="item.label" class="metric-card management-metric-card">
        <span class="metric-icon management-metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <div>
          <small>{{ item.label }}</small>
          <strong>{{ item.value }}</strong>
          <p>{{ item.note }}</p>
        </div>
      </article>
    </div>

    <div class="content-grid management-content-grid">
      <main class="list-panel management-panel">
        <div class="panel-title management-panel-title">
          <div>
            <h3>模型配置</h3>
            <p>共 {{ total }} 条配置，仅管理聊天与文本生成模型</p>
          </div>
          <div class="filter-bar management-filter-bar">
            <el-input
              v-model="query.keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索配置名、供应商或模型名"
              @clear="search"
              @keyup.enter="search"
            />
            <el-select v-model="query.status" clearable placeholder="全部状态" @change="search">
              <el-option label="已启用" :value="1" />
              <el-option label="已停用" :value="0" />
            </el-select>
            <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
            <el-button :icon="Plus" type="primary" @click="openCreate">新建模型配置</el-button>
          </div>
        </div>

        <div v-if="rows.length" class="model-list">
          <article v-for="row in rows" :key="row.id" class="model-card management-data-card">
            <el-dropdown class="management-card-menu" trigger="click">
              <button class="management-card-menu-button" type="button" aria-label="模型操作">
                <el-icon class="management-card-menu-icon"><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="Edit" @click="openEdit(row)">编辑配置</el-dropdown-item>
                  <el-dropdown-item :icon="TrendCharts" @click="openLogs(row)">调用日志</el-dropdown-item>
                  <el-dropdown-item
                    :icon="Delete"
                    divided
                    class="danger-item"
                    @click="removeModel(row)"
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <header>
              <span class="provider-mark">{{ (row.providerName || 'M').slice(0, 1).toUpperCase() }}</span>
              <div class="model-heading">
                <div>
                  <h4>{{ row.configName }}</h4>
                  <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'" effect="light">
                    {{ Number(row.status) === 1 ? '已启用' : '已停用' }}
                  </el-tag>
                </div>
                <p>{{ row.description || '暂无描述' }}</p>
              </div>
            </header>

            <div class="model-facts">
              <span><small>供应商</small><b>{{ row.providerName }}</b></span>
              <span><small>接口协议</small><b>{{ protocolLabel(row.protocol) }}</b></span>
              <span><small>模型名称</small><b>{{ row.modelName }}</b></span>
              <span><small>最大输出</small><b>{{ formatNumber(row.maxTokens) }} Token</b></span>
            </div>

            <footer>
              <div>
                <b>今日 {{ formatNumber(row.todayCalls) }} 次</b>
                <small v-if="row.lastTestAt">
                  最近测试 {{ row.lastTestStatus === 'SUCCESS' ? '成功' : '失败' }}
                  · {{ formatDuration(row.lastTestDurationMs) }}
                </small>
                <small v-else>尚未测试连接</small>
              </div>
              <div>
                <el-button :loading="testing" @click="runTest(row)">测试</el-button>
                <el-button type="primary" plain @click="openEdit(row)">配置</el-button>
              </div>
            </footer>
          </article>
        </div>
        <el-empty v-else description="暂无符合条件的模型配置">
          <el-button type="primary" @click="openCreate">新建模型配置</el-button>
        </el-empty>

        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          class="pagination management-pagination"
          layout="total, prev, pager, next, sizes"
          :page-sizes="[8, 16, 32]"
          :total="total"
          @current-change="loadPage"
          @size-change="search"
        />
      </main>

      <aside class="analytics-column management-side-column">
        <section class="analytics-card trend-card management-side-card">
          <header>
            <div>
              <h3>调用趋势</h3>
              <p>仅统计智能体实际运行调用</p>
            </div>
            <el-segmented v-model="trendDays" :options="[7, 30]" size="small" @change="reloadAnalytics" />
          </header>
          <div class="chart-summary">
            <b>{{ formatNumber((analytics.trend || []).reduce((sum, item) => sum + Number(item.calls || 0), 0)) }}</b>
            <span>近 {{ trendDays }} 天调用</span>
          </div>
          <svg class="trend-svg" viewBox="0 0 420 150" role="img" aria-label="模型调用趋势">
            <defs>
              <linearGradient id="modelTrendFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#5b7cff" stop-opacity=".3" />
                <stop offset="100%" stop-color="#5b7cff" stop-opacity="0" />
              </linearGradient>
            </defs>
            <line x1="14" y1="134" x2="406" y2="134" stroke="#e9edf6" />
            <polygon v-if="trendChart.area" :points="trendChart.area" fill="url(#modelTrendFill)" />
            <polyline
              v-if="trendChart.points"
              :points="trendChart.points"
              fill="none"
              stroke="#5b7cff"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="3"
            />
          </svg>
          <div class="trend-labels">
            <span v-for="label in trendLabels" :key="label.index">{{ label.text }}</span>
          </div>
        </section>

        <section class="analytics-card provider-card management-side-card">
          <header>
            <div>
              <h3>供应商分布</h3>
              <p>按未删除配置数量统计</p>
            </div>
          </header>
          <div v-if="analytics.providerDistribution?.length" class="provider-list">
            <div v-for="item in analytics.providerDistribution" :key="item.providerName">
              <span><b>{{ item.providerName }}</b><em>{{ item.modelCount }} 个 · {{ item.percent }}%</em></span>
              <i><u :style="{ width: `${item.percent}%` }" /></i>
            </div>
          </div>
          <el-empty v-else description="暂无供应商数据" :image-size="70" />
        </section>
      </aside>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="min(760px, 94vw)"
      destroy-on-close
      class="model-drawer"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <section class="form-section">
          <div class="section-heading">
            <span>01</span>
            <div><h4>身份与连接</h4><p>配置名称用于平台内识别，模型名称会原样发送给供应商。</p></div>
          </div>
          <div class="form-grid">
            <el-form-item label="配置名称" prop="configName">
              <el-input v-model="form.configName" maxlength="100" placeholder="例如：生产环境 GPT-4.1" />
            </el-form-item>
            <el-form-item label="模型供应商" prop="providerName">
              <el-input v-model="form.providerName" maxlength="100" placeholder="例如：OpenAI、阿里云、本地部署" />
            </el-form-item>
            <el-form-item label="接口协议" prop="protocol">
              <el-select v-model="form.protocol">
                <el-option
                  v-for="item in protocols"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="form.modelName" maxlength="200" placeholder="例如：gpt-4.1、qwen-max" />
            </el-form-item>
            <el-form-item class="span-2" label="Base URL" prop="baseURL">
              <el-input v-model="form.baseURL" placeholder="https://api.example.com/v1" />
            </el-form-item>
            <el-form-item class="span-2" label="API Key（可选）">
              <el-input
                v-model="form.apiKey"
                :type="showApiKey ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="本地模型或 Header 鉴权可留空"
                @input="form.removeApiKey = false"
              >
                <template #prefix><el-icon><Key /></el-icon></template>
                <template #suffix>
                  <el-icon class="eye-button" @click="showApiKey = !showApiKey">
                    <component :is="showApiKey ? Hide : View" />
                  </el-icon>
                </template>
              </el-input>
              <el-button v-if="form.id && form.apiKey" link type="danger" @click="clearApiKey">
                清空密钥
              </el-button>
            </el-form-item>
            <el-form-item class="span-2" label="描述">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="说明使用场景、环境或限制"
              />
            </el-form-item>
          </div>
        </section>

        <section class="form-section">
          <div class="section-heading">
            <span>02</span>
            <div><h4>生成参数</h4><p>超时与最大尝试次数会真实应用到每次逻辑模型调用。</p></div>
          </div>
          <div class="form-grid">
            <el-form-item label="流式输出">
              <el-switch v-model="form.streaming" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="思考模式">
              <el-switch v-model="form.thinking" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="Temperature">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="2" />
            </el-form-item>
            <el-form-item label="Top P">
              <el-input-number v-model="form.topP" :min="0" :max="1" :step="0.05" :precision="2" />
            </el-form-item>
            <el-form-item label="最大输出 Token" prop="maxTokens">
              <el-input-number v-model="form.maxTokens" :min="1" :max="10000000" :step="256" />
            </el-form-item>
            <el-form-item v-if="Number(form.thinking) === 1" label="思考预算 Token">
              <el-input-number v-model="form.thinkingBudget" :min="1" :max="10000000" :step="256" />
            </el-form-item>
            <el-form-item label="超时时间（毫秒）" prop="timeoutMs">
              <el-input-number v-model="form.timeoutMs" :min="1000" :max="600000" :step="1000" />
            </el-form-item>
            <el-form-item label="最大尝试次数（含首次）" prop="maxAttempts">
              <el-input-number v-model="form.maxAttempts" :min="1" :max="10" />
            </el-form-item>
            <el-form-item label="配置状态">
              <el-radio-group v-model="form.status">
                <el-radio-button :value="1">启用</el-radio-button>
                <el-radio-button :value="0">停用</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </div>
        </section>

        <section class="form-section">
          <div class="section-heading header-heading">
            <span>03</span>
            <div><h4>自定义 Header</h4><p>Header 名称在当前配置内忽略大小写且不可重复。</p></div>
            <el-button :icon="Plus" @click="addHeader">新增 Header</el-button>
          </div>
          <div v-if="form.headers.length" class="header-list">
            <div
              v-for="(header, index) in form.headers"
              :key="header.id || index"
              class="header-row"
              :class="{ removed: header.remove }"
            >
              <template v-if="!header.remove">
                <el-input v-model="header.headerName" placeholder="Header 名称" />
                <el-input
                  v-model="header.headerValue"
                  :type="header.visible ? 'text' : 'password'"
                  autocomplete="new-password"
                  placeholder="Header 值"
                >
                  <template #suffix>
                    <el-icon class="eye-button" @click="header.visible = !header.visible">
                      <component :is="header.visible ? Hide : View" />
                    </el-icon>
                  </template>
                </el-input>
                <el-button circle text type="danger" :icon="Delete" @click="removeHeader(header, index)" />
              </template>
              <template v-else>
                <span>{{ header.headerName }} 将在保存时删除</span>
                <el-button link type="primary" @click="restoreHeader(header)">撤销</el-button>
              </template>
            </div>
          </div>
          <el-empty v-else description="未配置自定义 Header" :image-size="64" />
        </section>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button :loading="testing" @click="runTest('form')">测试连接</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">保存配置</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="logVisible" width="min(1120px, 94vw)" :title="`${logs.model?.configName || ''} · 调用日志`">
      <div class="log-filters">
        <el-select v-model="logs.callSource" clearable placeholder="全部来源" @change="loadLogs">
          <el-option label="智能体运行" value="AGENT_RUN" />
          <el-option label="手动测试" value="MANUAL_TEST" />
        </el-select>
        <el-select v-model="logs.status" clearable placeholder="全部状态" @change="loadLogs">
          <el-option label="运行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button :icon="Refresh" @click="loadLogs">刷新</el-button>
      </div>
      <el-table v-loading="logs.loading" :data="logs.records" stripe>
        <el-table-column label="开始时间" prop="startedAt" min-width="170" />
        <el-table-column label="来源" min-width="120">
          <template #default="{ row }">
            {{ row.callSource === 'MANUAL_TEST' ? '手动测试' : row.sourcePath || '智能体运行' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">{{ row.durationMs == null ? '--' : formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column label="Token" width="120">
          <template #default="{ row }">{{ row.totalTokens == null ? '--' : formatNumber(row.totalTokens) }}</template>
        </el-table-column>
        <el-table-column label="错误" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || '--' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="logs.current"
        v-model:page-size="logs.size"
        class="pagination"
        layout="total, prev, pager, next"
        :total="logs.total"
        @current-change="loadLogs"
      />
    </el-dialog>
  </section>
</template>

<style scoped>
.model-page {
  box-sizing: border-box;
  width: calc(100% + 60px);
  min-height: calc(100vh - 75px);
  margin: 0 -30px;
  padding: 24px;
  color: #172033;
  background:
    radial-gradient(circle at 8% 4%, rgba(91, 124, 255, .1), transparent 28%),
    linear-gradient(180deg, #f7f9fd 0%, #f4f6fb 100%);
}

.panel-title,
.analytics-card header,
.model-card header,
.model-card footer,
.section-heading,
.drawer-footer,
.log-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

h3,
h4,
p {
  margin: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.metric-card,
.list-panel,
.analytics-card {
  border: 1px solid #e8ecf4;
  border-radius: 18px;
  background: rgba(255, 255, 255, .95);
  box-shadow: 0 12px 34px rgba(31, 44, 77, .05);
  min-height: 118px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
}

.metric-icon {
  display: grid;
  flex: 0 0 46px;
  height: 46px;
  place-items: center;
  border-radius: 14px;
  font-size: 22px;
}

.metric-icon.blue { color: #4668e8; background: #edf1ff; }
.metric-icon.violet { color: #7b53de; background: #f3edff; }
.metric-icon.green { color: #17926b; background: #e8f8f2; }
.metric-icon.amber { color: #c27b16; background: #fff5e2; }

.metric-card small {
  color: #7b879c;
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 3px 0;
  color: #131a2a;
  font-size: 26px;
}

.metric-card p {
  color: #8b96a8;
  font-size: 12px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  align-items: start;
  gap: 18px;
}

.list-panel {
  min-width: 0;
  padding: 20px;
}

.panel-title h3,
.analytics-card h3 {
  font-size: 17px;
}

.panel-title p,
.analytics-card header p {
  margin-top: 4px;
  color: #8a95a8;
  font-size: 12px;
}

.panel-title {
  gap: 24px;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.panel-title > div:first-child {
  flex: 0 0 auto;
}

.filter-bar {
  display: grid;
  width: min(100%, 720px);
  margin-left: auto;
  grid-template-columns: minmax(220px, 1fr) 140px auto auto;
  gap: 10px;
}

.model-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.model-card {
  overflow: hidden;
  border: 1px solid #e5eaf3;
  border-radius: 16px;
  background: #fff;
}

.model-card header {
  align-items: flex-start;
  padding: 17px 52px 12px 17px;
}

.provider-mark {
  display: grid;
  flex: 0 0 42px;
  height: 42px;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(145deg, #5b7cff, #764dd9);
  font-weight: 800;
}

.model-heading {
  min-width: 0;
  flex: 1;
}

.model-heading > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-heading h4 {
  overflow: hidden;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-heading p {
  overflow: hidden;
  margin-top: 6px;
  color: #8a95a8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 0 17px 16px;
}

.model-facts span {
  min-width: 0;
  padding: 10px 11px;
  border-radius: 10px;
  background: #f7f8fb;
}

.model-facts small {
  display: block;
  margin-bottom: 4px;
  color: #929cad;
  font-size: 11px;
}

.model-facts b {
  display: block;
  overflow: hidden;
  color: #384258;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-card footer {
  padding: 12px 17px;
  border-top: 1px solid #eef1f6;
  background: #fbfcfe;
}

.model-card footer > div:first-child {
  min-width: 0;
}

.model-card footer b,
.model-card footer small {
  display: block;
}

.model-card footer b {
  color: #44516a;
  font-size: 12px;
}

.model-card footer small {
  overflow: hidden;
  margin-top: 3px;
  color: #949dae;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination {
  justify-content: flex-end;
  margin-top: 20px;
}

.analytics-column {
  display: grid;
  gap: 18px;
}

.analytics-card {
  padding: 18px;
}

.chart-summary {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 18px;
}

.chart-summary b {
  font-size: 25px;
}

.chart-summary span {
  color: #8a95a8;
  font-size: 11px;
}

.trend-svg {
  display: block;
  width: 100%;
  margin-top: 4px;
}

.trend-labels {
  display: flex;
  justify-content: space-between;
  color: #9aa3b3;
  font-size: 10px;
}

.provider-list {
  display: grid;
  gap: 15px;
  margin-top: 19px;
}

.provider-list span {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 7px;
  font-size: 12px;
}

.provider-list em {
  color: #8b96a8;
  font-style: normal;
}

.provider-list i {
  display: block;
  overflow: hidden;
  height: 7px;
  border-radius: 999px;
  background: #edf0f6;
}

.provider-list u {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #5b7cff, #8b62e6);
}

.form-section {
  margin-bottom: 18px;
  padding: 20px;
  border: 1px solid #e7ebf3;
  border-radius: 16px;
  background: #fff;
}

.section-heading {
  justify-content: flex-start;
  margin-bottom: 18px;
}

.section-heading > span {
  display: grid;
  flex: 0 0 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  color: #4e6ee8;
  background: #edf1ff;
  font-size: 12px;
  font-weight: 800;
}

.section-heading > div {
  flex: 1;
}

.section-heading h4 {
  font-size: 15px;
}

.section-heading p {
  margin-top: 3px;
  color: #8a95a8;
  font-size: 11px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.span-2 {
  grid-column: span 2;
}

.header-heading {
  justify-content: flex-start;
}

.header-list {
  display: grid;
  gap: 10px;
}

.header-row {
  display: grid;
  grid-template-columns: minmax(150px, .8fr) minmax(220px, 1.4fr) auto;
  align-items: center;
  gap: 10px;
}

.header-row.removed {
  display: flex;
  justify-content: space-between;
  padding: 11px 13px;
  border-radius: 10px;
  color: #a26b6b;
  background: #fff4f4;
}

.eye-button {
  cursor: pointer;
}

.drawer-footer {
  justify-content: flex-end;
}

.log-filters {
  justify-content: flex-start;
  margin-bottom: 15px;
}

.log-filters .el-select {
  width: 160px;
}

:deep(.danger-item) {
  color: #d95050;
}

:deep(.model-drawer .el-drawer__body) {
  padding: 0 20px 20px;
  background: #f7f8fb;
}

:deep(.model-drawer .el-drawer__footer) {
  border-top: 1px solid #e8ebf2;
}

:deep(.form-grid .el-input-number),
:deep(.form-grid .el-select) {
  width: 100%;
}

@media (max-width: 1250px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .analytics-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .model-page {
    width: calc(100% + 36px);
    min-height: 0;
    margin: -18px;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .model-list,
  .analytics-column {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    width: 100%;
    grid-template-columns: 1fr 1fr;
  }

  .panel-title {
    align-items: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 640px) {
  .model-page {
    padding: 14px;
  }

  .metric-grid,
  .filter-bar,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }

  .header-row {
    grid-template-columns: 1fr auto;
  }

  .header-row .el-input:nth-child(2) {
    grid-column: 1 / -1;
    grid-row: 2;
  }
}
</style>

<style scoped src="../../assets/management-page.css"></style>
