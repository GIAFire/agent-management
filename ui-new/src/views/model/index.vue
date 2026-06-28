<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModuleShell from '@/components/common/ModuleShell.vue'
import ResourceCard from '@/components/common/ResourceCard.vue'
import CreateCard from '@/components/common/CreateCard.vue'
import { listAgents } from '@/api/agent'
import { createModelConfig, deleteModelConfig, listModelConfigs, updateModelConfig } from '@/api/model'
import { asArray, formatTime } from '@/utils/collections'
import { withAuditFields } from '@/utils/payload'

const loading = ref(false)
const drawerVisible = ref(false)
const detailVisible = ref(false)
const keyword = ref('')
const selectedProvider = ref('全部')
const formRef = ref()
const models = ref([])
const agents = ref([])
const current = ref({})

const form = reactive({
  id: '',
  agentId: '',
  provider: 'OpenAI',
  baseURL: '',
  apiKey: '',
  modelName: '',
  isStream: 1,
  temperature: 0.7,
  topP: 0.9,
  maxTokens: 4096,
  timeoutMs: 60000,
  maxAttempts: 3,
  fallbackModelConfigId: '',
  status: 1
})

const providerOptions = [
  { label: '全部', value: '全部' },
  { label: 'OpenAI', value: 'OpenAI' },
  { label: 'DashScope', value: 'DashScope' },
  { label: 'Ollama', value: 'Ollama' },
  { label: 'Anthropic', value: 'Anthropic' },
  { label: 'Gemini', value: 'Gemini' }
]

const rules = {
  provider: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  baseURL: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
}

const agentNameById = computed(() => {
  return agents.value.reduce((map, item) => {
    map[String(item.id)] = item.agentName
    return map
  }, {})
})

const filteredModels = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return models.value.filter((item) => {
    const providerMatched = selectedProvider.value === '全部' || item.provider === selectedProvider.value
    const wordMatched = !word ||
      item.provider?.toLowerCase().includes(word) ||
      item.modelName?.toLowerCase().includes(word) ||
      item.baseURL?.toLowerCase().includes(word)
    return providerMatched && wordMatched
  })
})

const resetForm = () => {
  Object.assign(form, {
    id: '',
    agentId: '',
    provider: 'OpenAI',
    baseURL: '',
    apiKey: '',
    modelName: '',
    isStream: 1,
    temperature: 0.7,
    topP: 0.9,
    maxTokens: 4096,
    timeoutMs: 60000,
    maxAttempts: 3,
    fallbackModelConfigId: '',
    status: 1
  })
  formRef.value?.clearValidate?.()
}

