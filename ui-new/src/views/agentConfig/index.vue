<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModuleShell from '@/components/common/ModuleShell.vue'
import ResourceCard from '@/components/common/ResourceCard.vue'
import CreateCard from '@/components/common/CreateCard.vue'
import RelationGraph from '@/components/common/RelationGraph.vue'
import { listAgents } from '@/api/agent'
import { createAgentConfig, deleteAgentConfig, listAgentConfigs, updateAgentConfig } from '@/api/agentConfig'
import { listModelConfigs } from '@/api/model'
import { listTools } from '@/api/tool'
import { asArray, formatTime } from '@/utils/collections'
import { prettyJson, safeParseJson, withAuditFields } from '@/utils/payload'

const loading = ref(false)
const drawerVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref()
const current = ref({})
const keyword = ref('')
const selectedStatus = ref('全部')
const agents = ref([])
const models = ref([])
const tools = ref([])
const configs = ref([])

const selectedToolIds = ref([])
const selectedSkillIds = ref([])
const selectedMcpIds = ref([])
const selectedKnowledgeIds = ref([])

const form = reactive({
  id: '',
  agentId: '',
  versionNo: '',
  sysPrompt: '',
  modelConfigId: '',
  maxIters: 50,
  permissionMode: 'BYPASS',
  publishStatus: 0,
  compactionTriggerMessages: 20,
  compactionKeepMessages: 8,
  workspacePath: '',
  workspaceIsolation: 'TENANT_AGENT',
  visualSchemaJson: ''
})

const fakeSkills = [
  { id: 'skill-order', name: '订单处理技能包', description: '订单查询、退款说明、物流跟踪' },
  { id: 'skill-report', name: '数据分析技能包', description: '指标解释、异常定位、报表生成' },
  { id: 'skill-content', name: '内容运营技能包', description: '选题扩写、校对、渠道适配' }
]

const fakeMcps = [
  { id: 'mcp-filesystem', name: '文件系统 MCP', description: '受限目录文件读写和检索' },
  { id: 'mcp-browser', name: '浏览器自动化 MCP', description: '页面打开、点击、截图和表单填写' },
  { id: 'mcp-enterprise', name: '企业知识 MCP', description: '连接企业内部服务目录' }
]

const fakeKnowledge = [
  { id: 'kb-after-sale', name: '售后政策知识库', description: '退换货、质保、发票和物流异常' },
  { id: 'kb-product', name: '产品手册知识库', description: '产品参数、兼容性和安装步骤' },
  { id: 'kb-policy', name: '内部制度知识库', description: '组织制度、审批流程和安全规范' }
]

const statusOptions = [
  { label: '全部', value: '全部' },
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已废弃', value: 2 }
]

const rules = {
  agentId: [{ required: true, message: '请选择智能体', trigger: 'change' }],
  modelConfigId: [{ required: true, message: '请选择模型配置', trigger: 'change' }],
  versionNo: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  sysPrompt: [{ required: true, message: '请输入智能体系统提示词', trigger: 'blur' }]
}

const agentNameById = computed(() => {
  return agents.value.reduce((map, item) => {
    map[String(item.id)] = item.agentName
    return map
  }, {})
})

const modelById = computed(() => {
  return models.value.reduce((map, item) => {
    map[String(item.id)] = item
    return map
  }, {})
})

const toolById = computed(() => {
  return tools.value.reduce((map, item) => {
    map[String(item.id)] = item
    return map
  }, {})
})

const candidateModels = computed(() => {
  if (!form.agentId) {
    return models.value
  }
  return models.value.filter((item) => !item.agentId || String(item.agentId) === String(form.agentId))
})

const selectedModel = computed(() => modelById.value[String(form.modelConfigId)] || {})
const selectedAgentName = computed(() => agentNameById.value[String(form.agentId)] || '未选择智能体')

const filteredConfigs = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return configs.value.filter((item) => {
    const statusMatched = selectedStatus.value === '全部' || Number(item.publishStatus) === Number(selectedStatus.value)
    const agentName = agentNameById.value[String(item.agentId)] || ''
    const model = modelById.value[String(item.modelConfigId)] || {}
    const wordMatched = !word ||
      item.versionNo?.toLowerCase().includes(word) ||
      item.sysPrompt?.toLowerCase().includes(word) ||
      agentName.toLowerCase().includes(word) ||
      model.modelName?.toLowerCase().includes(word)
    return statusMatched && wordMatched
  })
})

