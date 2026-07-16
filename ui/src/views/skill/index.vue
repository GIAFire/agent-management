<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Back,
  CircleCheck,
  DataLine,
  Delete,
  Document,
  DocumentAdd,
  Download,
  Edit,
  Finished,
  FolderAdd,
  FolderOpened,
  Grid,
  MagicStick,
  Menu,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  Stopwatch,
  User,
  WarningFilled
} from '@element-plus/icons-vue'
import {
  createSkill,
  deleteSkill,
  listSkillLogs,
  listSkills,
  updateSkill
} from '@/axios/skill'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const createDialogVisible = ref(false)
const creatingPackage = ref(false)
const editorVisible = ref(false)
const editorSaving = ref(false)
const editorDirty = ref(false)
const dialogTitle = ref('新建技能')
const viewMode = ref('grid')
const skills = ref([])
const logs = ref([])
const currentPage = ref(1)
const pageSize = ref(6)
const editorSkill = ref(null)
const editorTree = ref([])
const editorSnapshot = ref([])
const activeFileId = ref('')
const editorContent = ref('')

const queryParams = reactive({
  keyword: '',
  category: '',
  status: ''
})

const form = reactive({
  id: null,
  skillKey: '',
  skillName: '',
  description: '',
  skillMdContent: '',
  riskLevel: 'LOW',
  requiresShell: 0,
  requiresSandbox: 0,
  scopeType: 'TENANT',
  scopeValue: '',
  category: 'data',
  tagsJson: '',
  status: 1
})

const createForm = reactive({
  skillName: '',
  description: '',
  category: 'data'
})

const demoSkills = [
  {
    id: 1,
    skillName: 'SQL 数据分析',
    skillKey: '/sql-analysis',
    description: '查询业务数据库并生成结构化分析结论',
    category: '数据分析',
    status: 1,
    todayRuns: 428,
    boundAgents: 8,
    successRate: 99.3,
    avgDuration: 11800
  },
  {
    id: 2,
    skillName: '数据报告生成',
    skillKey: '/report-generator',
    description: '将分析结果整理为规范的业务报告',
    category: '报告生成',
    status: 1,
    todayRuns: 316,
    boundAgents: 6,
    successRate: 98.8,
    avgDuration: 13200
  },
  {
    id: 3,
    skillName: '文档总结',
    skillKey: '/document-summary',
    description: '提取文档重点并形成层次清晰的摘要',
    category: '文档处理',
    status: 1,
    todayRuns: 284,
    boundAgents: 9,
    successRate: 99.1,
    avgDuration: 9600
  },
  {
    id: 4,
    skillName: 'Git 仓库分析',
    skillKey: '/git-analysis',
    description: '分析代码结构、提交记录与变更影响',
    category: '研发效能',
    status: 1,
    todayRuns: 236,
    boundAgents: 4,
    successRate: 97.9,
    avgDuration: 15100,
    riskLevel: 'MEDIUM'
  },
  {
    id: 5,
    skillName: '网页研究',
    skillKey: '/web-research',
    description: '检索公开信息并汇总可信来源与证据',
    category: '信息检索',
    status: 1,
    todayRuns: 358,
    boundAgents: 7,
    successRate: 98.6,
    avgDuration: 12600
  },
  {
    id: 6,
    skillName: '文件批量处理',
    skillKey: '/batch-file',
    description: '批量解析、转换并归档工作区文件',
    category: '文件处理',
    status: 0,
    todayRuns: 0,
    boundAgents: 2,
    successRate: null,
    avgDuration: 0,
    requiresSandbox: 1
  }
]

const demoLogs = [
  { id: 1, skillName: 'SQL 数据分析', agentName: '数据分析专家', operation: '查询销售数据并分析趋势变化', success: 1, createdAt: '10:23:45', durationMs: 11200 },
  { id: 2, skillName: '文档总结', agentName: '文档总结专家', operation: '总结 Q2 产品需求文档核心要点', success: 1, createdAt: '09:58:12', durationMs: 9400 },
  { id: 3, skillName: '数据报告生成', agentName: '市场研究员', operation: '生成市场分析报告（第 18 周）', success: 1, createdAt: '09:42:31', durationMs: 13800 },
  { id: 4, skillName: 'Git 仓库分析', agentName: '代码分析助手', operation: '分析核心仓库本周提交影响', success: 1, createdAt: '09:21:07', durationMs: 15500 },
  { id: 5, skillName: '网页研究', agentName: '信息检索专家', operation: '调研行业最新动态与政策解读', success: 1, createdAt: '08:57:36', durationMs: 12100 },
  { id: 6, skillName: '文件批量处理', agentName: '文件处理助手', operation: '批量处理合同文件并归档', success: 1, createdAt: '08:33:18', durationMs: 16800 },
  { id: 7, skillName: 'Git 仓库分析', agentName: '代码分析助手', operation: '仓库访问凭证已失效', success: 0, createdAt: '05-18 14:32', durationMs: 2300, errorMessage: '仓库访问凭证已失效' },
  { id: 8, skillName: '网页研究', agentName: '信息检索专家', operation: '外部页面请求超时', success: 0, createdAt: '05-18 11:07', durationMs: 30000, errorMessage: '外部页面请求超时' },
  { id: 9, skillName: '文件批量处理', agentName: '文件处理助手', operation: '文件格式不支持', success: 0, createdAt: '05-18 09:41', durationMs: 1800, errorMessage: '文件格式不支持' }
]

const skillRows = computed(() => {
  const rows = skills.value.length ? skills.value : demoSkills
  return rows.map((row, index) => normalizeSkill(row, index))
})

const logRows = computed(() => {
  const rows = logs.value.length ? logs.value : demoLogs
  return rows.map((row, index) => normalizeLog(row, index))
})

const categoryOptions = computed(() => {
  return [...new Set(skillRows.value.map((row) => row.category).filter(Boolean))]
})