const loadData = async () => {
  loading.value = true
  try {
    const [modelRes, agentRes] = await Promise.all([listModelConfigs(), listAgents()])
    models.value = asArray(modelRes)
    agents.value = asArray(agentRes)
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  resetForm()
  drawerVisible.value = true
}

const openEdit = (item) => {
  resetForm()
  Object.assign(form, {
    id: item.id || '',
    agentId: item.agentId || '',
    provider: item.provider || 'OpenAI',
    baseURL: item.baseURL || item.baseUrl || '',
    apiKey: item.apiKey || '',
    modelName: item.modelName || '',
    isStream: Number(item.isStream ?? 1),
    temperature: Number(item.temperature ?? 0.7),
    topP: Number(item.topP ?? 0.9),
    maxTokens: Number(item.maxTokens ?? 4096),
    timeoutMs: Number(item.timeoutMs ?? 60000),
    maxAttempts: Number(item.maxAttempts ?? 3),
    fallbackModelConfigId: item.fallbackModelConfigId || '',
    status: Number(item.status ?? 1)
  })
  drawerVisible.value = true
}

const openDetail = (item) => {
  current.value = item
  detailVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  const payload = withAuditFields({
    id: form.id || undefined,
    agentId: form.agentId || null,
    provider: form.provider,
    baseURL: form.baseURL,
    apiKey: form.apiKey,
    modelName: form.modelName,
    isStream: form.isStream,
    temperature: form.temperature,
    topP: form.topP,
    maxTokens: form.maxTokens,
    timeoutMs: form.timeoutMs,
    maxAttempts: form.maxAttempts,
    fallbackModelConfigId: form.fallbackModelConfigId || null,
    status: form.status
  }, form.id ? 'update' : 'create')

  if (form.id) {
    await updateModelConfig(payload)
    ElMessage.success('模型配置已更新')
  } else {
    await createModelConfig(payload)
    ElMessage.success('模型配置已创建')
  }
  drawerVisible.value = false
  await loadData()
}

const removeModel = async (item) => {
  await ElMessageBox.confirm('删除后关联该模型的智能体配置需要重新选择模型，确认继续？', '确认删除', {
    type: 'warning'
  })
  await deleteModelConfig(item.id)
  ElMessage.success('模型配置已删除')
  await loadData()
}

const handleAction = (action, item) => {
  if (action === 'view') openDetail(item)
  if (action === 'edit') openEdit(item)
  if (action === 'delete') removeModel(item)
}

const displayAgent = (item) => {
  return item.agentId ? agentNameById.value[String(item.agentId)] || `Agent ${item.agentId}` : '通用模型'
}

onMounted(loadData)
</script>

<template>
  <ModuleShell
    title="模型供应商"
    description="模型供应商模块把 Provider、Base URL、API Key、模型名和生成参数组合成可被智能体配置选择的模型能力。"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
      <el-button type="primary" @click="openCreate">新建模型配置</el-button>
    </template>

    <template #filters>
      <div class="filter-left">
        <el-segmented v-model="selectedProvider" :options="providerOptions" />
      </div>
      <div class="filter-right">
        <el-input v-model="keyword" clearable placeholder="搜索供应商、模型或地址" style="width: 320px">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </template>

    <section v-loading="loading" class="card-grid">
      <CreateCard title="新建模型配置" description="配置供应商凭证和模型参数，可在智能体配置中引用" @click="openCreate" />
      <ResourceCard
        v-for="item in filteredModels"
        :key="item.id"
        :title="item.modelName || item.provider"
        :description="item.baseURL || item.baseUrl || '暂无 Base URL'"
        :tags="[item.provider || 'Provider', displayAgent(item), Number(item.status) === 1 ? '启用' : '停用']"
        :meta="formatTime(item.updatedAt || item.createdAt)"
        :disabled="Number(item.status) !== 1"
        icon="Connection"
        accent="green"
        @action="(action) => handleAction(action, item)"
      />
    </section>

    <el-empty v-if="!loading && filteredModels.length === 0" class="empty-panel" description="暂无模型配置" />

    <el-drawer v-model="drawerVisible" :title="form.id ? '编辑模型配置' : '新建模型配置'" size="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="模型供应商" prop="provider">
            <el-select v-model="form.provider" filterable allow-create>
              <el-option label="OpenAI" value="OpenAI" />
              <el-option label="DashScope" value="DashScope" />
              <el-option label="Ollama" value="Ollama" />
              <el-option label="Anthropic" value="Anthropic" />
              <el-option label="Gemini" value="Gemini" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联智能体">
            <el-select v-model="form.agentId" filterable clearable placeholder="可选，不选则作为通用模型">
              <el-option v-for="agent in agents" :key="agent.id" :label="agent.agentName" :value="agent.id" />
            </el-select>
          </el-form-item>
          <el-form-item class="full-row" label="Base URL" prop="baseURL">
            <el-input v-model="form.baseURL" placeholder="https://api.openai.com/v1" />
          </el-form-item>
          <el-form-item label="模型名称" prop="modelName">
            <el-input v-model="form.modelName" placeholder="gpt-4.1 / qwen-plus" />
          </el-form-item>
          <el-form-item label="API Key" prop="apiKey">
            <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
          </el-form-item>
          <el-form-item label="流式输出">
            <el-switch v-model="form.isStream" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="Temperature">
            <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Top P">
            <el-input-number v-model="form.topP" :min="0" :max="1" :step="0.05" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大 Tokens">
            <el-input-number v-model="form.maxTokens" :min="1" :max="200000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="超时毫秒">
            <el-input-number v-model="form.timeoutMs" :min="1000" :step="1000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大重试">
            <el-input-number v-model="form.maxAttempts" :min="0" :max="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="兜底模型">
            <el-select v-model="form.fallbackModelConfigId" filterable clearable>
              <el-option
                v-for="item in models.filter((model) => model.id !== form.id)"
                :key="item.id"
                :label="`${item.provider} / ${item.modelName}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="detailVisible" title="模型配置详情" size="620px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ current.provider }}</el-descriptions-item>
        <el-descriptions-item label="模型名称">{{ current.modelName }}</el-descriptions-item>
        <el-descriptions-item label="Base URL">{{ current.baseURL || current.baseUrl }}</el-descriptions-item>
        <el-descriptions-item label="关联智能体">{{ displayAgent(current) }}</el-descriptions-item>
        <el-descriptions-item label="流式输出">{{ Number(current.isStream) === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="Temperature">{{ current.temperature }}</el-descriptions-item>
        <el-descriptions-item label="Top P">{{ current.topP }}</el-descriptions-item>
        <el-descriptions-item label="最大 Tokens">{{ current.maxTokens }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(current.updatedAt || current.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </ModuleShell>
</template>