const statusLabel = (status) => {
  const value = Number(status)
  if (value === 1) return '已发布'
  if (value === 2) return '已废弃'
  return '草稿'
}

const parseVisual = (config) => safeParseJson(config?.visualSchemaJson, {})

const relationIds = (config) => {
  const schema = parseVisual(config)
  const relations = schema.relations || {}
  return {
    toolIds: relations.toolIds || [],
    skillIds: relations.skillIds || [],
    mcpIds: relations.mcpIds || [],
    knowledgeBaseIds: relations.knowledgeBaseIds || []
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: '',
    agentId: '',
    versionNo: `v${configs.value.length + 1}`,
    sysPrompt: '',
    modelConfigId: '',
    maxIters: 50,
    permissionMode: 'BYPASS',
    publishStatus: 0,
    compactionTriggerMessages: 20,
    compactionKeepMessages: 8,
    workspacePath: '',
    workspaceIsolation: 'TENANT_AGENT',
    visualSchemaJson: ''
  })
  selectedToolIds.value = []
  selectedSkillIds.value = []
  selectedMcpIds.value = []
  selectedKnowledgeIds.value = []
  formRef.value?.clearValidate?.()
}

const loadData = async () => {
  loading.value = true
  try {
    const [agentRes, modelRes, toolRes, configRes] = await Promise.all([
      listAgents(),
      listModelConfigs(),
      listTools({ silentError: true }).catch(() => []),
      listAgentConfigs()
    ])
    agents.value = asArray(agentRes)
    models.value = asArray(modelRes)
    tools.value = asArray(toolRes)
    configs.value = asArray(configRes)
  } finally {
    loading.value = false
  }
}

const applyJsonConfig = (item) => {
  const compaction = safeParseJson(item.compactionConfigJson, {})
  const workspace = safeParseJson(item.workspaceConfigJson, {})
  const relations = relationIds(item)

  selectedToolIds.value = relations.toolIds.map(String)
  selectedSkillIds.value = relations.skillIds.map(String)
  selectedMcpIds.value = relations.mcpIds.map(String)
  selectedKnowledgeIds.value = relations.knowledgeBaseIds.map(String)

  form.compactionTriggerMessages = Number(compaction.triggerMessages ?? 20)
  form.compactionKeepMessages = Number(compaction.keepMessages ?? 8)
  form.workspacePath = workspace.workspacePath || workspace.path || ''
  form.workspaceIsolation = workspace.isolation || 'TENANT_AGENT'
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
    versionNo: item.versionNo || '',
    sysPrompt: item.sysPrompt || '',
    modelConfigId: item.modelConfigId || '',
    maxIters: Number(item.maxIters ?? 50),
    permissionMode: item.permissionMode || 'BYPASS',
    publishStatus: Number(item.publishStatus ?? 0),
    visualSchemaJson: item.visualSchemaJson || ''
  })
  applyJsonConfig(item)
  drawerVisible.value = true
}

const openDetail = (item) => {
  current.value = item
  detailVisible.value = true
}