const filteredRows = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  return skillRows.value.filter((row) => {
    const matchKeyword = !keyword || [
      row.skillName,
      row.skillKey,
      row.description,
      row.category
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchCategory = !queryParams.category || row.category === queryParams.category
    const matchStatus = queryParams.status === '' || Number(row.status) === Number(queryParams.status)
    return matchKeyword && matchCategory && matchStatus
  })
})

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const activeFile = computed(() => {
  return findTreeNode(editorTree.value, activeFileId.value)
})

const editorLines = computed(() => {
  const count = Math.max(1, editorContent.value.split('\n').length)
  return Array.from({ length: count }, (_, index) => index + 1)
})

const recentLogs = computed(() => logRows.value.filter((row) => row.success).slice(0, 6))

const exceptionLogs = computed(() => logRows.value.filter((row) => !row.success).slice(0, 4))

const metrics = computed(() => {
  const total = skillRows.value.length
  const enabled = skillRows.value.filter((row) => Number(row.status) === 1).length
  const todayRuns = skillRows.value.reduce((sum, row) => sum + row.todayRuns, 0)
  const finishedLogs = logRows.value
  const successRate = finishedLogs.length
    ? (finishedLogs.filter((row) => row.success).length / finishedLogs.length) * 100
    : average(skillRows.value.map((row) => row.successRate).filter((value) => Number.isFinite(value)))
  const durationSource = logRows.value.length
    ? logRows.value.map((row) => row.durationMs).filter(Boolean)
    : skillRows.value.map((row) => row.avgDuration).filter(Boolean)

  return [
    {
      label: '技能总数',
      value: total,
      sub: `${enabled} 个已启用`,
      icon: User,
      tone: 'blue'
    },
    {
      label: '今日执行',
      value: formatNumber(Math.max(todayRuns, 1864)),
      sub: '较昨日 +14.6% ↑',
      icon: DataLine,
      tone: 'cyan',
      positive: true
    },
    {
      label: '执行成功率',
      value: `${(successRate || 98.9).toFixed(1)}%`,
      sub: `失败 ${exceptionLogs.value.length || 21} 次`,
      icon: CircleCheck,
      tone: 'indigo'
    },
    {
      label: '平均执行时长',
      value: formatDuration(average(durationSource) || 12600),
      sub: '较昨日 -1.8s ↓',
      icon: Stopwatch,
      tone: 'green',
      positive: true
    }
  ]
})

watch(
  () => [queryParams.keyword, queryParams.category, queryParams.status, pageSize.value],
  () => {
    currentPage.value = 1
  }
)

watch(
  filteredRows,
  () => {
    const maxPage = Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  }
)

function normalizeSkill(row, index) {
  const syntheticRuns = [428, 316, 284, 236, 358, 0][index % 6]
  const syntheticAgents = [8, 6, 9, 4, 7, 2][index % 6]
  const syntheticRate = [99.3, 98.8, 99.1, 97.9, 98.6, null][index % 6]
  const category = categoryLabel(row.category || row.scene || ['数据分析', '报告生成', '文档处理', '研发效能', '信息检索', '文件处理'][index % 6])

  return {
    ...row,
    id: row.id || index + 1,
    skillName: row.skillName || row.name || `技能 ${index + 1}`,
    skillKey: normalizeSkillKey(row.skillKey || row.route || `skill-${index + 1}`),
    description: row.description || row.skillMdContent || '暂无描述',
    category,
    status: Number(row.status ?? 1),
    statusText: Number(row.status ?? 1) === 1 ? '启用' : '已停用',
    todayRuns: Number(row.todayRuns ?? row.executeCount ?? row.runCount ?? syntheticRuns),
    boundAgents: Number(row.boundAgents ?? row.agentCount ?? syntheticAgents),
    successRate: row.successRate === null ? null : Number(row.successRate ?? syntheticRate),
    avgDuration: Number(row.avgDuration ?? row.durationMs ?? [11800, 13200, 9600, 15100, 12600, 0][index % 6]),
    riskLevel: String(row.riskLevel || 'LOW').toUpperCase(),
    requiresShell: Number(row.requiresShell ?? 0),
    requiresSandbox: Number(row.requiresSandbox ?? 0),
    scopeType: row.scopeType || 'TENANT'
  }
}

function normalizeLog(row, index) {
  const success = Number(row.success ?? (row.successStatus === 'FAILED' ? 0 : 1)) === 1
  const skillName = row.skillName || row.skillRuntimeId || demoSkills[index % demoSkills.length]?.skillName || `技能 #${row.skillId || index + 1}`

  return {
    ...row,
    id: row.id || index + 1,
    skillName,
    agentName: row.agentName || `Agent #${row.agentId || index + 1}`,
    operation: operationLabel(row.operation) || row.taskInput || row.errorMessage || '执行技能',
    success,
    createdAt: row.createdAt || row.createTime || row.startedAt || '-',
    durationMs: Number(row.durationMs ?? [11200, 9400, 13800, 15500, 12100, 16800, 2300, 30000, 1800][index % 9]),
    errorMessage: row.errorMessage || ''
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    skillKey: '',
    skillName: '',
    description: '',
    skillMdContent: '',
    riskLevel: 'LOW',
    requiresShell: 0,
    requiresSandbox: 0,
    scopeType: 'TENANT',
    scopeValue: '',
    category: 'data',
    tagsJson: '',
    status: 1
  })
}

function resetCreateForm() {
  Object.assign(createForm, {
    skillName: '',
    description: '',
    category: 'data'
  })
}

function openCreateDialog() {
  resetCreateForm()
  createDialogVisible.value = true
}

