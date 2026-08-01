<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  Clock,
  DataLine,
  Delete,
  Document,
  Edit,
  Folder,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  Setting,
  Upload,
  User
} from '@element-plus/icons-vue'
import {
  createSkill,
  createSkillPackageNode,
  deleteSkill,
  deleteSkillPackageFolder,
  deleteSkillPackageNode,
  getSkill,
  getSkillFileContent,
  getSkillMetrics,
  listRecentSkillLogs,
  listSkillFilesBySkill,
  pageSkillLogs,
  pageSkills,
  updateSkill,
  updateSkillPackageFile
} from '@/axios/skill'
import { listRole } from '@/axios/role'

const ALL_ROLES = '0'
const categories = [
  { value: 'data', label: '数据分析' },
  { value: 'report', label: '报告生成' },
  { value: 'document', label: '文档处理' },
  { value: 'code', label: '代码研发' },
  { value: 'research', label: '信息研究' },
  { value: 'file', label: '文件处理' },
  { value: 'rag', label: '知识检索' },
  { value: 'ops', label: '运维自动化' }
]
const riskOptions = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const operationOptions = [
  { value: 'LOAD_SKILL', label: '加载技能' },
  { value: 'READ_REFERENCE', label: '读取资源' },
  { value: 'RUN_SCRIPT', label: '运行脚本' }
]

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const metrics = reactive({
  total: 0,
  enabled: 0,
  todayUses: 0,
  useChangePercent: null,
  successRate: null,
  failedUses: 0,
  averageDurationMs: null,
  averageDurationChangeMs: null
})
const filters = reactive({
  keyword: '',
  category: '',
  status: '',
  riskLevel: '',
  current: 1,
  size: 8
})

const recentLogs = ref([])
const exceptionLogs = ref([])
const roles = ref([])

const formVisible = ref(false)
const formSaving = ref(false)
const formMode = ref('create')
const skillForm = reactive({
  id: null,
  skillCode: '',
  skillName: '',
  description: '',
  category: 'data',
  tags: [],
  riskLevel: 'LOW',
  status: 0,
  roleCodes: [ALL_ROLES],
  skillContent: ''
})

const editorVisible = ref(false)
const editorSaving = ref(false)
const editorSkill = ref(null)
const resourceRows = ref([])
const virtualFolders = ref([])
const activeNode = ref({ type: 'main', path: 'SKILL.md', id: 'main' })
const editorContent = ref('')
const uploadInput = ref()

const resourceDialogVisible = ref(false)
const resourceSaving = ref(false)
const resourceForm = reactive({
  id: null,
  path: '',
  content: ''
})

const folderDialogVisible = ref(false)
const folderPath = ref('')

const logDialogVisible = ref(false)
const logLoading = ref(false)
const logRows = ref([])
const logTotal = ref(0)
const logFilters = reactive({
  current: 1,
  size: 10,
  skillId: null,
  success: '',
  operation: ''
})

const roleOptions = computed(() => [
  { label: '所有角色', value: ALL_ROLES },
  ...roles.value
    .map((role) => ({
      label: role.roleName || role.roleCode,
      value: String(role.roleCode || '')
    }))
    .filter((role) => role.value && role.value !== ALL_ROLES)
])

const metricCards = computed(() => [
  {
    label: '技能总数',
    value: metrics.total,
    note: `${metrics.enabled} 个已启用`,
    icon: User,
    tone: 'blue'
  },
  {
    label: '今日使用',
    value: formatNumber(metrics.todayUses),
    note: changeText(metrics.useChangePercent, '%'),
    icon: DataLine,
    tone: 'cyan'
  },
  {
    label: '今日成功率',
    value: formatPercent(metrics.successRate),
    note: `失败 ${metrics.failedUses} 次`,
    icon: CircleCheck,
    tone: 'purple'
  },
  {
    label: '平均使用时长',
    value: formatDuration(metrics.averageDurationMs),
    note: changeText(metrics.averageDurationChangeMs, 'ms'),
    icon: Clock,
    tone: 'green'
  }
])

