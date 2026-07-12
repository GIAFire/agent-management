<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Briefcase,
  Check,
  Close,
  Download,
  Key,
  Lightning,
  MagicStick,
  Monitor,
} from '@element-plus/icons-vue'
import { addAgent, deleteAgent, getAgent, listAgent, updateAgent } from '@/axios/agent'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('创建智能体')
const wizardStep = ref(1)
const formRef = ref()
const agentRows = ref([])
const activeFilter = ref('全部')

const sampleRows = [
  {
    id: 'demo-1',
    agentName: '数据分析助手',
    agentKey: 'data-analyst',
    agentType: 'HARNESS',
    currentVersionId: 'v2.4',
    modelName: 'Qwen3-235B',
    status: 1,
    description: '数据分析与可视化'
  },
  {
    id: 'demo-2',
    agentName: '合同审查助手',
    agentKey: 'contract-review',
    agentType: 'HARNESS',
    currentVersionId: 'v1.8',
    modelName: 'DeepSeek-V3',
    status: 1,
    description: '合同风险识别与审查'
  },
  {
    id: 'demo-3',
    agentName: '客服应答助手',
    agentKey: 'service-agent',
    agentType: 'REACT',
    currentVersionId: 'v3.1',
    modelName: 'Qwen3-32B',
    status: 1,
    description: '智能客服与问答'
  },
  {
    id: 'demo-4',
    agentName: '报告生成助手',
    agentKey: 'report-agent',
    agentType: 'HARNESS',
    currentVersionId: 'v1.3',
    modelName: 'DeepSeek-V3',
    status: 2,
    description: '自动生成业务报告'
  }
]

const form = reactive({
  id: null,
  agentKey: '',
  agentName: '',
  description: '',
  agentType: 'HARNESS',
  currentVersionId: null,
  status: 1
})

const rules = {
  agentKey: [{ required: true, message: '请输入智能体编码', trigger: 'blur' }],
  agentName: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
  agentType: [{ required: true, message: '请选择智能体类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const agentTypes = [
  {
    value: 'HARNESS',
    title: 'Harness Agent',
    desc: '面向复杂任务，支持 Plan、Workspace、Sandbox 与多 Agent 协作',
    icon: Briefcase,
    tag: '推荐'
  },
  {
    value: 'REACT',
    title: 'ReAct Agent',
    desc: '通过“思考-行动-观察”循环处理轻量工具调用任务',
    icon: MagicStick
  },
  {
    value: 'TEMPLATE',
    title: '从企业模板创建',
    desc: '复用已验证的模型、工具、知识库与权限组合',
    icon: Lightning
  }
]

const sourceRows = computed(() => agentRows.value.length ? agentRows.value : sampleRows)

const filteredRows = computed(() => {
  if (activeFilter.value === '运行中') {
    return sourceRows.value.filter((row) => Number(row.status) === 1)
  }
  if (activeFilter.value === '需关注') {
    return sourceRows.value.filter((row) => Number(row.status) !== 1)
  }
  return sourceRows.value
})

const stats = computed(() => {
  const total = sourceRows.value.length
  const running = sourceRows.value.filter((row) => Number(row.status) === 1).length
  return [
    { label: '智能体总数', value: total || 32, note: `${running || 24} 个正在运行` },
    { label: '已发布版本', value: 86, note: '本周新增 7 个' },
    { label: '今日对话', value: '4,692', note: '较昨日 +18.2%' }
  ]
})

const statusMeta = (status) => {
  const value = Number(status)
  if (value === 1) {
    return { text: '运行中', className: 'running' }
  }
  if (value === 2) {
    return { text: '需关注', className: 'warning' }
  }
  return { text: '已停用', className: 'muted' }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    agentKey: '',
    agentName: '',
    description: '',
    agentType: 'HARNESS',
    currentVersionId: null,
    status: 1
  })
}