const buildVisualSchema = () => {
  const model = selectedModel.value
  const toolNodes = selectedToolIds.value.map((id) => ({
    id: `tool-${id}`,
    type: 'tool',
    label: toolById.value[String(id)]?.toolName || `Tool ${id}`
  }))
  const skillNodes = selectedSkillIds.value.map((id) => ({
    id: `skill-${id}`,
    type: 'skill',
    label: fakeSkills.find((item) => item.id === id)?.name || id
  }))
  const mcpNodes = selectedMcpIds.value.map((id) => ({
    id: `mcp-${id}`,
    type: 'mcp',
    label: fakeMcps.find((item) => item.id === id)?.name || id
  }))
  const knowledgeNodes = selectedKnowledgeIds.value.map((id) => ({
    id: `knowledge-${id}`,
    type: 'knowledge',
    label: fakeKnowledge.find((item) => item.id === id)?.name || id
  }))
  const capabilityNodes = [...toolNodes, ...skillNodes, ...mcpNodes, ...knowledgeNodes]

  return {
    version: 'ui-new-1',
    flow: 'agent-orchestration',
    generatedAt: new Date().toISOString(),
    nodes: [
      { id: `agent-${form.agentId}`, type: 'agent', label: selectedAgentName.value },
      { id: `model-${form.modelConfigId}`, type: 'model', label: `${model.provider || ''} ${model.modelName || ''}`.trim() },
      { id: `config-${form.versionNo}`, type: 'agentConfig', label: form.versionNo },
      ...capabilityNodes
    ],
    edges: [
      { source: `agent-${form.agentId}`, target: `config-${form.versionNo}`, label: 'publishes' },
      { source: `config-${form.versionNo}`, target: `model-${form.modelConfigId}`, label: 'uses' },
      ...capabilityNodes.map((node) => ({ source: `config-${form.versionNo}`, target: node.id, label: 'enables' }))
    ],
    relations: {
      agentId: form.agentId,
      modelConfigId: form.modelConfigId,
      toolIds: selectedToolIds.value,
      skillIds: selectedSkillIds.value,
      mcpIds: selectedMcpIds.value,
      knowledgeBaseIds: selectedKnowledgeIds.value
    },
    runtime: {
      permissionMode: form.permissionMode,
      maxIters: form.maxIters
    }
  }
}

const buildPayload = () => {
  const visualSchema = buildVisualSchema()
  return withAuditFields({
    id: form.id || undefined,
    agentId: form.agentId,
    versionNo: form.versionNo,
    sysPrompt: form.sysPrompt,
    modelConfigId: form.modelConfigId,
    maxIters: form.maxIters,
    compactionConfigJson: JSON.stringify({
      triggerMessages: form.compactionTriggerMessages,
      keepMessages: form.compactionKeepMessages
    }),
    workspaceConfigJson: JSON.stringify({
      workspacePath: form.workspacePath,
      isolation: form.workspaceIsolation
    }),
    permissionMode: form.permissionMode,
    visualSchemaJson: JSON.stringify(visualSchema),
    publishStatus: form.publishStatus,
    publishedAt: Number(form.publishStatus) === 1 ? new Date().toISOString().slice(0, 19).replace('T', ' ') : null
  }, form.id ? 'update' : 'create')
}

const submitForm = async () => {
  await formRef.value.validate()
  const payload = buildPayload()
  if (form.id) {
    await updateAgentConfig(payload)
    ElMessage.success('智能体配置已更新')
  } else {
    await createAgentConfig(payload)
    ElMessage.success('智能体配置已创建')
  }
  drawerVisible.value = false
  await loadData()
}

const removeConfig = async (item) => {
  await ElMessageBox.confirm('删除后该版本编排快照无法恢复，确认继续？', '确认删除', {
    type: 'warning'
  })
  await deleteAgentConfig(item.id)
  ElMessage.success('配置已删除')
  await loadData()
}

const handleAction = (action, item) => {
  if (action === 'view') openDetail(item)
  if (action === 'edit') openEdit(item)
  if (action === 'delete') removeConfig(item)
}

const configModelName = (item) => {
  const model = modelById.value[String(item.modelConfigId)] || {}
  return [model.provider, model.modelName].filter(Boolean).join(' / ') || '未绑定模型'
}

const configToolCount = (item) => relationIds(item).toolIds.length

watch(
  () => form.agentId,
  () => {
    if (form.modelConfigId && !candidateModels.value.some((item) => String(item.id) === String(form.modelConfigId))) {
      form.modelConfigId = ''
    }
    if (form.agentId && !form.workspacePath) {
      form.workspacePath = `/tmp/agentscope/workspaces/tenant-1/agent-${form.agentId}`
    }
  }
)

onMounted(loadData)
</script>