const resourceTree = computed(() => {
  const root = [
    {
      id: 'main',
      label: 'SKILL.md',
      path: 'SKILL.md',
      type: 'main',
      children: []
    }
  ]
  const folderMap = new Map()
  const ensureFolder = (path) => {
    if (!path) return null
    if (folderMap.has(path)) return folderMap.get(path)
    const parts = path.split('/')
    const parentPath = parts.slice(0, -1).join('/')
    const node = {
      id: `folder:${path}`,
      label: parts.at(-1),
      path,
      type: 'folder',
      children: []
    }
    folderMap.set(path, node)
    const parent = ensureFolder(parentPath)
    ;(parent ? parent.children : root).push(node)
    return node
  }

  virtualFolders.value.forEach(ensureFolder)
  resourceRows.value.forEach((resource) => {
    const path = resource.resourcePath || resource.relativePath
    const parts = String(path).split('/')
    const parent = ensureFolder(parts.slice(0, -1).join('/'))
    const node = {
      id: `resource:${resource.id}`,
      resourceId: resource.id,
      label: parts.at(-1),
      path,
      type: 'file',
      content: resource.resourceContent,
      loaded: resource.resourceContent !== undefined && resource.resourceContent !== null,
      children: []
    }
    ;(parent ? parent.children : root).push(node)
  })

  const sortNodes = (nodes) => {
    nodes.sort((left, right) => {
      if (left.type === 'main') return -1
      if (right.type === 'main') return 1
      if (left.type !== right.type) return left.type === 'folder' ? -1 : 1
      return left.label.localeCompare(right.label, 'zh-CN')
    })
    nodes.forEach((node) => sortNodes(node.children))
  }
  sortNodes(root)
  return root
})

function normalizeRoles(values) {
  const normalized = (values || []).map(String).filter(Boolean)
  if (normalized.includes(ALL_ROLES)) return [ALL_ROLES]
  return [...new Set(normalized)]
}

function handleRoleChange(values) {
  skillForm.roleCodes = normalizeRoles(values)
}

function categoryLabel(value) {
  return categories.find((item) => item.value === value)?.label || value || '未分类'
}

function riskType(value) {
  return {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger'
  }[value] || 'info'
}

function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN').format(Number(value || 0))
}

function formatPercent(value) {
  return value === null || value === undefined ? '--' : `${Number(value).toFixed(1)}%`
}

function formatDuration(value) {
  if (value === null || value === undefined) return '--'
  const duration = Number(value)
  return duration >= 1000 ? `${(duration / 1000).toFixed(1)}s` : `${Math.round(duration)}ms`
}

function changeText(value, unit) {
  if (value === null || value === undefined) return '暂无昨日同期数据'
  const number = Number(value)
  const prefix = number > 0 ? '+' : ''
  return `较昨日同期 ${prefix}${number.toFixed(1)}${unit}`
}

function operationLabel(value) {
  return operationOptions.find((item) => item.value === value)?.label || value || '-'
}

