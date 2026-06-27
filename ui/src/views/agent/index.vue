<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatLineRound, Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { addAgent, deleteAgent, getAgent, listAgent, updateAgent } from '@/axios/agent'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增智能体')
const formRef = ref()
const page = ref(1)
const pageSize = ref(10)
const agentRows = ref([])

const queryParams = reactive({
  keyword: '',
  status: ''
})

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
  agentKey: [
    { required: true, message: '请输入智能体编码', trigger: 'blur' }
  ],
  agentName: [
    { required: true, message: '请输入智能体名称', trigger: 'blur' }
  ],
  agentType: [
    { required: true, message: '请选择智能体类型', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const filteredRows = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  return agentRows.value.filter((row) => {
    const matchKeyword = !keyword || [row.agentKey, row.agentName, row.description, row.agentType]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchStatus = queryParams.status === '' || Number(row.status) === Number(queryParams.status)
    return matchKeyword && matchStatus
  })
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const statusText = (status) => {
  const statusMap = {
    0: '停用',
    1: '启用',
    2: '草稿'
  }
  return statusMap[Number(status)] || '-'
}

const statusTagType = (status) => {
  const value = Number(status)
  if (value === 1) {
    return 'success'
  }
  if (value === 2) {
    return 'warning'
  }
  return 'info'
}

const formatTime = (row) => {
  return row.updateTime || row.updatedAt || row.createTime || row.createdAt || '-'
}

const normalizeId = (value) => {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
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

const handleQuery = () => {
  page.value = 1
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.status = ''
  page.value = 1
}

const handleAdd = async () => {
  dialogTitle.value = '新增智能体'
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑智能体'
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

const buildPayload = () => {
  return {
    id: normalizeId(form.id),
    agentKey: form.agentKey,
    agentName: form.agentName,
    description: form.description,
    agentType: form.agentType,
    currentVersionId: normalizeId(form.currentVersionId),
    status: form.status
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
      ElMessage.success('智能体新增成功')
    }

    dialogVisible.value = false
    await loadAgentList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
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
  router.push({
    name: 'AgentChat',
    params: { agentId: row.id },
    query: {
      agentName: row.agentName || undefined,
      agentKey: row.agentKey || undefined
    }
  })
}

onMounted(() => {
  loadAgentList()
})
</script>

<template>
  <section class="resource-page">
    <div class="query-bar">
      <el-form class="query-form" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            clearable
            placeholder="请输入智能体名称、编码或类型"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryParams.status"
            clearable
            placeholder="全部"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
            <el-option label="草稿" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-panel">
      <div class="table-toolbar">
        <div class="table-title">
          <h2>智能体管理列表</h2>
          <span>共 {{ filteredRows.length }} 条</span>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
          <el-button :icon="Refresh" @click="loadAgentList">刷新</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedRows"
        stripe
        class="data-table"
      >
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="agentName" label="智能体名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="agentKey" label="智能体编码" min-width="170" show-overflow-tooltip />
        <el-table-column prop="agentType" label="类型" width="120" />
        <el-table-column label="当前版本ID" width="120">
          <template #default="{ row }">
            {{ row.currentVersionId ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :icon="ChatLineRound" @click="handleChat(row)">对话</el-button>
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="filteredRows.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="620px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="116px"
      >
        <el-form-item label="智能体名称" prop="agentName">
          <el-input v-model="form.agentName" placeholder="请输入智能体名称" />
        </el-form-item>
        <el-form-item label="智能体编码" prop="agentKey">
          <el-input v-model="form.agentKey" placeholder="如 customer-service-agent" />
        </el-form-item>
        <el-form-item label="智能体类型" prop="agentType">
          <el-select v-model="form.agentType" placeholder="请选择智能体类型">
            <el-option label="HARNESS" value="HARNESS" />
            <el-option label="REACT" value="REACT" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前版本ID">
          <el-input
            v-model="form.currentVersionId"
            class="agent-number"
            placeholder="可为空"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
            <el-radio :value="2">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入智能体描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.agent-number {
  width: 100%;
}
</style>
