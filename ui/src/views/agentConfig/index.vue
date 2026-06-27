<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  addAgentConfig,
  deleteAgentConfig,
  getAgentConfig,
  listAgentConfig,
  updateAgentConfig
} from '@/axios/agentConfig'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增智能体配置')
const formRef = ref()
const page = ref(1)
const pageSize = ref(10)
const configRows = ref([])

const queryParams = reactive({
  keyword: '',
  publishStatus: ''
})

const form = reactive({
  id: null,
  agentId: null,
  versionNo: '',
  sysPrompt: '',
  modelConfigId: null,
  maxIters: 10,
  compactionConfigJson: '',
  workspaceConfigJson: '',
  permissionMode: 'ASK',
  visualSchemaJson: '',
  publishStatus: 0,
  publishedAt: ''
})

const rules = {
  agentId: [
    { required: true, message: '请输入关联智能体ID', trigger: 'blur' }
  ],
  versionNo: [
    { required: true, message: '请输入版本号', trigger: 'blur' }
  ],
  modelConfigId: [
    { required: true, message: '请输入模型配置ID', trigger: 'blur' }
  ],
  publishStatus: [
    { required: true, message: '请选择发布状态', trigger: 'change' }
  ]
}

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已废弃', value: 2 }
]

const permissionOptions = [
  { label: 'ASK', value: 'ASK' },
  { label: 'ALLOW', value: 'ALLOW' },
  { label: 'DENY', value: 'DENY' },
  { label: 'EXPLORE', value: 'EXPLORE' }
]

const filteredRows = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()

  return configRows.value.filter((row) => {
    const matchKeyword = !keyword || [
      row.id,
      row.agentId,
      row.versionNo,
      row.sysPrompt,
      row.modelConfigId,
      row.permissionMode
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchStatus = queryParams.publishStatus === ''
      || Number(row.publishStatus) === Number(queryParams.publishStatus)

    return matchKeyword && matchStatus
  })
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const statusText = (status) => {
  const option = statusOptions.find((item) => item.value === Number(status))
  return option?.label || '-'
}

const statusTagType = (status) => {
  const value = Number(status)
  if (value === 1) {
    return 'success'
  }
  if (value === 0) {
    return 'warning'
  }
  return 'info'
}

const formatTime = (row) => {
  return row.updatedAt || row.createdAt || row.publishedAt || '-'
}

const normalizeNumber = (value) => {
  return value === '' || value === undefined ? null : value
}

const normalizeId = (value) => {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    agentId: null,
    versionNo: '',
    sysPrompt: '',
    modelConfigId: null,
    maxIters: 10,
    compactionConfigJson: '',
    workspaceConfigJson: '',
    permissionMode: 'ASK',
    visualSchemaJson: '',
    publishStatus: 0,
    publishedAt: ''
  })
}