function generateCode(name) {
  const code = String(name || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return /^[a-z]/.test(code) ? code.slice(0, 64).replace(/-+$/, '') : ''
}

function resetForm() {
  Object.assign(skillForm, {
    id: null,
    skillCode: '',
    skillName: '',
    description: '',
    category: 'data',
    tags: [],
    riskLevel: 'LOW',
    status: 0,
    roleCodes: [ALL_ROLES],
    skillContent: ''
  })
}

function buildSkillPayload(source = skillForm) {
  return {
    id: source.id,
    skillKey: source.skillCode,
    source: source.skillCode,
    skillName: source.skillName,
    name: source.skillName,
    description: source.description,
    category: source.category,
    tags: source.tags || [],
    riskLevel: source.riskLevel,
    status: Number(source.status),
    roleCodes: normalizeRoles(source.roleCodes),
    skillContent: source.skillContent,
    skillMdContent: source.skillContent
  }
}

async function loadDashboard() {
  loading.value = true
  try {
    const [metricData, pageData, recentData, failedData] = await Promise.all([
      getSkillMetrics(),
      pageSkills({
        current: filters.current,
        size: filters.size,
        keyword: filters.keyword || undefined,
        category: filters.category || undefined,
        status: filters.status === '' ? undefined : filters.status,
        riskLevel: filters.riskLevel || undefined
      }),
      listRecentSkillLogs({ limit: 6 }),
      listRecentSkillLogs({ limit: 4, success: 0 })
    ])
    Object.assign(metrics, metricData || {})
    rows.value = pageData?.records || []
    total.value = Number(pageData?.total || 0)
    recentLogs.value = recentData || []
    exceptionLogs.value = failedData || []
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  if (roles.value.length) return
  const data = await listRole()
  roles.value = Array.isArray(data) ? data.filter((role) => Number(role.status ?? 1) === 1) : []
}

function search() {
  filters.current = 1
  loadDashboard()
}

async function openCreate() {
  resetForm()
  formMode.value = 'create'
  await loadRoles()
  formVisible.value = true
}

async function openEdit(row) {
  const detail = await getSkill(row.id)
  resetForm()
  Object.assign(skillForm, detail)
  skillForm.tags = detail?.tags || []
  skillForm.roleCodes = normalizeRoles(detail?.roleCodes)
  formMode.value = 'edit'
  await loadRoles()
  formVisible.value = true
}

function handleNameBlur() {
  if (formMode.value === 'create' && !skillForm.skillCode) {
    skillForm.skillCode = generateCode(skillForm.skillName)
  }
}

async function saveSkillForm() {
  if (!skillForm.skillName.trim() || !skillForm.skillCode.trim()) {
    ElMessage.warning('请填写技能名称和技能编码')
    return
  }
  if (!skillForm.description.trim()) {
    ElMessage.warning('请填写技能描述')
    return
  }
  if (!skillForm.roleCodes.length) {
    ElMessage.warning('请至少选择一个角色范围')
    return
  }
  formSaving.value = true
  try {
    if (formMode.value === 'create') {
      const created = await createSkill({
        ...buildSkillPayload(),
        status: 0
      })
      formVisible.value = false
      ElMessage.success('技能已创建，当前为停用状态')
      await loadDashboard()
      await openEditor(created)
    } else {
      await updateSkill(buildSkillPayload())
      formVisible.value = false
      ElMessage.success('技能配置已更新')
      await loadDashboard()
    }
  } finally {
    formSaving.value = false
  }
}

async function toggleStatus(row) {
  const detail = await getSkill(row.id)
  await updateSkill({
    ...buildSkillPayload(detail),
    status: Number(row.status) === 1 ? 0 : 1
  })
  ElMessage.success(Number(row.status) === 1 ? '技能已停用' : '技能已启用')
  await loadDashboard()
}

async function removeSkill(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除技能“${row.skillName}”吗？有关联或使用历史时将执行逻辑删除。`,
      '删除技能',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteSkill(row.id)
  ElMessage.success('技能已删除')
  await loadDashboard()
}

async function openEditor(row) {
  const detail = row?.skillCode ? row : await getSkill(row.id)
  editorSkill.value = {
    ...detail,
    tags: detail.tags || [],
    roleCodes: normalizeRoles(detail.roleCodes)
  }
  resourceRows.value = await listSkillFilesBySkill(detail.id)
  virtualFolders.value = []
  activeNode.value = { type: 'main', path: 'SKILL.md', id: 'main' }
  editorContent.value = detail.skillContent || ''
  editorVisible.value = true
}

async function selectResource(node) {
  activeNode.value = node
  if (node.type === 'main') {
    editorContent.value = editorSkill.value?.skillContent || ''
    return
  }
  if (node.type === 'folder') {
    editorContent.value = ''
    return
  }
  if (!node.loaded) {
    node.content = await getSkillFileContent(node.resourceId)
    node.loaded = true
  }
  editorContent.value = node.content || ''
}

async function saveEditorContent() {
  if (!editorSkill.value || activeNode.value.type === 'folder') return
  editorSaving.value = true
  try {
    if (activeNode.value.type === 'main') {
      editorSkill.value.skillContent = editorContent.value
      const updated = await updateSkill(buildSkillPayload(editorSkill.value))
      editorSkill.value = { ...editorSkill.value, ...updated }
    } else {
      await updateSkillPackageFile({
        id: activeNode.value.resourceId,
        resourcePath: activeNode.value.path,
        resourceContent: editorContent.value
      })
      const resource = resourceRows.value.find(
        (item) => String(item.id) === String(activeNode.value.resourceId)
      )
      if (resource) resource.resourceContent = editorContent.value
      activeNode.value.content = editorContent.value
    }
    ElMessage.success('内容已保存')
    await loadDashboard()
  } finally {
    editorSaving.value = false
  }
}

function openNewResource(parentPath = '') {
  Object.assign(resourceForm, {
    id: null,
    path: parentPath ? `${parentPath}/` : '',
    content: ''
  })
  resourceDialogVisible.value = true
}

function openNewFolder(parentPath = '') {
  folderPath.value = parentPath ? `${parentPath}/` : ''
  folderDialogVisible.value = true
}

function createVirtualFolder() {
  const path = folderPath.value.trim().replace(/\/+$/, '')
  if (!path || path.includes('\\') || path.split('/').some((part) => !part || part === '.' || part === '..')) {
    ElMessage.warning('请输入有效的相对目录路径')
    return
  }
  if (!virtualFolders.value.includes(path)) virtualFolders.value.push(path)
  folderDialogVisible.value = false
}

async function saveResource() {
  if (!resourceForm.path.trim()) {
    ElMessage.warning('请填写资源相对路径')
    return
  }
  resourceSaving.value = true
  try {
    const created = await createSkillPackageNode({
      skillId: editorSkill.value.id,
      resourcePath: resourceForm.path.trim(),
      resourceContent: resourceForm.content,
      directory: false
    })
    resourceRows.value.push(created)
    resourceDialogVisible.value = false
    ElMessage.success('资源文件已创建')
  } finally {
    resourceSaving.value = false
  }
}

async function deleteActiveNode() {
  await deleteTreeNode(activeNode.value)
}

async function deleteTreeNode(node) {
  if (!node || node.type === 'main') return
  try {
    await ElMessageBox.confirm(
      `确认删除“${node.path}”吗？`,
      node.type === 'folder' ? '删除目录' : '删除文件',
      { type: 'warning' }
    )
  } catch {
    return
  }
  if (node.type === 'folder') {
    await deleteSkillPackageFolder(editorSkill.value.id, node.path)
    resourceRows.value = resourceRows.value.filter(
      (item) => !String(item.resourcePath).startsWith(`${node.path}/`)
    )
    virtualFolders.value = virtualFolders.value.filter(
      (path) => path !== node.path && !path.startsWith(`${node.path}/`)
    )
  } else {
    await deleteSkillPackageNode(node.resourceId)
    resourceRows.value = resourceRows.value.filter(
      (item) => String(item.id) !== String(node.resourceId)
    )
  }
  const activePath = activeNode.value?.path || ''
  const removedActiveNode = node.type === 'folder'
    ? activePath === node.path || activePath.startsWith(`${node.path}/`)
    : String(activeNode.value?.resourceId) === String(node.resourceId)
  if (removedActiveNode) {
    await selectResource({ type: 'main', path: 'SKILL.md', id: 'main' })
  }
  ElMessage.success('已删除')
}

async function handleTreeCommand(command, node) {
  if (command === 'new-file') {
    openNewResource(node.path)
    return
  }
  if (command === 'new-folder') {
    openNewFolder(node.path)
    return
  }
  if (command === 'delete') {
    await deleteTreeNode(node)
  }
}

function triggerUpload() {
  uploadInput.value?.click()
}

async function handleUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 1024 * 1024) {
    ElMessage.warning('资源文件不能超过 1 MiB')
    return
  }
  const parent = activeNode.value.type === 'folder' ? `${activeNode.value.path}/` : ''
  Object.assign(resourceForm, {
    path: `${parent}${file.name}`,
    content: await file.text()
  })
  await saveResource()
}

async function openLogs(options = {}) {
  Object.assign(logFilters, {
    current: 1,
    skillId: options.skillId ?? null,
    success: options.success ?? '',
    operation: options.operation ?? ''
  })
  logDialogVisible.value = true
  await loadLogs()
}

async function loadLogs() {
  logLoading.value = true
  try {
    const data = await pageSkillLogs({
      current: logFilters.current,
      size: logFilters.size,
      skillId: logFilters.skillId || undefined,
      success: logFilters.success === '' ? undefined : logFilters.success,
      operation: logFilters.operation || undefined
    })
    logRows.value = data?.records || []
    logTotal.value = Number(data?.total || 0)
  } finally {
    logLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadDashboard()])
})
</script>

<template>
  <section class="skill-page management-page">
    <div class="metric-grid management-metrics">
      <article v-for="card in metricCards" :key="card.label" class="metric-card management-metric-card">
        <div class="metric-icon management-metric-icon" :class="card.tone"><el-icon><component :is="card.icon" /></el-icon></div>
        <div>
          <small>{{ card.label }}</small>
          <strong>{{ card.value }}</strong>
          <p>{{ card.note }}</p>
        </div>
      </article>
    </div>

    <div class="content-grid management-content-grid">
      <main class="main-column">
        <section class="panel list-panel management-panel">
          <div class="panel-title management-panel-title">
            <div>
              <h3>技能列表</h3>
              <p>列表指标均来自当天真实使用日志。</p>
            </div>
            <div class="filters management-filter-bar">
              <el-input
                v-model="filters.keyword"
                clearable
                placeholder="搜索名称、编码或描述"
                :prefix-icon="Search"
                @clear="search"
                @keyup.enter="search"
              />
              <el-select v-model="filters.category" clearable placeholder="全部分类" @change="search">
                <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-select v-model="filters.status" clearable placeholder="全部状态" @change="search">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
              <el-select v-model="filters.riskLevel" clearable placeholder="全部风险" @change="search">
                <el-option v-for="risk in riskOptions" :key="risk" :label="risk" :value="risk" />
              </el-select>
              <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="openCreate">新建技能</el-button>
            </div>
          </div>

          <div v-loading="loading" class="skill-grid">
            <article v-for="row in rows" :key="row.id" class="skill-card management-data-card">
              <el-dropdown class="management-card-menu" trigger="click">
                <button class="management-card-menu-button" type="button" aria-label="技能操作">
                  <el-icon class="management-card-menu-icon"><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="toggleStatus(row)">
                      {{ Number(row.status) === 1 ? '停用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="removeSkill(row)">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <div class="skill-card-head">
                <div class="skill-avatar"><el-icon><Setting /></el-icon></div>
                <div class="skill-identity">
                  <h4>{{ row.skillName }}</h4>
                  <code>{{ row.skillCode }}</code>
                </div>
                <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'" effect="light">
                  {{ Number(row.status) === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
              <p class="skill-description">{{ row.description }}</p>
              <div class="tag-line">
                <el-tag size="small">{{ categoryLabel(row.category) }}</el-tag>
                <el-tag size="small" :type="riskType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
                <el-tag v-if="row.hasScripts" size="small" type="warning">含脚本</el-tag>
              </div>
              <div class="skill-stats">
                <span><strong>{{ row.todayUses }}</strong> 今日使用</span>
                <span><strong>{{ row.boundAgents }}</strong> 绑定 Agent</span>
                <span><strong>{{ formatPercent(row.successRate) }}</strong> 成功率</span>
              </div>
              <footer>
                <el-button link type="primary" @click="openEditor(row)">编辑内容</el-button>
                <el-button link @click="openEdit(row)">配置</el-button>
                <el-button link @click="openLogs({ skillId: row.id })">使用记录</el-button>
              </footer>
            </article>
            <el-empty v-if="!loading && !rows.length" description="暂无符合条件的技能" />
          </div>

          <el-pagination
            v-model:current-page="filters.current"
            v-model:page-size="filters.size"
            class="management-pagination"
            background
            layout="total, sizes, prev, pager, next"
            :page-sizes="[8, 16, 32]"
            :total="total"
            @change="loadDashboard"
          />
        </section>
      </main>

      <aside class="side-column management-side-column">
        <section class="panel activity-panel management-side-card">
          <div class="panel-title compact">
            <div><h3>最近使用记录</h3><p>最近 6 条技能读取行为</p></div>
            <el-button link type="primary" @click="openLogs()">查看全部</el-button>
          </div>
          <div class="activity-list">
            <button
              v-for="log in recentLogs"
              :key="log.id"
              class="activity-item"
              @click="openLogs({ skillId: log.skillId })"
            >
              <span class="activity-dot" :class="{ failed: Number(log.success) === 0 }" />
              <span>
                <strong>{{ log.skillName || log.skillCode || '已删除技能' }}</strong>
                <small>{{ log.agentName || `Agent #${log.agentId}` }} · {{ operationLabel(log.operation) }}</small>
              </span>
              <time>{{ formatDuration(log.durationMs) }}</time>
            </button>
            <el-empty v-if="!recentLogs.length" :image-size="54" description="暂无使用记录" />
          </div>
        </section>

        <section class="panel exception-panel management-side-card">
          <div class="panel-title compact">
            <div><h3>异常使用</h3><p>最近 4 条失败记录</p></div>
            <el-button link type="danger" @click="openLogs({ success: 0 })">查看全部</el-button>
          </div>
          <div class="activity-list">
            <button
              v-for="log in exceptionLogs"
              :key="log.id"
              class="activity-item"
              @click="openLogs({ skillId: log.skillId, success: 0 })"
            >
              <span class="activity-dot failed" />
              <span>
                <strong>{{ log.skillName || log.skillCode || '已删除技能' }}</strong>
                <small>{{ log.errorMessage || '技能读取失败' }}</small>
              </span>
              <time>{{ log.startedAt || '-' }}</time>
            </button>
            <el-empty v-if="!exceptionLogs.length" :image-size="54" description="暂无异常记录" />
          </div>
        </section>
      </aside>
    </div>

    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新建技能' : '编辑技能配置'"
      width="680px"
      destroy-on-close
    >
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="技能名称" required>
            <el-input
              v-model="skillForm.skillName"
              :disabled="formMode === 'edit'"
              maxlength="100"
              @blur="handleNameBlur"
            />
          </el-form-item>
          <el-form-item label="技能编码" required>
            <el-input
              v-model="skillForm.skillCode"
              :disabled="formMode === 'edit'"
              maxlength="64"
              placeholder="例如 data-analysis"
            />
          </el-form-item>
          <el-form-item label="分类" required>
            <el-select v-model="skillForm.category">
              <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="风险等级">
            <el-select v-model="skillForm.riskLevel">
              <el-option v-for="risk in riskOptions" :key="risk" :label="risk" :value="risk" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="技能描述" required>
          <el-input
            v-model="skillForm.description"
            :disabled="formMode === 'edit'"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="skillForm.tags" multiple filterable allow-create default-first-option>
            <el-option v-for="tag in skillForm.tags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="可用角色" required>
          <el-select
            :model-value="skillForm.roleCodes"
            multiple
            @change="handleRoleChange"
          >
            <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formMode === 'edit'" label="状态">
          <el-switch v-model="skillForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-alert
          v-if="formMode === 'edit'"
          class="metadata-alert"
          type="info"
          :closable="false"
          title="技能名称和描述以 SKILL.md 顶部元数据中的 name、description 为准，请在内容编辑器中修改。"
        />
        <el-alert
          v-else
          type="info"
          :closable="false"
          title="新技能默认停用。创建后将自动进入 SKILL.md 编辑器，完成内容后再启用。"
        />
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSaving" @click="saveSkillForm">
          {{ formMode === 'create' ? '创建并编辑内容' : '保存配置' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="editorVisible" size="82%" :with-header="false" destroy-on-close>
      <div v-if="editorSkill" class="editor-shell">
        <header class="editor-header">
          <div>
            <span>技能内容编辑器</span>
            <h3>{{ editorSkill.skillName }} <code>{{ editorSkill.skillCode }}</code></h3>
          </div>
          <div>
            <input ref="uploadInput" type="file" hidden @change="handleUpload">
            <el-button :icon="Upload" @click="triggerUpload">上传文本</el-button>
            <el-button type="primary" :loading="editorSaving" @click="saveEditorContent">保存当前文件</el-button>
            <el-button @click="editorVisible = false">关闭</el-button>
          </div>
        </header>
        <div class="editor-body">
          <aside class="resource-sidebar">
            <div class="resource-actions">
              <el-button size="small" :icon="Plus" @click="openNewResource(activeNode.type === 'folder' ? activeNode.path : '')">文件</el-button>
              <el-button size="small" :icon="Folder" @click="openNewFolder(activeNode.type === 'folder' ? activeNode.path : '')">目录</el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :icon="Delete"
                :disabled="activeNode.type === 'main'"
                @click="deleteActiveNode"
              />
            </div>
            <el-tree
              :data="resourceTree"
              node-key="id"
              default-expand-all
              highlight-current
              :expand-on-click-node="false"
              @node-click="selectResource"
            >
              <template #default="{ data }">
                <span class="tree-node">
                  <span class="tree-node-main">
                    <el-icon><Folder v-if="data.type === 'folder'" /><Document v-else /></el-icon>
                    <span class="tree-node-label">{{ data.label }}</span>
                  </span>
                  <el-dropdown
                    v-if="data.type !== 'main'"
                    trigger="click"
                    @command="(command) => handleTreeCommand(command, data)"
                  >
                    <button
                      class="tree-node-more"
                      type="button"
                      title="更多操作"
                      @click.stop
                    >
                      <el-icon><MoreFilled /></el-icon>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item v-if="data.type === 'folder'" command="new-file">
                          新增文件
                        </el-dropdown-item>
                        <el-dropdown-item v-if="data.type === 'folder'" command="new-folder">
                          新增文件夹
                        </el-dropdown-item>
                        <el-dropdown-item
                          command="delete"
                          :divided="data.type === 'folder'"
                        >
                          删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </span>
              </template>
            </el-tree>
          </aside>
          <main class="code-editor">
            <div class="file-bar">
              <span>{{ activeNode.path }}</span>
              <small v-if="activeNode.type === 'folder'">目录由资源路径自动推导，不单独入库</small>
            </div>
            <el-input
              v-if="activeNode.type !== 'folder'"
              v-model="editorContent"
              type="textarea"
              resize="none"
              spellcheck="false"
              placeholder="输入 UTF-8 文本内容"
            />
            <el-empty v-else description="请选择文件，或在此目录下新建资源文件" />
          </main>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="resourceDialogVisible" title="新建资源文件" width="620px">
      <el-form label-position="top">
        <el-form-item label="相对路径" required>
          <el-input v-model="resourceForm.path" placeholder="例如 references/guide.md" />
        </el-form-item>
        <el-form-item label="文件内容">
          <el-input v-model="resourceForm.content" type="textarea" :rows="12" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resourceSaving" @click="saveResource">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="folderDialogVisible" title="新建虚拟目录" width="500px">
      <el-input v-model="folderPath" placeholder="例如 references/design" />
      <template #footer>
        <el-button @click="folderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createVirtualFolder">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logDialogVisible" title="技能使用记录" width="1080px">
      <div class="log-filters">
        <el-select v-model="logFilters.success" clearable placeholder="全部结果" @change="loadLogs">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-select v-model="logFilters.operation" clearable placeholder="全部操作" @change="loadLogs">
          <el-option v-for="item in operationOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button v-if="logFilters.skillId" @click="logFilters.skillId = null; loadLogs()">清除技能筛选</el-button>
      </div>
      <el-table v-loading="logLoading" :data="logRows">
        <el-table-column label="技能" min-width="150">
          <template #default="{ row }">{{ row.skillName || row.skillCode || '已删除技能' }}</template>
        </el-table-column>
        <el-table-column label="Agent" min-width="140">
          <template #default="{ row }">{{ row.agentName || `Agent #${row.agentId}` }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">{{ operationLabel(row.operation) }}</template>
        </el-table-column>
        <el-table-column prop="resourcePath" label="资源路径" min-width="180" show-overflow-tooltip />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="Number(row.success) === 1 ? 'success' : 'danger'" size="small">
              {{ Number(row.success) === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="170" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="logFilters.current"
        v-model:page-size="logFilters.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="logTotal"
        @change="loadLogs"
      />
    </el-dialog>
  </section>
</template>

<style scoped>
.panel-title,
.skill-card-head,
.editor-header,
.resource-actions,
.file-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

h2,
h3,
h4,
p {
  margin: 0;
}

.panel-title p {
  margin-top: 6px;
  color: #768198;
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.metric-card,
.panel {
  background: #fff;
  border: 1px solid #e8ecf3;
  border-radius: 14px;
  box-shadow: 0 8px 26px rgb(23 32 51 / 4%);
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
}

.metric-icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 12px;
  font-size: 22px;
}

.metric-icon.blue { color: #2563eb; background: #eaf1ff; }
.metric-icon.cyan { color: #0891b2; background: #e6f8fc; }
.metric-icon.purple { color: #7c3aed; background: #f0eaff; }
.metric-icon.green { color: #059669; background: #e8f8f2; }

.metric-card span,
.metric-card small {
  display: block;
  color: #7b8497;
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 4px 0;
  font-size: 25px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: start;
}

.panel {
  padding: 20px;
}

.panel-title {
  margin-bottom: 16px;
}

.panel-title.compact {
  align-items: flex-start;
}

.filters {
  display: grid;
  grid-template-columns: minmax(200px, 1.5fr) repeat(3, minmax(120px, 0.8fr)) auto auto;
  gap: 10px;
  margin-bottom: 18px;
}

.skill-grid {
  display: grid;
  min-height: 240px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.skill-card {
  padding: 17px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
}

.skill-card-head {
  padding-right: 36px;
}

.skill-avatar {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 10px;
  color: #2563eb;
  background: #edf3ff;
}

.skill-identity {
  min-width: 0;
  flex: 1;
}

.skill-identity h4 {
  overflow: hidden;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

code {
  color: #6b7590;
  font-size: 12px;
}

.skill-description {
  min-height: 42px;
  margin: 14px 0 10px;
  color: #687287;
  font-size: 13px;
  line-height: 1.6;
}

.tag-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.skill-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 15px 0;
  padding: 12px 0;
  border-block: 1px solid #eef1f6;
  color: #81899a;
  font-size: 11px;
}

.skill-stats strong {
  display: block;
  margin-bottom: 3px;
  color: #283248;
  font-size: 15px;
}

.skill-card footer {
  display: flex;
  align-items: center;
  gap: 4px;
}

.el-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

.side-column {
  display: grid;
  gap: 18px;
}

.activity-list {
  display: grid;
  gap: 4px;
}

.activity-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
  padding: 11px 6px;
  text-align: left;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #f0f2f6;
  cursor: pointer;
}

.activity-item:hover {
  background: #f8faff;
}

.activity-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
  background: #10b981;
}

.activity-dot.failed {
  background: #ef4444;
}

.activity-item strong,
.activity-item small {
  display: block;
}

.activity-item strong {
  margin-bottom: 4px;
  color: #283248;
  font-size: 13px;
}

.activity-item small,
.activity-item time {
  overflow: hidden;
  color: #8a93a5;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-grid :deep(.el-select),
.filters :deep(.el-select),
.log-filters :deep(.el-select),
.el-form-item :deep(.el-select) {
  width: 100%;
}

.editor-shell {
  display: flex;
  height: 100%;
  flex-direction: column;
}

.editor-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf2;
}

.editor-header h3 {
  margin-top: 5px;
}

.editor-header h3 code {
  margin-left: 8px;
}

.editor-body {
  display: grid;
  min-height: 0;
  flex: 1;
  grid-template-columns: 280px minmax(0, 1fr);
}

.resource-sidebar {
  overflow: auto;
  padding: 14px;
  background: #f8f9fc;
  border-right: 1px solid #e8ecf2;
}

.resource-actions {
  justify-content: flex-start;
  margin-bottom: 12px;
}

.tree-node {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.tree-node-main {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.tree-node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-more {
  display: grid;
  width: 25px;
  height: 25px;
  flex: 0 0 auto;
  place-items: center;
  padding: 0;
  color: #7d8799;
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
  opacity: .65;
}

.tree-node:hover .tree-node-more,
.tree-node-more:focus {
  opacity: 1;
}

.tree-node-more:hover {
  color: #2563eb;
  background: #e9f0ff;
}

.metadata-alert {
  margin-bottom: 14px;
}

.code-editor {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.file-bar {
  padding: 10px 16px;
  color: #626d83;
  background: #fbfcfe;
  border-bottom: 1px solid #e8ecf2;
}

.code-editor :deep(.el-textarea) {
  flex: 1;
}

.code-editor :deep(.el-textarea__inner) {
  height: 100% !important;
  padding: 20px;
  color: #d8dee9;
  background: #1f2430;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  font: 14px/1.7 Consolas, Monaco, monospace;
}

.log-filters {
  display: grid;
  grid-template-columns: 160px 180px auto;
  gap: 10px;
  margin-bottom: 14px;
}

@media (max-width: 1200px) {
  .metric-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
  .side-column { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 820px) {
  .skill-page { padding: 14px; }
  .metric-grid,
  .skill-grid,
  .side-column,
  .form-grid { grid-template-columns: 1fr; }
  .filters { grid-template-columns: 1fr 1fr; }
  .editor-body { grid-template-columns: 220px minmax(0, 1fr); }
}
</style>

<style scoped src="../../assets/management-page.css"></style>