<template>
  <ModuleShell
    title="智能体配置"
    description="智能体配置替代系统提示词模块，负责把智能体、模型供应商、工具以及未来的技能包/MCP/知识库编排成一个可发布版本。"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
      <el-button type="primary" @click="openCreate">新建配置</el-button>
    </template>

    <template #filters>
      <div class="filter-left">
        <el-segmented v-model="selectedStatus" :options="statusOptions" />
      </div>
      <div class="filter-right">
        <el-input v-model="keyword" clearable placeholder="搜索智能体、模型或提示词" style="width: 340px">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </template>

    <section v-loading="loading" class="card-grid">
      <CreateCard title="新建智能体配置" description="选择智能体、模型和工具，发布一个可运行编排版本" @click="openCreate" />
      <ResourceCard
        v-for="item in filteredConfigs"
        :key="item.id"
        :title="`${agentNameById[String(item.agentId)] || '智能体'} / ${item.versionNo}`"
        :description="item.sysPrompt"
        :tags="[statusLabel(item.publishStatus), configModelName(item), `${configToolCount(item)} 个工具`]"
        :meta="formatTime(item.updatedAt || item.createdAt)"
        :disabled="Number(item.publishStatus) === 2"
        icon="SetUp"
        accent="amber"
        @action="(action) => handleAction(action, item)"
      />
    </section>

    <el-empty v-if="!loading && filteredConfigs.length === 0" class="empty-panel" description="暂无智能体配置" />

    <el-drawer v-model="drawerVisible" :title="form.id ? '编辑智能体配置' : '新建智能体配置'" size="86%">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-steps class="config-steps" :active="3" finish-status="success" simple>
          <el-step title="选择智能体" />
          <el-step title="绑定模型" />
          <el-step title="编排能力" />
          <el-step title="发布配置" />
        </el-steps>

        <div class="config-editor">
          <section class="config-panel">
            <h3>基础关联</h3>
            <div class="form-grid">
              <el-form-item label="智能体" prop="agentId">
                <el-select v-model="form.agentId" filterable placeholder="选择智能体">
                  <el-option v-for="agent in agents" :key="agent.id" :label="agent.agentName" :value="agent.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="版本号" prop="versionNo">
                <el-input v-model="form.versionNo" placeholder="v1 / 2026.06.28-1" />
              </el-form-item>
              <el-form-item label="模型配置" prop="modelConfigId">
                <el-select v-model="form.modelConfigId" filterable placeholder="选择模型配置">
                  <el-option
                    v-for="model in candidateModels"
                    :key="model.id"
                    :label="`${model.provider} / ${model.modelName}`"
                    :value="model.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="发布状态">
                <el-select v-model="form.publishStatus">
                  <el-option label="草稿" :value="0" />
                  <el-option label="已发布" :value="1" />
                  <el-option label="已废弃" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item class="full-row" label="智能体系统提示词" prop="sysPrompt">
                <el-input v-model="form.sysPrompt" type="textarea" :rows="7" placeholder="定义智能体角色、任务边界、工具使用策略和输出规范" />
              </el-form-item>
            </div>
          </section>

          <section class="config-panel">
            <h3>运行参数</h3>
            <div class="form-grid">
              <el-form-item label="最大循环次数">
                <el-input-number v-model="form.maxIters" :min="1" :max="1000" style="width: 100%" />
              </el-form-item>
              <el-form-item label="权限模式">
                <el-select v-model="form.permissionMode">
                  <el-option label="BYPASS" value="BYPASS" />
                  <el-option label="ASK" value="ASK" />
                  <el-option label="ALLOW" value="ALLOW" />
                  <el-option label="DENY" value="DENY" />
                </el-select>
              </el-form-item>
              <el-form-item label="压缩触发消息数">
                <el-input-number v-model="form.compactionTriggerMessages" :min="1" style="width: 100%" />
              </el-form-item>
              <el-form-item label="压缩保留消息数">
                <el-input-number v-model="form.compactionKeepMessages" :min="1" style="width: 100%" />
              </el-form-item>
              <el-form-item class="full-row" label="工作区路径">
                <el-input v-model="form.workspacePath" placeholder="/tmp/agentscope/workspaces/tenant-1/agent-1" />
              </el-form-item>
            </div>
          </section>

          <section class="config-panel full">
            <h3>能力编排</h3>
            <el-tabs>
              <el-tab-pane label="工具">
                <el-checkbox-group v-model="selectedToolIds" class="choice-grid">
                  <label v-for="tool in tools" :key="tool.id" class="choice-item">
                    <el-checkbox :value="String(tool.id)">
                      <strong>{{ tool.toolName }}</strong>
                      <span>{{ tool.description || tool.toolNameExplain || '暂无描述' }}</span>
                    </el-checkbox>
                  </label>
                </el-checkbox-group>
              </el-tab-pane>
              <el-tab-pane label="技能包">
                <el-checkbox-group v-model="selectedSkillIds" class="choice-grid">
                  <label v-for="item in fakeSkills" :key="item.id" class="choice-item">
                    <el-checkbox :value="item.id">
                      <strong>{{ item.name }}</strong>
                      <span>{{ item.description }}</span>
                    </el-checkbox>
                  </label>
                </el-checkbox-group>
              </el-tab-pane>
              <el-tab-pane label="MCP">
                <el-checkbox-group v-model="selectedMcpIds" class="choice-grid">
                  <label v-for="item in fakeMcps" :key="item.id" class="choice-item">
                    <el-checkbox :value="item.id">
                      <strong>{{ item.name }}</strong>
                      <span>{{ item.description }}</span>
                    </el-checkbox>
                  </label>
                </el-checkbox-group>
              </el-tab-pane>
              <el-tab-pane label="知识库">
                <el-checkbox-group v-model="selectedKnowledgeIds" class="choice-grid">
                  <label v-for="item in fakeKnowledge" :key="item.id" class="choice-item">
                    <el-checkbox :value="item.id">
                      <strong>{{ item.name }}</strong>
                      <span>{{ item.description }}</span>
                    </el-checkbox>
                  </label>
                </el-checkbox-group>
              </el-tab-pane>
            </el-tabs>
          </section>

          <aside class="config-preview">
            <h3>编排预览</h3>
            <RelationGraph
              :agent-name="selectedAgentName"
              :model-name="selectedModel.modelName || '未选择模型'"
              :config-name="form.versionNo || '未命名版本'"
              :tool-count="selectedToolIds.length"
            />
            <pre>{{ prettyJson(buildVisualSchema()) }}</pre>
          </aside>
        </div>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">保存配置</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="detailVisible" title="智能体配置详情" size="760px">
      <RelationGraph
        :agent-name="agentNameById[String(current.agentId)] || '智能体'"
        :model-name="configModelName(current)"
        :config-name="current.versionNo"
        :tool-count="configToolCount(current)"
      />
      <el-descriptions class="detail-block" :column="1" border>
        <el-descriptions-item label="ID">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="智能体">{{ agentNameById[String(current.agentId)] }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ configModelName(current) }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ current.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(current.publishStatus) }}</el-descriptions-item>
        <el-descriptions-item label="最大循环">{{ current.maxIters }}</el-descriptions-item>
        <el-descriptions-item label="权限模式">{{ current.permissionMode }}</el-descriptions-item>
        <el-descriptions-item label="系统提示词">{{ current.sysPrompt }}</el-descriptions-item>
      </el-descriptions>
      <h3 class="json-title">visual_schema_json</h3>
      <pre class="json-preview">{{ prettyJson(parseVisual(current)) }}</pre>
    </el-drawer>
  </ModuleShell>