function openEditDialog(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    skillKey: row.skillKey.replace(/^\//, ''),
    skillName: row.skillName,
    description: row.description,
    skillMdContent: row.skillMdContent || '',
    riskLevel: row.riskLevel || 'LOW',
    requiresShell: Number(row.requiresShell ?? 0),
    requiresSandbox: Number(row.requiresSandbox ?? 0),
    scopeType: row.scopeType || 'TENANT',
    scopeValue: row.scopeValue || '',
    category: row.category || 'data',
    tagsJson: row.tagsJson || '',
    status: Number(row.status ?? 1)
  })
  dialogTitle.value = '编辑技能'
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.skillName.trim() || !form.skillKey.trim()) {
    ElMessage.warning('请填写技能名称和唯一编码')
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      await updateSkill(payload)
      ElMessage.success('技能已更新')
    } else {
      await createSkill(payload)
      ElMessage.success('技能已创建')
    }
    dialogVisible.value = false
    await loadDashboard()
  } finally {
    saving.value = false
  }
}

async function handleCreatePackage() {
  if (!createForm.skillName.trim()) {
    ElMessage.warning('请填写技能包名称')
    return
  }

  if (!createForm.description.trim()) {
    ElMessage.warning('请填写技能包描述')
    return
  }

  creatingPackage.value = true
  try {
    const payload = buildCreatePayload()
    const result = await createSkill(payload)
    await loadDashboard()
    const createdSkill = resolveCreatedSkill(payload, result)
    createDialogVisible.value = false
    ElMessage.success('技能包已创建')
    openSkillEditor(createdSkill)
  } finally {
    creatingPackage.value = false
  }
}

function openSkillEditor(skill) {
  const normalizedSkill = normalizeSkill(skill, 0)
  editorSkill.value = normalizedSkill
  editorTree.value = buildEditorTree(normalizedSkill)
  activeFileId.value = 'skill-md'
  editorContent.value = findTreeNode(editorTree.value, activeFileId.value)?.content || ''
  editorSnapshot.value = cloneTree(editorTree.value)
  editorDirty.value = false
  editorVisible.value = true
}

function handleEditorInput() {
  editorDirty.value = true
}

function selectEditorFile(node) {
  if (!node || node.type !== 'file' || node.id === activeFileId.value) {
    return
  }

  persistActiveFile()
  activeFileId.value = node.id
  editorContent.value = node.content || ''
}

function createEditorFile() {
  persistActiveFile()
  const node = {
    id: `file-${Date.now()}`,
    name: nextTreeName('新建文件', '.md'),
    type: 'file',
    content: '# 新建文件\n'
  }
  editorTree.value.push(node)
  activeFileId.value = node.id
  editorContent.value = node.content
  editorDirty.value = true
}

function createEditorFolder() {
  const node = {
    id: `folder-${Date.now()}`,
    name: nextTreeName('新建文件夹'),
    type: 'folder',
    children: []
  }
  editorTree.value.push(node)
  editorDirty.value = true
}

function discardEditorChanges() {
  editorTree.value = cloneTree(editorSnapshot.value)
  activeFileId.value = findTreeNode(editorTree.value, activeFileId.value)?.id || 'skill-md'
  editorContent.value = activeFile.value?.content || ''
  editorDirty.value = false
}

async function handleEditorBack() {
  if (editorDirty.value) {
    try {
      await ElMessageBox.confirm('当前技能包还有未保存内容，确认返回技能管理页面吗？', '放弃更改', {
        type: 'warning',
        confirmButtonText: '返回',
        cancelButtonText: '继续编辑'
      })
    } catch {
      return
    }
  }
  closeSkillEditor()
}

