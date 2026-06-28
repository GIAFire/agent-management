<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModuleShell from '@/components/common/ModuleShell.vue'
import ResourceCard from '@/components/common/ResourceCard.vue'
import CreateCard from '@/components/common/CreateCard.vue'
import RelationGraph from '@/components/common/RelationGraph.vue'
import { createAgent, deleteAgent, listAgents, updateAgent } from '@/api/agent'
import { listAgentConfigs } from '@/api/agentConfig'
import { listModelConfigs } from '@/api/model'
import { asArray, formatTime } from '@/utils/collections'
import { withAuditFields } from '@/utils/payload'

const loading = ref(false)
const drawerVisible = ref(false)
const detailVisible = ref(false)
const current = ref({})
const formRef = ref()
const keyword = ref('')
const selectedType = ref('全部')
const agents = ref([])
const configs = ref([])
const models = ref([])

const form = reactive({
  id: '',
  agentKey: '',
  agentName: '',
  description: '',
  agentType: 'HARNESS',
  status: 2
})

const typeOptions = [
  { label: '全部', value: '全部' },
  { label: 'HARNESS', value: 'HARNESS' },
  { label: 'REACT', value: 'REACT' }
]

const rules = {
  agentKey: [{ required: true, message: '请输入智能体编码', trigger: 'blur' }],
  agentName: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
  agentType: [{ required: true, message: '请选择智能体类型', trigger: 'change' }]
}

const relationMap = computed(() => {
  const configCount = new Map()
  const modelCount = new Map()
  configs.value.forEach((item) => {
    const key = String(item.agentId || '')
    configCount.set(key, (configCount.get(key) || 0) + 1)
  })
  models.value.forEach((item) => {
    const key = String(item.agentId || '')
    if (key) {
      modelCount.set(key, (modelCount.get(key) || 0) + 1)
    }
  })
  return { configCount, modelCount }
})

const filteredAgents = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return agents.value.filter((item) => {
    const typeMatched = selectedType.value === '全部' || item.agentType === selectedType.value
    const wordMatched = !word ||
      item.agentName?.toLowerCase().includes(word) ||
      item.agentKey?.toLowerCase().includes(word) ||
      item.description?.toLowerCase().includes(word)
    return typeMatched && wordMatched
  })
})

const resetForm = () => {
  Object.assign(form, {
    id: '',
    agentKey: '',
    agentName: '',
    description: '',
    agentType: 'HARNESS',
    status: 2
  })
  formRef.value?.clearValidate?.()
}

const loadData = async () => {
  loading.value = true
  try {
    const [agentRes, configRes, modelRes] = await Promise.all([
      listAgents(),
      listAgentConfigs(),
      listModelConfigs()
    ])
    agents.value = asArray(agentRes)
    configs.value = asArray(configRes)
    models.value = asArray(modelRes)
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
    agentKey: item.agentKey || '',
    agentName: item.agentName || '',
    description: item.description || '',
    agentType: item.agentType || 'HARNESS',
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
    agentKey: form.agentKey,
    agentName: form.agentName,
    description: form.description,
    agentType: form.agentType,
    status: form.status
  }, form.id ? 'update' : 'create')

  if (form.id) {
    await updateAgent(payload)
    ElMessage.success('智能体已更新')
  } else {
    await createAgent(payload)
    ElMessage.success('智能体已创建')
  }
  drawerVisible.value = false
  await loadData()
}

const removeAgent = async (item) => {
  const configTotal = relationMap.value.configCount.get(String(item.id)) || 0
  await ElMessageBox.confirm(
    configTotal ? `该智能体已有 ${configTotal} 个配置版本，删除后会影响编排关系。确认继续？` : '删除后无法恢复，确认继续？',
    '确认删除',
    { type: 'warning' }
  )
  await deleteAgent(item.id)
  ElMessage.success('智能体已删除')
  await loadData()
}

const handleAction = (action, item) => {
  if (action === 'view') openDetail(item)
  if (action === 'edit') openEdit(item)
  if (action === 'delete') removeAgent(item)
}

onMounted(loadData)
</script>

<template>
  <ModuleShell
    title="智能体"
    description="智能体创建模块保存基础身份信息，包括唯一编码、名称、类型和状态；发布配置时会选择这里的智能体作为编排中心。"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
      <el-button type="primary" @click="openCreate">新建智能体</el-button>
    </template>

    <template #filters>
      <div class="filter-left">
        <el-segmented v-model="selectedType" :options="typeOptions" />
      </div>
      <div class="filter-right">
        <el-input v-model="keyword" clearable placeholder="搜索名称、编码或描述" style="width: 320px">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </template>

    <section v-loading="loading" class="card-grid">
      <CreateCard title="新建智能体" description="先创建基础身份，再进入智能体配置发布编排版本" @click="openCreate" />
      <ResourceCard
        v-for="item in filteredAgents"
        :key="item.id"
        :title="item.agentName"
        :description="item.description"
        :tags="[item.agentType || 'HARNESS', Number(item.status) === 1 ? '启用' : Number(item.status) === 2 ? '草稿' : '停用']"
        :meta="`${relationMap.configCount.get(String(item.id)) || 0} 配置 / ${relationMap.modelCount.get(String(item.id)) || 0} 模型`"
        :disabled="Number(item.status) === 0"
        icon="Monitor"
        accent="blue"
        @action="(action) => handleAction(action, item)"
      />
    </section>

    <el-empty v-if="!loading && filteredAgents.length === 0" class="empty-panel" description="暂无智能体" />

    <el-drawer v-model="drawerVisible" :title="form.id ? '编辑智能体' : '新建智能体'" size="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="智能体名称" prop="agentName">
            <el-input v-model="form.agentName" placeholder="例如：售后服务智能体" />
          </el-form-item>
          <el-form-item label="唯一编码" prop="agentKey">
            <el-input v-model="form.agentKey" placeholder="customer-service-agent" />
          </el-form-item>
          <el-form-item label="智能体类型" prop="agentType">
            <el-select v-model="form.agentType">
              <el-option label="HARNESS" value="HARNESS" />
              <el-option label="REACT" value="REACT" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
              <el-option label="草稿" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item class="full-row" label="描述">
            <el-input v-model="form.description" type="textarea" :rows="5" placeholder="描述智能体职责、边界和适用场景" />
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

    <el-drawer v-model="detailVisible" title="智能体详情" size="620px">
      <RelationGraph
        :agent-name="current.agentName"
        :model-name="`${relationMap.modelCount.get(String(current.id)) || 0} 个模型配置`"
        :config-name="`${relationMap.configCount.get(String(current.id)) || 0} 个发布版本`"
        :tool-count="0"
      />
      <el-descriptions class="mt-20" :column="1" border>
        <el-descriptions-item label="ID">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="编码">{{ current.agentKey }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ current.agentName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ current.agentType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ Number(current.status) === 1 ? '启用' : Number(current.status) === 2 ? '草稿' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(current.updatedAt || current.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ current.description }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </ModuleShell>
</template>

<style scoped>
.mt-20 {
  margin-top: 20px;
}
</style>
