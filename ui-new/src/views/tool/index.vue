<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModuleShell from '@/components/common/ModuleShell.vue'
import ResourceCard from '@/components/common/ResourceCard.vue'
import CreateCard from '@/components/common/CreateCard.vue'
import {
  createTool,
  createToolGroup,
  deleteTool,
  listToolGroups,
  listTools,
  updateTool,
  updateToolGroup
} from '@/api/tool'
import { asArray, formatTime } from '@/utils/collections'
import { withAuditFields } from '@/utils/payload'

const loading = ref(false)
const drawerVisible = ref(false)
const groupDrawerVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref()
const groupFormRef = ref()
const keyword = ref('')
const selectedGroup = ref('全部')
const tools = ref([])
const groups = ref([])
const current = ref({})
const toolApiUnavailable = ref(false)

const form = reactive({
  id: '',
  toolName: '',
  groupId: '',
  permissionCode: '',
  toolNameExplain: '',
  description: '',
  toolKey: '',
  signatureHash: '',
  toolType: 'JAVA_BEAN',
  beanName: '',
  className: '',
  methodName: '',
  inputSchema: '',
  outputSchema: '',
  readOnly: true,
  concurrency: false,
  stateInjected: false,
  riskLevel: 'LOW',
  enabled: true,
  timeoutMs: 30000,
  maxRetries: 1,
  defaultGroupCode: ''
})

const groupForm = reactive({
  id: '',
  groupName: '',
  description: '',
  enabled: true,
  activeByDefault: false
})

const rules = {
  toolName: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
  toolType: [{ required: true, message: '请选择工具类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入工具描述', trigger: 'blur' }]
}

const groupRules = {
  groupName: [{ required: true, message: '请输入工具组名称', trigger: 'blur' }]
}

const groupNameById = computed(() => {
  return groups.value.reduce((map, item) => {
    map[String(item.id)] = item.groupName
    return map
  }, {})
})

const groupOptions = computed(() => [
  { label: '全部', value: '全部' },
  ...groups.value.map((item) => ({ label: item.groupName, value: String(item.id) }))
])

const filteredTools = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return tools.value.filter((item) => {
    const groupMatched = selectedGroup.value === '全部' ||
      String(item.groupId || item.defaultGroupCode || '') === selectedGroup.value
    const wordMatched = !word ||
      item.toolName?.toLowerCase().includes(word) ||
      item.description?.toLowerCase().includes(word) ||
      item.toolKey?.toLowerCase().includes(word) ||
      item.className?.toLowerCase().includes(word)
    return groupMatched && wordMatched
  })
})

const displayGroup = (item) => {
  return groupNameById.value[String(item.groupId || '')] || item.defaultGroupCode || '默认工具组'
}

const resetForm = () => {
  Object.assign(form, {
    id: '',
    toolName: '',
    groupId: '',
    permissionCode: '',
    toolNameExplain: '',
    description: '',
    toolKey: '',
    signatureHash: '',
    toolType: 'JAVA_BEAN',
    beanName: '',
    className: '',
    methodName: '',
    inputSchema: '',
    outputSchema: '',
    readOnly: true,
    concurrency: false,
    stateInjected: false,
    riskLevel: 'LOW',
    enabled: true,
    timeoutMs: 30000,
    maxRetries: 1,
    defaultGroupCode: ''
  })
  formRef.value?.clearValidate?.()
}

const resetGroupForm = () => {
  Object.assign(groupForm, {
    id: '',
    groupName: '',
    description: '',
    enabled: true,
    activeByDefault: false
  })
  groupFormRef.value?.clearValidate?.()
}

const loadData = async () => {
  loading.value = true
  toolApiUnavailable.value = false
  try {
    const [toolRes, groupRes] = await Promise.all([
      listTools({ silentError: true }).catch(() => {
        toolApiUnavailable.value = true
        return []
      }),
      listToolGroups({ silentError: true }).catch(() => {
        toolApiUnavailable.value = true
        return []
      })
    ])
    tools.value = asArray(toolRes)
    groups.value = asArray(groupRes)
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
    toolName: item.toolName || '',
    groupId: item.groupId || '',
    permissionCode: item.permissionCode || '',
    toolNameExplain: item.toolNameExplain || '',
    description: item.description || '',
    toolKey: item.toolKey || '',
    signatureHash: item.signatureHash || '',
    toolType: item.toolType || 'JAVA_BEAN',
    beanName: item.beanName || '',
    className: item.className || '',
    methodName: item.methodName || '',
    inputSchema: item.inputSchema || '',
    outputSchema: item.outputSchema || '',
    readOnly: Boolean(item.readOnly ?? true),
    concurrency: Boolean(item.concurrency ?? false),
    stateInjected: Boolean(item.stateInjected ?? false),
    riskLevel: item.riskLevel || 'LOW',
    enabled: Boolean(item.enabled ?? true),
    timeoutMs: Number(item.timeoutMs ?? 30000),
    maxRetries: Number(item.maxRetries ?? 1),
    defaultGroupCode: item.defaultGroupCode || ''
  })
  drawerVisible.value = true
}