</template>

<style scoped>
.config-steps {
  margin-bottom: 18px;
}

.config-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 440px);
  gap: 16px;
}

.config-panel,
.config-preview {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: #fff;
}

.config-panel.full {
  grid-column: 1;
}

.config-panel h3,
.config-preview h3 {
  margin-bottom: 14px;
  font-size: 16px;
}

.config-preview {
  position: sticky;
  top: 10px;
  grid-column: 2;
  grid-row: 1 / span 3;
  align-self: start;
}

.config-preview pre,
.json-preview {
  max-height: 420px;
  margin-top: 16px;
  padding: 12px;
  overflow: auto;
  border-radius: var(--radius-md);
  background: #f6f8fb;
  color: #344054;
  font-size: 12px;
  white-space: pre-wrap;
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}

.choice-item {
  display: block;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: #fbfcfe;
}

.choice-item strong {
  display: block;
  color: #111827;
}

.choice-item span {
  display: block;
  margin-top: 4px;
  color: var(--color-muted);
  font-size: 12px;
  white-space: normal;
}

.detail-block,
.json-title {
  margin-top: 20px;
}

@media (max-width: 1080px) {
  .config-editor {
    grid-template-columns: 1fr;
  }

  .config-preview {
    position: static;
    grid-column: auto;
    grid-row: auto;
  }
}
</style>