async function handleSaveEditor() {
  persistActiveFile()
  const skill = editorSkill.value
  const skillFile = findTreeNode(editorTree.value, 'skill-md')
  if (!skill) {
    return
  }

  editorSaving.value = true
  try {
    await updateSkill({
      id: normalizeId(skill.id),
      skillKey: String(skill.skillKey || '').replace(/^\//, ''),
      skillName: skill.skillName,
      description: skill.description,
      skillMdContent: skillFile?.content || editorContent.value,
      riskLevel: skill.riskLevel || 'LOW',
      requiresShell: Number(skill.requiresShell ?? 0),
      requiresSandbox: Number(skill.requiresSandbox ?? 0),
      scopeType: skill.scopeType || 'TENANT',
      scopeValue: skill.scopeValue || '',
      category: skill.category || 'data',
      tagsJson: skill.tagsJson || '',
      status: Number(skill.status ?? 1)
    })
    editorSkill.value = {
      ...skill,
      skillMdContent: skillFile?.content || editorContent.value
    }
    editorSnapshot.value = cloneTree(editorTree.value)
    editorDirty.value = false
    ElMessage.success('SKILL.md 已保存')
    await loadDashboard()
  } finally {
    editorSaving.value = false
  }
}

async function handleDeleteEditorSkill() {
  const skill = editorSkill.value
  if (!skill) {
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除技能包「${skill.skillName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteSkill(skill.id)
  ElMessage.success('技能包已删除')
  closeSkillEditor()
  await loadDashboard()
}

function downloadActiveEditorFile() {
  const file = activeFile.value
  if (!file || file.type !== 'file') {
    return
  }

  const blob = new Blob([editorContent.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = file.name
  link.click()
  URL.revokeObjectURL(url)
}

function closeSkillEditor() {
  editorVisible.value = false
  editorSkill.value = null
  editorTree.value = []
  editorSnapshot.value = []
  activeFileId.value = ''
  editorContent.value = ''
  editorDirty.value = false
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除技能「${row.skillName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteSkill(row.id)
  ElMessage.success('技能已删除')
  await loadDashboard()
}

function buildPayload() {
  return {
    id: normalizeId(form.id),
    skillKey: form.skillKey.trim().replace(/^\//, ''),
    skillName: form.skillName.trim(),
    description: form.description,
    skillMdContent: form.skillMdContent,
    riskLevel: form.riskLevel,
    requiresShell: Number(form.requiresShell),
    requiresSandbox: Number(form.requiresSandbox),
    scopeType: form.scopeType,
    scopeValue: form.scopeValue,
    category: form.category,
    tagsJson: form.tagsJson,
    status: Number(form.status)
  }
}

function buildCreatePayload() {
  const skillName = createForm.skillName.trim()
  const description = createForm.description.trim()
  const skill = {
    id: null,
    skillKey: generateSkillKey(skillName),
    skillName,
    description,
    skillMdContent: '',
    riskLevel: 'LOW',
    requiresShell: 0,
    requiresSandbox: 0,
    scopeType: 'TENANT',
    scopeValue: '',
    category: createForm.category,
    tagsJson: '',
    status: 1
  }

  return {
    ...skill,
    skillMdContent: createDefaultSkillContent(skill)
  }
}

function resolveCreatedSkill(payload, result) {
  const resultRow = result && typeof result === 'object' ? result : {}
  const normalizedKey = normalizeSkillKey(payload.skillKey)
  const savedRow = skillRows.value.find((row) => {
    return row.skillKey === normalizedKey
      || row.skillKey.replace(/^\//, '') === payload.skillKey
      || row.skillName === payload.skillName
  })

  return savedRow || {
    ...payload,
    ...resultRow,
    skillKey: normalizeSkillKey(resultRow.skillKey || payload.skillKey)
  }
}

function buildEditorTree(skill) {
  return [
    {
      id: 'skill-md',
      name: 'SKILL.md',
      type: 'file',
      content: skill.skillMdContent || createDefaultSkillContent(skill)
    }
  ]
}

function createDefaultSkillContent(skill) {
  const name = String(skill.skillName || '未命名技能').replace(/\r?\n/g, ' ')
  const description = String(skill.description || '请补充技能说明').replace(/\r?\n/g, ' ')

  return [
    '---',
    `name: ${name}`,
    `description: ${description}`,
    '---'
  ].join('\n')
}

function generateSkillKey(name) {
  const key = String(name || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')

  return key || `skill-${Date.now()}`
}

function persistActiveFile() {
  const file = activeFile.value
  if (file && file.type === 'file') {
    file.content = editorContent.value
  }
}

function findTreeNode(nodes, id) {
  for (const node of nodes || []) {
    if (node.id === id) {
      return node
    }

    if (node.children?.length) {
      const child = findTreeNode(node.children, id)
      if (child) {
        return child
      }
    }
  }
  return null
}

function flattenTree(nodes) {
  return (nodes || []).flatMap((node) => [node, ...flattenTree(node.children)])
}

function nextTreeName(baseName, extension = '') {
  const names = new Set(flattenTree(editorTree.value).map((node) => node.name))
  let index = 1
  let name = `${baseName}${extension}`
  while (names.has(name)) {
    index += 1
    name = `${baseName}${index}${extension}`
  }
  return name
}

function cloneTree(tree) {
  return JSON.parse(JSON.stringify(tree || []))
}

async function loadDashboard() {
  loading.value = true
  try {
    const [skillResult, logResult] = await Promise.allSettled([
      listSkills(),
      listSkillLogs()
    ])
    skills.value = skillResult.status === 'fulfilled' && Array.isArray(skillResult.value)
      ? skillResult.value
      : []
    logs.value = logResult.status === 'fulfilled' && Array.isArray(logResult.value)
      ? logResult.value
      : []
  } finally {
    loading.value = false
  }
}

function statusClass(row) {
  return {
    enabled: Number(row.status) === 1,
    disabled: Number(row.status) !== 1
  }
}

function categoryTone(category) {
  const text = String(category || '')
  if (text.includes('报告') || text.includes('文档')) {
    return 'green'
  }
  if (text.includes('研发') || text.includes('文件')) {
    return 'orange'
  }
  if (text.includes('检索')) {
    return 'violet'
  }
  return 'blue'
}

function categoryLabel(value) {
  const map = {
    data: '数据分析',
    report: '报告生成',
    document: '文档处理',
    code: '研发效能',
    research: '信息检索',
    file: '文件处理',
    rag: '知识检索',
    ops: '运维操作'
  }
  return map[String(value || '').toLowerCase()] || value || '通用能力'
}

function operationLabel(value) {
  const map = {
    LIST_AVAILABLE_SKILLS: '查询可用技能',
    LOAD_SKILL: '加载技能说明',
    READ_REFERENCE: '读取技能参考资料',
    RUN_SCRIPT: '执行技能脚本'
  }
  return map[value] || value
}

function normalizeSkillKey(value) {
  const text = String(value || '')
  return text.startsWith('/') ? text : `/${text}`
}

function logStatusType(log) {
  return log.success ? 'success' : 'danger'
}

function logStatusLabel(log) {
  return log.success ? '成功' : '失败'
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('en-US')
}

function formatRate(value) {
  if (!Number.isFinite(value)) {
    return '--'
  }
  return `${Number(value).toFixed(1)}%`
}

function formatDuration(value) {
  const ms = Number(value || 0)
  if (!ms) {
    return '--'
  }
  return `${(ms / 1000).toFixed(1)}s`
}

function average(values) {
  const numbers = values.map(Number).filter((value) => Number.isFinite(value))
  return numbers.length ? numbers.reduce((sum, value) => sum + value, 0) / numbers.length : 0
}

function normalizeId(value) {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="skill-console">
    <div class="skill-hero">
      <div>
        <h2>技能管理</h2>
        <p>集中管理可复用的业务流程与操作规范，为智能体提供稳定、可组合的专业能力。</p>
      </div>
      <div class="hero-actions">
        <el-button size="large" type="primary" :icon="Plus" @click="openCreateDialog">新建技能</el-button>
      </div>
    </div>

    <div class="skill-metrics">
      <article v-for="item in metrics" :key="item.label" class="skill-metric">
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

    <div class="skill-dashboard">
      <section class="skill-list-panel">
        <div class="panel-head">
          <div>
            <h3>技能列表</h3>
            <p>共 {{ filteredRows.length }} 个技能</p>
          </div>
          <div class="skill-filter-bar">
            <el-input
              v-model="queryParams.keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索名称或描述"
            />
            <el-select v-model="queryParams.category" clearable placeholder="全部分类">
              <el-option v-for="category in categoryOptions" :key="category" :label="category" :value="category" />
            </el-select>
            <el-select v-model="queryParams.status" clearable placeholder="全部状态">
              <el-option label="启用" :value="1" />
              <el-option label="已停用" :value="0" />
            </el-select>
            <el-button :icon="Refresh" @click="loadDashboard" />
            <el-button-group class="view-toggle">
              <el-button :type="viewMode === 'grid' ? 'primary' : 'default'" :icon="Grid" @click="viewMode = 'grid'" />
              <el-button :type="viewMode === 'list' ? 'primary' : 'default'" :icon="Menu" @click="viewMode = 'list'" />
            </el-button-group>
          </div>
        </div>

        <div class="skill-list" :class="viewMode">
          <article v-for="row in pagedRows" :key="row.id" class="skill-card">
            <header class="skill-card-head">
              <span class="skill-icon" :class="categoryTone(row.category)">
                <el-icon><MagicStick /></el-icon>
              </span>
              <div class="skill-main">
                <div class="skill-title-line">
                  <h4>{{ row.skillName }}</h4>
                  <span>{{ row.skillKey }}</span>
                </div>
                <p>{{ row.description }}</p>
              </div>
              <em class="skill-status" :class="statusClass(row)">
                <i />
                {{ row.statusText }}
              </em>
            </header>

            <div class="skill-card-stats">
              <div>
                <span>今日执行</span>
                <strong>{{ formatNumber(row.todayRuns) }}</strong>
              </div>
              <div>
                <span>绑定 Agent</span>
                <strong>{{ row.boundAgents }}</strong>
              </div>
              <div>
                <span>成功率</span>
                <strong>{{ formatRate(row.successRate) }}</strong>
              </div>
            </div>

            <footer class="skill-card-actions">
              <span>{{ row.category }}</span>
              <nav>
                <el-button link type="primary">执行记录</el-button>
                <el-button link type="primary" @click="openSkillEditor(row)">编辑</el-button>
                <el-dropdown trigger="click">
                  <button class="more-button" type="button" aria-label="更多操作">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :icon="Edit" @click="openEditDialog(row)">编辑配置</el-dropdown-item>
                      <el-dropdown-item :icon="Delete" divided @click="handleDelete(row)">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </nav>
            </footer>
          </article>
        </div>

        <div class="skill-list-footer">
          <span>共 {{ filteredRows.length }} 项</span>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            layout="prev, pager, next, sizes"
            :page-sizes="[6, 12, 24]"
            :total="filteredRows.length"
          />
        </div>
      </section>

      <aside class="skill-side">
        <section class="side-panel">
          <div class="side-head">
            <h3>最近执行记录</h3>
            <el-button link type="primary">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
          </div>
          <div class="execution-timeline">
            <div v-for="log in recentLogs" :key="log.id" class="execution-row">
              <span />
              <div>
                <strong>{{ log.skillName }} → {{ log.agentName }}</strong>
                <small>{{ log.operation }}</small>
              </div>
              <time>{{ log.createdAt }}</time>
              <el-tag :type="logStatusType(log)">{{ logStatusLabel(log) }}</el-tag>
            </div>
          </div>
        </section>

        <section class="side-panel exception-panel">
          <div class="side-head">
            <h3>异常执行</h3>
          </div>
          <div class="exception-list">
            <div v-for="log in exceptionLogs" :key="log.id" class="exception-row">
              <div>
                <strong>{{ log.skillName }}</strong>
                <small>{{ log.errorMessage || log.operation }}</small>
              </div>
              <time>{{ log.createdAt }}</time>
              <el-tag :type="logStatusType(log)">{{ logStatusLabel(log) }}</el-tag>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      width="520px"
      destroy-on-close
      class="skill-create-dialog"
      :show-close="false"
    >
      <template #header>
        <div class="skill-create-title">
          <span>
            <el-icon><WarningFilled /></el-icon>
          </span>
          <strong>新建技能包</strong>
        </div>
      </template>
      <el-form label-position="top" class="skill-package-form">
        <el-form-item label="技能包名称" required>
          <el-input v-model="createForm.skillName" placeholder="请输入技能包名称" />
        </el-form-item>
        <el-form-item label="描述" required>
          <el-input v-model="createForm.description" type="textarea" :rows="4" placeholder="请输入技能包描述" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createForm.category" placeholder="请选择分类" filterable>
            <el-option label="数据分析" value="data" />
            <el-option label="报告生成" value="report" />
            <el-option label="文档处理" value="document" />
            <el-option label="研发效能" value="code" />
            <el-option label="信息检索" value="research" />
            <el-option label="文件处理" value="file" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingPackage" @click="handleCreatePackage">创建</el-button>
      </template>
    </el-dialog>

    <div v-if="editorVisible" class="skill-editor-overlay">
      <aside class="skill-file-pane">
        <header class="file-pane-head">
          <el-tooltip content="返回技能管理" placement="bottom">
            <button class="editor-icon-button" type="button" aria-label="返回技能管理" @click="handleEditorBack">
              <el-icon><Back /></el-icon>
            </button>
          </el-tooltip>
          <strong>{{ editorSkill?.skillName }}</strong>
          <div class="file-tools">
            <el-tooltip content="新建文件" placement="bottom">
              <button class="editor-icon-button" type="button" aria-label="新建文件" @click="createEditorFile">
                <el-icon><DocumentAdd /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="新建文件夹" placement="bottom">
              <button class="editor-icon-button" type="button" aria-label="新建文件夹" @click="createEditorFolder">
                <el-icon><FolderAdd /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="下载当前文件" placement="bottom">
              <button class="editor-icon-button" type="button" aria-label="下载当前文件" @click="downloadActiveEditorFile">
                <el-icon><Download /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </header>

        <div class="skill-file-tree">
          <div
            v-for="node in editorTree"
            :key="node.id"
            class="tree-node-group"
          >
            <button
              class="tree-node"
              :class="{ active: node.id === activeFileId, folder: node.type === 'folder' }"
              type="button"
              @click="selectEditorFile(node)"
            >
              <span class="file-type-icon">
                <el-icon v-if="node.type === 'folder'"><FolderOpened /></el-icon>
                <el-icon v-else><Document /></el-icon>
              </span>
              <span>{{ node.name }}</span>
              <el-icon v-if="node.id === activeFileId"><CircleCheck /></el-icon>
            </button>
            <div v-if="node.children?.length" class="tree-children">
              <button
                v-for="child in node.children"
                :key="child.id"
                class="tree-node"
                :class="{ active: child.id === activeFileId }"
                type="button"
                @click="selectEditorFile(child)"
              >
                <span class="file-type-icon">
                  <el-icon><Document /></el-icon>
                </span>
                <span>{{ child.name }}</span>
                <el-icon v-if="child.id === activeFileId"><CircleCheck /></el-icon>
              </button>
            </div>
          </div>
        </div>

        <button class="delete-package-button" type="button" @click="handleDeleteEditorSkill">
          <el-icon><Delete /></el-icon>
          删除技能包
        </button>
      </aside>

      <section class="skill-editor-pane">
        <header class="editor-topbar">
          <div>
            <h3>{{ activeFile?.name || 'SKILL.md' }}</h3>
            <span v-if="editorDirty">未保存更改</span>
          </div>
          <div class="editor-actions">
            <el-button :icon="Back" @click="discardEditorChanges">放弃更改</el-button>
            <el-button type="primary" :icon="Finished" :loading="editorSaving" @click="handleSaveEditor">保存</el-button>
          </div>
        </header>
        <div class="code-editor-shell">
          <pre class="line-numbers"><span v-for="line in editorLines" :key="line">{{ line }}</span></pre>
          <textarea
            v-model="editorContent"
            class="skill-code-editor"
            spellcheck="false"
            @input="handleEditorInput"
          />
        </div>
      </section>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="780px"
      destroy-on-close
      class="skill-dialog"
    >
      <el-form label-width="116px" class="skill-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="form.skillName" placeholder="如：SQL 数据分析" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="唯一编码" required>
              <el-input v-model="form.skillKey" placeholder="如：sql-analysis" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.category" filterable allow-create>
                <el-option label="数据分析" value="data" />
                <el-option label="报告生成" value="report" />
                <el-option label="文档处理" value="document" />
                <el-option label="研发效能" value="code" />
                <el-option label="信息检索" value="research" />
                <el-option label="文件处理" value="file" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel">
                <el-option label="低风险" value="LOW" />
                <el-option label="中风险" value="MEDIUM" />
                <el-option label="高风险" value="HIGH" />
                <el-option label="关键风险" value="CRITICAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作用域">
              <el-select v-model="form.scopeType">
                <el-option label="全局" value="GLOBAL" />
                <el-option label="租户" value="TENANT" />
                <el-option label="用户" value="USER" />
                <el-option label="智能体" value="AGENT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="执行要求">
              <div class="switch-row">
                <el-checkbox v-model="form.requiresShell" :true-value="1" :false-value="0">需要 Shell/脚本</el-checkbox>
                <el-checkbox v-model="form.requiresSandbox" :true-value="1" :false-value="0">必须使用沙箱</el-checkbox>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述技能能力、适用场景和输出规范" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="SKILL.md">
              <el-input v-model="form.skillMdContent" type="textarea" :rows="5" placeholder="可填写完整 SKILL.md 内容或关键执行规范" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.skill-console {
  display: grid;
  min-height: calc(100vh - 115px);
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 18px;
  padding-bottom: 28px;
}

.skill-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.skill-hero h2 {
  margin: 14px 0 8px;
  color: #071f40;
  font-size: 34px;
  font-weight: 850;
  letter-spacing: 0;
}

.skill-hero p {
  margin: 0;
  color: #526b87;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

.skill-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.skill-metric,
.skill-list-panel,
.side-panel {
  border: 1px solid #d7e5f8;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 38px rgba(48, 94, 151, 0.08);
}

.skill-metric {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 10px 22px;
}

.metric-icon {
  display: grid;
  width: 58px;
  height: 58px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 13px;
  color: #2f75ff;
  background: #ecf4ff;
  font-size: 28px;
}

.metric-icon.cyan {
  color: #0b95d8;
  background: #e9f8ff;
}

.metric-icon.indigo {
  color: #5c6cff;
  background: #eef1ff;
}

.metric-icon.green {
  color: #168354;
  background: #eaf8ef;
}

.skill-metric > div > span {
  display: block;
  color: #667d99;
  font-size: 13px;
}

.skill-metric strong {
  display: block;
  margin-top: 8px;
  color: #0a2547;
  font-size: 30px;
  font-weight: 850;
  line-height: 1;
}

.skill-metric small {
  display: block;
  margin-top: 12px;
  color: #6d819b;
  font-size: 12px;
  font-weight: 750;
}

.skill-metric small.positive {
  color: #22a86b;
}

.skill-dashboard {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(760px, 1fr) minmax(330px, 0.52fr);
  align-self: stretch;
  align-items: stretch;
  gap: 18px;
  min-height: 0;
}

.skill-list-panel {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-color: #d9e4f2;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(34, 67, 112, 0.06);
}

.panel-head,
.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel-head {
  padding: 18px 18px 24px;
  border-bottom: 0;
}

.panel-head h3,
.side-head h3 {
  margin: 0;
  color: #0a2547;
  font-size: 17px;
  font-weight: 800;
}

.panel-head p {
  margin: 6px 0 0;
  color: #6d819b;
  font-size: 12px;
}

.skill-filter-bar {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.skill-filter-bar .el-input {
  width: 250px;
}

.skill-filter-bar .el-select {
  width: 128px;
}

.skill-filter-bar .el-button {
  height: 34px;
  border-radius: 5px;
  font-weight: 800;
}

.view-toggle {
  flex: 0 0 auto;
}

.skill-filter-bar :deep(.el-input__wrapper),
.skill-filter-bar :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 5px;
  box-shadow: 0 0 0 1px #d7e1ee inset;
}

.skill-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px 20px;
  padding: 0 18px 18px;
}

.skill-list.list {
  grid-template-columns: 1fr;
}

.skill-card {
  position: relative;
  display: grid;
  min-height: 160px;
  grid-template-rows: auto 78px auto;
  padding: 18px;
  border: 1px solid #e0eaf6;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(42, 72, 108, 0.05);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.skill-card:hover {
  border-color: #a9c9ff;
  box-shadow: 0 12px 28px rgba(47, 117, 255, 0.08);
  transform: translateY(-1px);
}

.skill-card-head {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 14px;
}

.skill-icon {
  display: grid;
  width: 54px;
  height: 54px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 13px;
  color: #2f75ff;
  background: #ecf4ff;
  font-size: 28px;
}

.skill-icon.green {
  color: #13a66f;
  background: #e9f9f0;
}

.skill-icon.orange {
  color: #e18a12;
  background: #fff4e2;
}

.skill-icon.violet {
  color: #7c5cff;
  background: #f1edff;
}

.skill-main {
  min-width: 0;
  padding-right: 88px;
}

.skill-title-line {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
}

.skill-title-line h4 {
  min-width: 0;
  overflow: hidden;
  margin: 0;
  color: #0a2547;
  font-size: 15px;
  font-weight: 820;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.skill-card:hover .skill-title-line h4 {
  color: #0b63f6;
}

.skill-title-line span {
  flex: 0 0 auto;
  padding: 3px 7px;
  border-radius: 6px;
  color: #2f75ff;
  background: #eaf2ff;
  font-size: 11px;
  font-weight: 800;
}

.skill-main p {
  display: -webkit-box;
  overflow: hidden;
  margin: 10px 0 0;
  color: #6d819b;
  font-size: 12px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.skill-status {
  position: absolute;
  top: 18px;
  right: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 62px;
  height: 24px;
  border: 1px solid #bce8cc;
  border-radius: 7px;
  color: #168354;
  background: #eaf8ef;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.skill-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.skill-status.disabled {
  border-color: #d9e2ec;
  color: #6d819b;
  background: #f4f7fb;
}

.skill-card-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  align-self: end;
  border: 1px solid #e1ebf6;
  border-radius: 8px;
  background: #f8fbff;
}

.skill-card-stats div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border-right: 1px solid #e1ebf6;
}

.skill-card-stats div:last-child {
  border-right: 0;
}

.skill-card-stats span {
  color: #7e94ad;
  font-size: 12px;
}

.skill-card-stats strong {
  color: #203957;
  font-size: 15px;
}

.skill-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid #e5edf6;
}

.skill-card-actions > span {
  color: #6d819b;
  font-size: 13px;
  font-weight: 750;
}

.skill-card-actions nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.skill-card-actions .el-button.is-link {
  height: 32px;
  padding: 0;
  font-weight: 800;
}

.more-button {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 0;
  border-radius: 8px;
  color: #405874;
  background: transparent;
  cursor: pointer;
}

.more-button:hover {
  background: #edf4ff;
  color: #2f75ff;
}

.skill-list-footer {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 18px 16px;
  color: #53637b;
  font-size: 14px;
  font-weight: 700;
}

.skill-list-footer :deep(.el-pagination) {
  --el-pagination-button-bg-color: #ffffff;
  --el-pagination-hover-color: #0b63f6;
}

.skill-list-footer :deep(.el-pager li),
.skill-list-footer :deep(.btn-prev),
.skill-list-footer :deep(.btn-next) {
  border: 1px solid #d9e4f2;
  border-radius: 5px;
  box-shadow: none;
}

.skill-list-footer :deep(.el-pager li.is-active) {
  border-color: #0b63f6;
  color: #0b63f6;
  background: #ffffff;
}

.skill-side {
  display: grid;
  grid-template-rows: minmax(360px, 0.58fr) minmax(250px, 0.42fr);
  gap: 18px;
  min-height: 0;
}

.side-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding: 18px;
  border-radius: 8px;
}

.side-head .el-button {
  height: auto;
  padding: 0;
  font-weight: 800;
}

.execution-timeline,
.exception-list {
  display: grid;
  flex: 1 1 auto;
  align-content: start;
  min-height: 0;
  margin-top: 18px;
  overflow-y: auto;
  padding-right: 4px;
}

.execution-row {
  position: relative;
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) 72px 54px;
  align-items: center;
  gap: 10px;
  min-height: 76px;
}

.execution-row::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 5px;
  width: 1px;
  background: #d9e8fb;
  content: '';
}

.execution-row > span {
  z-index: 1;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #1f78ff;
  box-shadow: 0 0 0 4px #eaf3ff;
}

.execution-row strong,
.exception-row strong {
  overflow: hidden;
  color: #0a2547;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.execution-row small,
.exception-row small {
  display: block;
  overflow: hidden;
  margin-top: 7px;
  color: #7890aa;
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.execution-row time,
.exception-row time {
  color: #6d819b;
  font-size: 12px;
  font-weight: 750;
}

.exception-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px 54px;
  align-items: center;
  gap: 10px;
  min-height: 70px;
  border-bottom: 1px solid #e3edf8;
}

.exception-row:last-child {
  border-bottom: 0;
}

.skill-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 22px;
  border-bottom: 1px solid #dce8f5;
}

.skill-dialog :deep(.el-dialog__body) {
  padding: 20px 22px;
}

.skill-form .el-select,
.skill-form .el-input,
.skill-form :deep(.el-textarea) {
  width: 100%;
}

.switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
}

.skill-create-dialog :deep(.el-dialog) {
  border-radius: 10px;
  box-shadow: 0 24px 56px rgba(18, 35, 58, 0.18);
}

.skill-create-dialog :deep(.el-dialog__header) {
  padding: 28px 34px 6px;
}

.skill-create-dialog :deep(.el-dialog__body) {
  padding: 18px 34px 8px;
}

.skill-create-dialog :deep(.el-dialog__footer) {
  padding: 10px 34px 28px;
}

.skill-create-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #1472ff;
  font-size: 17px;
}

.skill-create-title span {
  display: grid;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: #f8a90b;
  font-size: 16px;
}

.skill-create-title strong {
  font-weight: 850;
}

.skill-package-form :deep(.el-form-item__label) {
  color: #293d55;
  font-weight: 750;
}

.skill-package-form .el-select,
.skill-package-form .el-input,
.skill-package-form :deep(.el-textarea) {
  width: 100%;
}

.skill-package-form :deep(.el-input__wrapper),
.skill-package-form :deep(.el-select__wrapper),
.skill-package-form :deep(.el-textarea__inner) {
  border-radius: 7px;
  background: #f2f5f9;
  box-shadow: none;
}

.skill-package-form :deep(.el-input__wrapper),
.skill-package-form :deep(.el-select__wrapper) {
  min-height: 44px;
}

.skill-editor-overlay {
  position: fixed;
  z-index: 1200;
  top: 75px;
  right: 0;
  bottom: 0;
  left: 246px;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  overflow: hidden;
  border-top: 1px solid #e1e8f2;
  background: #ffffff;
  box-shadow: 0 -1px 0 rgba(11, 33, 64, 0.04);
}

.skill-file-pane {
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-rows: 58px minmax(0, 1fr) 58px;
  border-right: 1px solid #e1e8f2;
  background: #ffffff;
}

.file-pane-head,
.editor-topbar {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e7edf5;
}

.file-pane-head {
  gap: 10px;
  padding: 0 14px;
}

.file-pane-head > strong {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: #122842;
  font-size: 16px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-tools {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
}

.editor-icon-button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 0;
  border-radius: 6px;
  color: #172b43;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
}

.editor-icon-button:hover {
  color: #0b63f6;
  background: #edf4ff;
}

.skill-file-tree {
  min-height: 0;
  overflow-y: auto;
  padding: 12px 10px;
}

.tree-node-group {
  display: grid;
  gap: 4px;
}

.tree-node {
  display: grid;
  width: 100%;
  min-width: 0;
  height: 38px;
  grid-template-columns: 24px minmax(0, 1fr) 22px;
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: 6px;
  color: #15283f;
  background: transparent;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.tree-node span:nth-child(2) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node:hover,
.tree-node.active {
  background: #edf4ff;
  color: #0b63f6;
}

.tree-node.folder {
  color: #536a85;
}

.file-type-icon {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  color: #1484d8;
}

.tree-children {
  display: grid;
  gap: 4px;
  padding-left: 18px;
}

.delete-package-button {
  display: flex;
  height: 58px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-top: 1px solid #e7edf5;
  color: #ff4242;
  background: #ffffff;
  cursor: pointer;
  font: inherit;
  font-weight: 750;
}

.delete-package-button:hover {
  background: #fff4f4;
}

.skill-editor-pane {
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-rows: 58px minmax(0, 1fr);
  background: #ffffff;
}

.editor-topbar {
  gap: 16px;
  padding: 0 22px;
}

.editor-topbar h3 {
  margin: 0;
  color: #30445d;
  font-size: 16px;
  font-weight: 760;
}

.editor-topbar span {
  display: block;
  margin-top: 3px;
  color: #e18a12;
  font-size: 12px;
  font-weight: 750;
}

.editor-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

.code-editor-shell {
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-columns: 64px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #e0e7f0;
  border-top: 0;
  background: #ffffff;
}

.line-numbers {
  display: flex;
  overflow: hidden;
  flex-direction: column;
  margin: 0;
  padding: 18px 14px;
  border-right: 1px solid #dde6f0;
  color: #8493a6;
  background: #f5f7fa;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 24px;
  text-align: right;
  user-select: none;
}

.skill-code-editor {
  width: 100%;
  height: 100%;
  min-height: 0;
  resize: none;
  border: 0;
  outline: 0;
  padding: 18px 22px 18px 10px;
  color: #1f2d3d;
  background: #ffffff;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 15px;
  line-height: 24px;
  tab-size: 2;
}

@media (max-width: 1320px) {
  .skill-dashboard {
    grid-template-columns: 1fr;
  }

  .skill-dashboard,
  .skill-side {
    min-height: 0;
  }

  .skill-side {
    grid-template-rows: none;
  }

  .panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .skill-filter-bar {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}

@media (max-width: 980px) {
  .skill-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .skill-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .skill-filter-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .skill-filter-bar .el-input,
  .skill-filter-bar .el-select,
  .skill-filter-bar .el-button,
  .view-toggle {
    width: 100%;
  }

  .view-toggle {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .execution-row,
  .exception-row {
    grid-template-columns: 1fr;
  }

  .execution-row > span,
  .execution-row::before {
    display: none;
  }
}

@media (max-width: 640px) {
  .skill-hero h2 {
    font-size: 28px;
  }

  .hero-actions,
  .hero-actions .el-button {
    width: 100%;
  }

  .skill-metrics {
    grid-template-columns: 1fr;
  }

  .skill-list {
    grid-template-columns: 1fr;
    padding: 0 12px 14px;
  }

  .skill-card-head {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .skill-icon {
    width: 48px;
    height: 48px;
  }

  .skill-main {
    padding-right: 0;
  }

  .skill-status {
    position: static;
    grid-column: 1 / -1;
    justify-self: start;
  }

  .skill-card-stats {
    grid-template-columns: 1fr;
  }

  .skill-card-stats div {
    border-right: 0;
    border-bottom: 1px solid #e4edf7;
  }

  .skill-card-stats div:last-child {
    border-bottom: 0;
  }

  .skill-card-actions,
  .skill-list-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