const normalizeId = (value) => {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

const loadAgentList = async () => {
  loading.value = true
  try {
    const data = await listAgent()
    agentRows.value = Array.isArray(data) ? data : []
  } catch {
    agentRows.value = []
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  dialogTitle.value = '创建智能体'
  wizardStep.value = 1
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const handleEdit = async (row) => {
  if (String(row.id).startsWith('demo-')) {
    ElMessage.info('示例智能体仅用于展示，请先创建真实智能体')
    return
  }

  dialogTitle.value = '编辑智能体'
  wizardStep.value = 2
  resetForm()
  const data = await getAgent(row.id)
  Object.assign(form, {
    id: data?.id,
    agentKey: data?.agentKey || '',
    agentName: data?.agentName || '',
    description: data?.description || '',
    agentType: data?.agentType || 'HARNESS',
    currentVersionId: data?.currentVersionId ?? null,
    status: Number(data?.status ?? 1)
  })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const buildPayload = () => ({
  id: normalizeId(form.id),
  agentKey: form.agentKey,
  agentName: form.agentName,
  description: form.description,
  agentType: form.agentType === 'TEMPLATE' ? 'HARNESS' : form.agentType,
  currentVersionId: normalizeId(form.currentVersionId),
  status: form.status
})

const nextStep = async () => {
  if (wizardStep.value === 1) {
    wizardStep.value = 2
    return
  }
  if (wizardStep.value === 2) {
    await formRef.value?.validateField(['agentName', 'agentKey'])
    wizardStep.value = 3
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateAgent(buildPayload())
      ElMessage.success('智能体更新成功')
    } else {
      await addAgent(buildPayload())
      ElMessage.success('智能体创建成功')
    }
    dialogVisible.value = false
    await loadAgentList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  if (String(row.id).startsWith('demo-')) {
    ElMessage.info('示例智能体仅用于展示')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除智能体「${row.agentName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteAgent(row.id)
  ElMessage.success('智能体删除成功')
  await loadAgentList()
}

const handleChat = (row) => {
  if (String(row.id).startsWith('demo-')) {
    ElMessage.info('示例智能体仅用于展示，请先创建真实智能体')
    return
  }

  router.push({
    name: 'AgentChat',
    params: { agentId: row.id },
    query: {
      agentName: row.agentName || undefined,
      agentKey: row.agentKey || undefined
    }
  })
}

onMounted(async () => {
  await loadAgentList()
  if (route.query.create === '1') {
    await handleAdd()
  }
})
</script>

<template>
  <section class="agent-center-page">
    <div class="page-hero agent-hero">
      <div>
        <span class="eyebrow">AGENT WORKSPACE</span>
        <h2>智能体中心</h2>
        <p>集中创建、配置和发布企业智能体，统一管理版本与运行状态。</p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Download">导出数据</el-button>
        <el-button type="primary" :icon="Lightning" @click="handleAdd">新建</el-button>
      </div>
    </div>

    <div class="agent-stats">
      <article v-for="item in stats" :key="item.label" class="agent-stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small><i />{{ item.note }}</small>
      </article>
    </div>

    <article class="agent-list-panel">
      <div class="agent-list-header">
        <div>
          <h3>智能体列表</h3>
          <p>展示平台当前配置与实时运行状态</p>
        </div>
        <el-segmented v-model="activeFilter" :options="['全部', '运行中', '需关注']" />
      </div>

      <el-table
        v-loading="loading"
        :data="filteredRows"
        class="agent-table"
      >
        <el-table-column prop="agentName" label="智能体" min-width="180" />
        <el-table-column prop="agentType" label="类型" min-width="140" />
        <el-table-column label="当前版本" min-width="140">
          <template #default="{ row }">
            {{ row.currentVersionId || 'v1.0' }}
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="170">
          <template #default="{ row }">
            {{ row.modelName || row.model || 'Qwen3-235B' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <span class="status-badge" :class="statusMeta(row.status).className">
              <i />{{ statusMeta(row.status).text }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleChat(row)">
              对话
            </el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <div class="agent-bottom-grid">
      <article class="insight-card">
        <span class="insight-icon">
          <el-icon><MagicStick /></el-icon>
        </span>
        <div>
          <h3>智能建议</h3>
          <p>根据最近 7 天运行数据，建议优先检查报告生成助手的模型超时配置。</p>
        </div>
        <el-button link type="primary">查看建议</el-button>
      </article>
      <article class="insight-card">
        <span class="insight-icon cyan">
          <el-icon><Key /></el-icon>
        </span>
        <div>
          <h3>安全与审计</h3>
          <p>所有敏感工具调用均经过租户、角色与运行时三层权限校验。</p>
        </div>
        <el-button link type="primary">打开审计</el-button>
      </article>
    </div>

    <el-dialog
      v-model="dialogVisible"
      class="agent-dialog"
      width="780px"
      destroy-on-close
      :show-close="false"
    >
      <template #header>
        <div class="dialog-title">
          <div>
            <span>NEW AGENT</span>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button type="button" class="dialog-close" @click="dialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>

      <div class="wizard-steps">
        <div
          v-for="step in 3"
          :key="step"
          :class="{ active: wizardStep >= step }"
        >
          <span>{{ step }}</span>
          {{ step === 1 ? '选择类型' : step === 2 ? '基本信息' : '运行配置' }}
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div v-if="wizardStep === 1" class="agent-type-step">
          <h4>选择智能体类型</h4>
          <p>不同类型决定 Agent 的推理与工具调用方式，创建后仍可调整。</p>
          <button
            v-for="type in agentTypes"
            :key="type.value"
            type="button"
            class="agent-type-card"
            :class="{ selected: form.agentType === type.value }"
            @click="form.agentType = type.value"
          >
            <span class="type-icon">
              <el-icon><component :is="type.icon" /></el-icon>
            </span>
            <span>
              <strong>{{ type.title }}</strong>
              <small>{{ type.desc }}</small>
              <em v-if="type.tag">{{ type.tag }}</em>
            </span>
            <i class="type-check">
              <el-icon><Check /></el-icon>
            </i>
          </button>
        </div>

        <div v-else-if="wizardStep === 2" class="agent-form-grid">
          <el-form-item label="智能体名称" prop="agentName">
            <el-input v-model="form.agentName" placeholder="如 数据分析助手" />
          </el-form-item>
          <el-form-item label="智能体英文名称" prop="agentKey">
            <el-input v-model="form.agentKey" placeholder="如 data-analyst" />
          </el-form-item>
          <el-form-item class="full" label="描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="说明智能体面向的业务场景"
            />
          </el-form-item>
        </div>

        <div v-else class="agent-form-grid">
          <el-form-item label="当前版本ID">
            <el-input v-model="form.currentVersionId" placeholder="可为空，如 v1.0" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">停用</el-radio>
              <el-radio :value="2">草稿</el-radio>
            </el-radio-group>
          </el-form-item>
          <div class="runtime-preview full">
            <span>
              <el-icon><Monitor /></el-icon>
            </span>
            <div>
              <strong>默认运行配置</strong>
              <p>创建后可在运行中心继续配置模型、权限、工作区和工具链。</p>
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button v-if="wizardStep === 1" @click="dialogVisible = false">取消</el-button>
        <el-button v-else @click="wizardStep -= 1">上一步</el-button>
        <el-button
          v-if="wizardStep < 3"
          type="primary"
          :icon="ArrowRight"
          @click="nextStep"
        >
          下一步
        </el-button>
        <el-button v-else type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