const loadConfigList = async () => {
  loading.value = true
  try {
    const data = await listAgentConfig()
    configRows.value = Array.isArray(data) ? data : []
  } catch {
    configRows.value = []
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  page.value = 1
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.publishStatus = ''
  page.value = 1
}

const handleAdd = async () => {
  dialogTitle.value = '新增智能体配置'
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑智能体配置'
  resetForm()
  const data = await getAgentConfig(row.id)
  Object.assign(form, {
    id: data?.id,
    agentId: data?.agentId ?? null,
    versionNo: data?.versionNo || '',
    sysPrompt: data?.sysPrompt || '',
    modelConfigId: data?.modelConfigId ?? null,
    maxIters: data?.maxIters ?? 10,
    compactionConfigJson: data?.compactionConfigJson || '',
    workspaceConfigJson: data?.workspaceConfigJson || '',
    permissionMode: data?.permissionMode || 'ASK',
    visualSchemaJson: data?.visualSchemaJson || '',
    publishStatus: Number(data?.publishStatus ?? 0),
    publishedAt: data?.publishedAt || ''
  })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const buildPayload = () => {
  return {
    id: normalizeId(form.id),
    agentId: normalizeId(form.agentId),
    versionNo: form.versionNo,
    sysPrompt: form.sysPrompt,
    modelConfigId: normalizeId(form.modelConfigId),
    maxIters: normalizeNumber(form.maxIters),
    compactionConfigJson: form.compactionConfigJson,
    workspaceConfigJson: form.workspaceConfigJson,
    permissionMode: form.permissionMode,
    visualSchemaJson: form.visualSchemaJson,
    publishStatus: form.publishStatus,
    publishedAt: form.publishedAt || null
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateAgentConfig(buildPayload())
      ElMessage.success('智能体配置更新成功')
    } else {
      await addAgentConfig(buildPayload())
      ElMessage.success('智能体配置新增成功')
    }

    dialogVisible.value = false
    await loadConfigList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除智能体配置「${row.versionNo || row.id}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteAgentConfig(row.id)
  ElMessage.success('智能体配置删除成功')
  await loadConfigList()
}

onMounted(() => {
  loadConfigList()
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
            placeholder="请输入版本号、智能体ID或权限模式"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select
            v-model="queryParams.publishStatus"
            clearable
            placeholder="全部"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
          <h2>智能体配置列表</h2>
          <span>共 {{ filteredRows.length }} 条</span>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
          <el-button :icon="Refresh" @click="loadConfigList">刷新</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedRows"
        stripe
        class="data-table"
      >
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="agentId" label="智能体ID" width="110" />
        <el-table-column prop="versionNo" label="版本号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="modelConfigId" label="模型配置ID" width="120" />
        <el-table-column prop="maxIters" label="最大循环" width="100" />
        <el-table-column prop="permissionMode" label="权限模式" width="110" />
        <el-table-column label="发布状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.publishStatus)">
              {{ statusText(row.publishStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sysPrompt" label="系统提示词" min-width="220" show-overflow-tooltip />
        <el-table-column label="发布时间" min-width="170">
          <template #default="{ row }">
            {{ row.publishedAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
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
      width="760px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="118px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="智能体ID" prop="agentId">
              <el-input
                v-model="form.agentId"
                class="agent-config-number"
                placeholder="关联 ai_agent_definition.id"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本号" prop="versionNo">
              <el-input v-model="form.versionNo" placeholder="如 v1、v2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型配置ID" prop="modelConfigId">
              <el-input
                v-model="form.modelConfigId"
                class="agent-config-number"
                placeholder="关联 ai_model_config.id"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大循环">
              <el-input-number
                v-model="form.maxIters"
                class="agent-config-number"
                :min="0"
                :controls="false"
                placeholder="ReAct 最大循环次数"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限模式">
              <el-select v-model="form.permissionMode" placeholder="请选择权限模式">
                <el-option
                  v-for="item in permissionOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布状态" prop="publishStatus">
              <el-select v-model="form.publishStatus" placeholder="请选择发布状态">
                <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发布时间">
              <el-date-picker
                v-model="form.publishedAt"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择发布时间"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="系统提示词">
              <el-input
                v-model="form.sysPrompt"
                type="textarea"
                :rows="4"
                placeholder="请输入 Agent 系统提示词"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="上下文压缩">
              <el-input
                v-model="form.compactionConfigJson"
                class="json-textarea"
                type="textarea"
                :rows="3"
                placeholder='如 {"triggerMessages":20,"keepMessages":8}'
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="工作区配置">
              <el-input
                v-model="form.workspaceConfigJson"
                class="json-textarea"
                type="textarea"
                :rows="3"
                placeholder='如 {"path":"/workspace","isolation":"tenant"}'
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="画布配置">
              <el-input
                v-model="form.visualSchemaJson"
                class="json-textarea"
                type="textarea"
                :rows="4"
                placeholder="请输入 Vue3 可视化画布 JSON"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.agent-config-number,
.el-select,
.el-date-editor {
  width: 100%;
}

.json-textarea :deep(.el-textarea__inner) {
  font-family: Consolas, Monaco, 'Courier New', monospace;
}
</style>