const openGroupCreate = () => {
  resetGroupForm()
  groupDrawerVisible.value = true
}

const openDetail = (item) => {
  current.value = item
  detailVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  const payload = withAuditFields({
    id: form.id || undefined,
    toolName: form.toolName,
    groupId: form.groupId || null,
    permissionCode: form.permissionCode,
    toolNameExplain: form.toolNameExplain,
    description: form.description,
    toolKey: form.toolKey,
    signatureHash: form.signatureHash,
    toolType: form.toolType,
    beanName: form.beanName,
    className: form.className,
    methodName: form.methodName,
    inputSchema: form.inputSchema,
    outputSchema: form.outputSchema,
    readOnly: form.readOnly,
    concurrency: form.concurrency,
    stateInjected: form.stateInjected,
    riskLevel: form.riskLevel,
    enabled: form.enabled,
    timeoutMs: form.timeoutMs,
    maxRetries: form.maxRetries,
    defaultGroupCode: form.defaultGroupCode
  }, form.id ? 'update' : 'create')

  if (form.id) {
    await updateTool(payload)
    ElMessage.success('工具已更新')
  } else {
    await createTool(payload)
    ElMessage.success('工具已创建')
  }
  drawerVisible.value = false
  await loadData()
}

const submitGroupForm = async () => {
  await groupFormRef.value.validate()
  const payload = withAuditFields({
    id: groupForm.id || undefined,
    groupName: groupForm.groupName,
    description: groupForm.description,
    enabled: groupForm.enabled,
    activeByDefault: groupForm.activeByDefault
  }, groupForm.id ? 'update' : 'create')

  if (groupForm.id) {
    await updateToolGroup(payload)
  } else {
    await createToolGroup(payload)
  }
  ElMessage.success('工具组已保存')
  groupDrawerVisible.value = false
  await loadData()
}

const removeTool = async (item) => {
  await ElMessageBox.confirm('删除工具后，智能体配置中保存的工具引用需要重新整理。确认继续？', '确认删除', {
    type: 'warning'
  })
  await deleteTool(item.id)
  ElMessage.success('工具已删除')
  await loadData()
}

const handleAction = (action, item) => {
  if (action === 'view') openDetail(item)
  if (action === 'edit') openEdit(item)
  if (action === 'delete') removeTool(item)
}

onMounted(loadData)
</script>

<template>
  <ModuleShell
    title="工具"
    description="工具模块是智能体的执行层能力池。这里维护全局工具定义，智能体配置发布时会从启用工具中选择并写入编排快照。"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
      <el-button @click="openGroupCreate">新建工具组</el-button>
      <el-button type="primary" @click="openCreate">新建工具</el-button>
    </template>

    <template #filters>
      <div class="filter-left">
        <el-segmented v-model="selectedGroup" :options="groupOptions" />
      </div>
      <div class="filter-right">
        <el-input v-model="keyword" clearable placeholder="搜索工具名称、描述或类名" style="width: 340px">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </template>

    <el-alert
      v-if="toolApiUnavailable"
      class="tool-api-alert"
      type="warning"
      show-icon
      :closable="false"
      title="当前运行中的 agent 服务还没有加载工具接口，重启后端到最新代码后这里会显示真实工具数据。"
    />

    <section v-loading="loading" class="card-grid">
      <CreateCard title="新建工具" description="注册 Java Bean、HTTP、MCP 等工具定义" @click="openCreate" />
      <ResourceCard
        v-for="item in filteredTools"
        :key="item.id"
        :title="item.toolName"
        :description="item.description || item.toolNameExplain"
        :tags="[item.toolType || 'TOOL', displayGroup(item), item.riskLevel || 'LOW']"
        :meta="formatTime(item.updatedAt || item.createdAt)"
        :disabled="!item.enabled"
        icon="Tools"
        accent="cyan"
        @action="(action) => handleAction(action, item)"
      />
    </section>

    <el-empty v-if="!loading && filteredTools.length === 0" class="empty-panel" description="暂无工具" />

    <el-drawer v-model="drawerVisible" :title="form.id ? '编辑工具' : '新建工具'" size="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="工具名称" prop="toolName">
            <el-input v-model="form.toolName" placeholder="查询订单" />
          </el-form-item>
          <el-form-item label="工具组">
            <el-select v-model="form.groupId" clearable filterable>
              <el-option v-for="group in groups" :key="group.id" :label="group.groupName" :value="String(group.id)" />
            </el-select>
          </el-form-item>
          <el-form-item label="工具类型" prop="toolType">
            <el-select v-model="form.toolType" filterable allow-create>
              <el-option label="JAVA_BEAN" value="JAVA_BEAN" />
              <el-option label="HTTP" value="HTTP" />
              <el-option label="MCP" value="MCP" />
              <el-option label="RPC" value="RPC" />
              <el-option label="SQL" value="SQL" />
            </el-select>
          </el-form-item>
          <el-form-item label="风险等级">
            <el-select v-model="form.riskLevel">
              <el-option label="LOW" value="LOW" />
              <el-option label="MEDIUM" value="MEDIUM" />
              <el-option label="HIGH" value="HIGH" />
            </el-select>
          </el-form-item>
          <el-form-item label="权限码">
            <el-input v-model="form.permissionCode" placeholder="order:query" />
          </el-form-item>
          <el-form-item label="默认组编码">
            <el-input v-model="form.defaultGroupCode" placeholder="order" />
          </el-form-item>
          <el-form-item class="full-row" label="工具说明" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="给模型看的能力描述" />
          </el-form-item>
          <el-form-item label="Bean 名称">
            <el-input v-model="form.beanName" placeholder="orderTools" />
          </el-form-item>
          <el-form-item label="类名">
            <el-input v-model="form.className" placeholder="com.zw.agent.tools.OrderTools" />
          </el-form-item>
          <el-form-item label="方法名">
            <el-input v-model="form.methodName" placeholder="queryOrder" />
          </el-form-item>
          <el-form-item label="工具 Key">
            <el-input v-model="form.toolKey" placeholder="类名#方法名" />
          </el-form-item>
          <el-form-item label="超时毫秒">
            <el-input-number v-model="form.timeoutMs" :min="1000" :step="1000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大重试">
            <el-input-number v-model="form.maxRetries" :min="0" :max="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.enabled" />
          </el-form-item>
          <el-form-item label="只读工具">
            <el-switch v-model="form.readOnly" />
          </el-form-item>
          <el-form-item label="并发调用">
            <el-switch v-model="form.concurrency" />
          </el-form-item>
          <el-form-item label="注入 AgentState">
            <el-switch v-model="form.stateInjected" />
          </el-form-item>
          <el-form-item class="full-row" label="入参 JSON Schema">
            <el-input v-model="form.inputSchema" type="textarea" :rows="5" placeholder='{"type":"object","properties":{}}' />
          </el-form-item>
          <el-form-item class="full-row" label="出参 Schema">
            <el-input v-model="form.outputSchema" type="textarea" :rows="4" />
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

    <el-drawer v-model="groupDrawerVisible" title="新建工具组" size="460px">
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-position="top">
        <el-form-item label="工具组名称" prop="groupName">
          <el-input v-model="groupForm.groupName" placeholder="订单工具" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="groupForm.enabled" />
        </el-form-item>
        <el-form-item label="默认激活">
          <el-switch v-model="groupForm.activeByDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="groupDrawerVisible = false">取消</el-button>
          <el-button type="primary" @click="submitGroupForm">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="detailVisible" title="工具详情" size="620px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ current.toolName }}</el-descriptions-item>
        <el-descriptions-item label="工具组">{{ displayGroup(current) }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ current.toolType }}</el-descriptions-item>
        <el-descriptions-item label="风险">{{ current.riskLevel }}</el-descriptions-item>
        <el-descriptions-item label="Bean">{{ current.beanName }}</el-descriptions-item>
        <el-descriptions-item label="类名">{{ current.className }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ current.methodName }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ current.description }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(current.updatedAt || current.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </ModuleShell>
</template>

<style scoped>
.tool-api-alert {
  margin-bottom: 16px;
}
</style>
