<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
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
  Upload,
  User,
  WarningFilled
} from '@element-plus/icons-vue'
import {
  createSkillPackageNode,
  createSkill,
  deleteSkillPackageNode,
  deleteSkill,
  getSkillFileContent,
  listSkillFilesBySkill,
  listSkillLogs,
  listSkills,
  updateSkillPackageFile,
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
const skills = ref([])
const logs = ref([])
const currentPage = ref(1)
const pageSize = ref(6)
const editorSkill = ref(null)
const editorTree = ref([])
const editorSnapshot = ref([])
const activeFileId = ref('')
const editorContent = ref('')
const nodeDialogVisible = ref(false)
const nodeDialogType = ref('file')
const nodeDialogSaving = ref(false)
const nodeDialogParent = ref(null)
const uploadInputRef = ref(null)
const uploadParentNode = ref(null)
const treeProps = {
  children: 'children',
  label: 'name'
}

const nodeForm = reactive({
  name: '',
  parentPath: ''
})

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null
})

let editorScrollLockState = null

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
]

const demoLogs = [
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

const nodeDialogTitle = computed(() => {
  return nodeDialogType.value === 'folder' ? '新建文件夹' : '新建文件'
})

const nodeDialogNameLabel = computed(() => {
  return nodeDialogType.value === 'folder' ? '文件夹名' : '文件名'
})

const nodeDialogPlaceholder = computed(() => {
  return nodeDialogType.value === 'folder' ? '例如：utils' : '例如：helper.py'
})

const contextMenuActions = computed(() => {
  if (!contextMenu.node) {
    return []
  }

  if (contextMenu.node.type === 'folder') {
    return [
      { command: 'new-file', label: '新建文件', icon: DocumentAdd },
      { command: 'new-folder', label: '新建文件夹', icon: FolderAdd },
      { command: 'upload', label: '上传文件', icon: Upload },
      { command: 'delete', label: '删除文件夹', icon: Delete, danger: true }
    ]
  }

  return [
    { command: 'delete', label: '删除文件', icon: Delete, danger: true }
  ]
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

watch(editorVisible, (visible) => {
  setEditorScrollLock(visible)
})

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

async function openSkillEditor(skill) {
  const normalizedSkill = normalizeSkill(skill, 0)
  editorSkill.value = normalizedSkill
  editorTree.value = buildEditorTree(normalizedSkill)
  activeFileId.value = 'skill-md'
  editorContent.value = findTreeNode(editorTree.value, activeFileId.value)?.content || ''
  editorSnapshot.value = cloneTree(editorTree.value)
  editorDirty.value = false
  editorVisible.value = true
  await loadSkillFilesForEditor(normalizedSkill)
}

function handleEditorInput() {
  editorDirty.value = true
}

async function selectEditorFile(node) {
  if (!node || node.type !== 'file' || node.id === activeFileId.value) {
    return
  }

  persistActiveFile()
  activeFileId.value = node.id
  editorContent.value = node.content || ''
  await ensureFileContent(node)
  editorContent.value = node.content || ''
}

function createEditorFile(parentNode = null) {
  persistActiveFile()
  openNodeDialog('file', parentNode)
}

function createEditorFolder(parentNode = null) {
  openNodeDialog('folder', parentNode)
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
  const file = activeFile.value
  const skillFile = findTreeNode(editorTree.value, 'skill-md')
  if (!skill) {
    return
  }

  editorSaving.value = true
  try {
    if (isSkillMdNode(file)) {
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
    } else if (file?.skillFileId) {
      const updatedFile = await updateSkillPackageFile({
        id: normalizeId(file.skillFileId),
        content: editorContent.value
      })
      mergeSkillFileNode(file, updatedFile)
    } else {
      ElMessage.warning('请先创建文件记录后再保存')
      return
    }

    editorSnapshot.value = cloneTree(editorTree.value)
    editorDirty.value = false
    ElMessage.success('文件已保存')
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

function openNodeDialog(type, parentNode = null) {
  const parent = parentNode?.type === 'folder' ? parentNode : null
  nodeDialogType.value = type
  nodeDialogParent.value = parent
  nodeForm.parentPath = parent?.relativePath || ''
  nodeForm.name = ''
  nodeDialogVisible.value = true
  hideTreeContextMenu()
}

function applyQuickFolderName(name) {
  nodeForm.name = name
}

async function handleCreateNode() {
  const skill = editorSkill.value
  const name = nodeForm.name.trim()
  if (!skill?.id) {
    ElMessage.warning('技能包信息不存在')
    return
  }

  if (!name) {
    ElMessage.warning(`请输入${nodeDialogNameLabel.value}`)
    return
  }

  if (name.includes('/') || name.includes('\\')) {
    ElMessage.warning('名称不能包含路径分隔符')
    return
  }

  if (hasSiblingName(nodeDialogParent.value, name)) {
    ElMessage.warning('同级目录下已存在同名文件或文件夹')
    return
  }

  nodeDialogSaving.value = true
  try {
    const isFolder = nodeDialogType.value === 'folder'
    const content = isFolder ? '' : createDefaultFileContent(name)
    const created = await createSkillPackageNode({
      skillId: normalizeId(skill.id),
      parentPath: nodeForm.parentPath,
      fileName: name,
      directory: isFolder,
      content
    })
    const node = skillFileToTreeNode(created, skill)
    if (!isFolder) {
      node.content = content
      node.loaded = true
    }
    insertTreeNode(node, nodeDialogParent.value)
    sortTreeNodes(editorTree.value)
    editorSnapshot.value = cloneTree(editorTree.value)
    nodeDialogVisible.value = false
    ElMessage.success(isFolder ? '文件夹已创建' : '文件已创建')

    if (node.type === 'file') {
      activeFileId.value = node.id
      editorContent.value = node.content || ''
    }
  } finally {
    nodeDialogSaving.value = false
  }
}

function openTreeContextMenu(event, data) {
  event.preventDefault()
  event.stopPropagation()
  contextMenu.node = data
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.visible = true
}

function hideTreeContextMenu() {
  contextMenu.visible = false
}

function handleContextAction(command) {
  const node = contextMenu.node
  hideTreeContextMenu()
  if (!node) {
    return
  }

  if (command === 'new-file') {
    createEditorFile(node)
    return
  }

  if (command === 'new-folder') {
    createEditorFolder(node)
    return
  }

  if (command === 'upload') {
    triggerUploadFile(node)
    return
  }

  if (command === 'delete') {
    deleteEditorNode(node)
  }
}

function triggerUploadFile(parentNode = null) {
  uploadParentNode.value = parentNode?.type === 'folder' ? parentNode : null
  if (uploadInputRef.value) {
    uploadInputRef.value.value = ''
    uploadInputRef.value.click()
  }
}

async function handleUploadFile(event) {
  const file = event.target.files?.[0]
  const skill = editorSkill.value
  const parent = uploadParentNode.value
  if (!file || !skill?.id) {
    return
  }

  if (hasSiblingName(parent, file.name)) {
    ElMessage.warning('同级目录下已存在同名文件或文件夹')
    return
  }

  const content = await file.text()
  const created = await createSkillPackageNode({
    skillId: normalizeId(skill.id),
    parentPath: parent?.relativePath || '',
    fileName: file.name,
    directory: false,
    content
  })
  const node = skillFileToTreeNode(created, skill)
  node.content = content
  node.loaded = true
  insertTreeNode(node, parent)
  sortTreeNodes(editorTree.value)
  editorSnapshot.value = cloneTree(editorTree.value)
  activeFileId.value = node.id
  editorContent.value = node.content || ''
  ElMessage.success('文件已上传')
}

async function deleteEditorNode(node) {
  if (!node) {
    return
  }

  if (isSkillMdNode(node)) {
    ElMessage.warning('SKILL.md 是技能入口文件，不能删除')
    return
  }

  const label = node.type === 'folder' ? '文件夹' : '文件'
  try {
    await ElMessageBox.confirm(`确认删除${label}「${node.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const removedIds = new Set(flattenTree([node]).map((item) => item.id))
  if (node.skillFileId) {
    await deleteSkillPackageNode(node.skillFileId)
  } else {
    const persistedChildren = flattenTree(node.children)
      .filter((item) => item.skillFileId && !isSkillMdNode(item))
      .sort((left, right) => String(right.relativePath || '').length - String(left.relativePath || '').length)
    for (const item of persistedChildren) {
      await deleteSkillPackageNode(item.skillFileId)
    }
  }
  removeTreeNode(editorTree.value, node.id)
  if (removedIds.has(activeFileId.value)) {
    const fallback = findFirstFile(editorTree.value)
    activeFileId.value = ''
    if (fallback) {
      await selectEditorFile(fallback)
    } else {
      editorContent.value = ''
    }
  }
  editorSnapshot.value = cloneTree(editorTree.value)
  editorDirty.value = false
  ElMessage.success(`${label}已删除`)
}

function closeSkillEditor() {
  editorVisible.value = false
  editorSkill.value = null
  editorTree.value = []
  editorSnapshot.value = []
  activeFileId.value = ''
  editorContent.value = ''
  editorDirty.value = false
  nodeDialogVisible.value = false
  nodeDialogParent.value = null
  uploadParentNode.value = null
  hideTreeContextMenu()
}

function setEditorScrollLock(locked) {
  if (typeof document === 'undefined') {
    return
  }

  const appMain = document.querySelector('.app-main')
  if (locked) {
    if (editorScrollLockState) {
      return
    }
    editorScrollLockState = {
      bodyOverflow: document.body.style.overflow,
      htmlOverflow: document.documentElement.style.overflow,
      appMain,
      appMainOverflow: appMain?.style.overflow || ''
    }
    document.body.style.overflow = 'hidden'
    document.documentElement.style.overflow = 'hidden'
    if (appMain) {
      appMain.style.overflow = 'hidden'
    }
    return
  }

  if (!editorScrollLockState) {
    return
  }
  document.body.style.overflow = editorScrollLockState.bodyOverflow
  document.documentElement.style.overflow = editorScrollLockState.htmlOverflow
  if (editorScrollLockState.appMain) {
    editorScrollLockState.appMain.style.overflow = editorScrollLockState.appMainOverflow
  }
  editorScrollLockState = null
}

function handleEditorWheel(event) {
  event.stopPropagation()
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

async function loadSkillFilesForEditor(skill) {
  if (!skill?.id) {
    return
  }

  try {
    const files = await listSkillFilesBySkill(skill.id)
    if (!Array.isArray(files) || !files.length) {
      return
    }
    editorTree.value = buildEditorTree(skill, files)
    const nextActive = findTreeNode(editorTree.value, 'skill-md') || findFirstFile(editorTree.value)
    activeFileId.value = nextActive?.id || ''
    if (nextActive) {
      await ensureFileContent(nextActive)
      editorContent.value = nextActive.content || ''
    }
    editorSnapshot.value = cloneTree(editorTree.value)
    editorDirty.value = false
  } catch {
    editorSnapshot.value = cloneTree(editorTree.value)
  }
}

function buildEditorTree(skill, files = []) {
  const root = []
  const pathMap = new Map()
  const sortedFiles = [...files].sort((left, right) => {
    const leftPath = String(left.relativePath || left.fileName || '')
    const rightPath = String(right.relativePath || right.fileName || '')
    const leftDepth = leftPath.split('/').length
    const rightDepth = rightPath.split('/').length
    return leftDepth - rightDepth || leftPath.localeCompare(rightPath)
  })

  sortedFiles.forEach((file) => {
    const node = skillFileToTreeNode(file, skill)
    addNodeByPath(root, pathMap, node)
  })

  if (!pathMap.has('SKILL.md')) {
    addNodeByPath(root, pathMap, {
      id: 'skill-md',
      name: 'SKILL.md',
      type: 'file',
      relativePath: 'SKILL.md',
      fileRole: 'SKILL_MD',
      content: skill.skillMdContent || createDefaultSkillContent(skill),
      loaded: true,
      children: []
    })
  }

  sortTreeNodes(root)
  return root
}

function skillFileToTreeNode(file, skill) {
  const relativePath = normalizeTreePath(file?.relativePath || file?.fileName || 'SKILL.md')
  const isDirectory = file?.fileRole === 'DIRECTORY' || file?.mimeType === 'inode/directory' || file?.directory === true
  const isSkillMd = file?.fileRole === 'SKILL_MD' || relativePath === 'SKILL.md'
  return {
    id: isSkillMd ? 'skill-md' : `${isDirectory ? 'folder' : 'file'}-${file?.id || relativePath}`,
    skillFileId: file?.id || null,
    workspaceFileId: file?.workspaceFileId || null,
    storageKey: file?.storageKey || '',
    name: file?.fileName || pathBasename(relativePath),
    type: isDirectory ? 'folder' : 'file',
    relativePath,
    fileRole: isDirectory ? 'DIRECTORY' : (file?.fileRole || (isSkillMd ? 'SKILL_MD' : 'ASSET')),
    content: isSkillMd ? (skill.skillMdContent || createDefaultSkillContent(skill)) : (file?.content ?? ''),
    loaded: isSkillMd || Boolean(file?.content),
    children: []
  }
}

function mergeSkillFileNode(node, file) {
  const children = node.children || []
  const next = skillFileToTreeNode(file, editorSkill.value || {})
  Object.assign(node, {
    skillFileId: next.skillFileId,
    workspaceFileId: next.workspaceFileId,
    storageKey: next.storageKey,
    relativePath: next.relativePath,
    fileRole: next.fileRole,
    name: next.name,
    content: editorContent.value,
    loaded: true,
    children
  })
}

function addNodeByPath(root, pathMap, node) {
  const existing = pathMap.get(node.relativePath)
  if (existing) {
    const children = existing.children || []
    Object.assign(existing, node, { children })
    return existing
  }

  const parentPath = pathParent(node.relativePath)
  const parent = parentPath ? ensureFolderNode(root, pathMap, parentPath) : null
  if (parent) {
    parent.children = parent.children || []
    parent.children.push(node)
  } else {
    root.push(node)
  }
  pathMap.set(node.relativePath, node)
  return node
}

function ensureFolderNode(root, pathMap, folderPath) {
  const normalizedPath = normalizeTreePath(folderPath)
  const existing = pathMap.get(normalizedPath)
  if (existing) {
    return existing
  }

  const node = {
    id: `folder-virtual-${normalizedPath}`,
    skillFileId: null,
    name: pathBasename(normalizedPath),
    type: 'folder',
    relativePath: normalizedPath,
    fileRole: 'DIRECTORY',
    loaded: true,
    virtual: true,
    children: []
  }
  const parentPath = pathParent(normalizedPath)
  const parent = parentPath ? ensureFolderNode(root, pathMap, parentPath) : null
  if (parent) {
    parent.children.push(node)
  } else {
    root.push(node)
  }
  pathMap.set(normalizedPath, node)
  return node
}

function normalizeTreePath(path) {
  return String(path || '')
    .replace(/\\/g, '/')
    .replace(/^\/+/, '')
    .replace(/\/+$/, '')
}

function pathParent(path) {
  const normalizedPath = normalizeTreePath(path)
  const index = normalizedPath.lastIndexOf('/')
  return index > 0 ? normalizedPath.slice(0, index) : ''
}

function pathBasename(path) {
  const normalizedPath = normalizeTreePath(path)
  return normalizedPath.split('/').filter(Boolean).pop() || normalizedPath
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

function createDefaultFileContent(name) {
  if (String(name || '').toLowerCase().endsWith('.md')) {
    const title = StringUtilsTitle(name)
    return `# ${title}\n`
  }
  return ''
}

function StringUtilsTitle(name) {
  return String(name || '新建文件').replace(/\.[^.]+$/, '').replace(/[-_]+/g, ' ')
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

async function ensureFileContent(node) {
  if (!node || node.type !== 'file' || node.loaded || !node.skillFileId || isSkillMdNode(node)) {
    return
  }

  node.loading = true
  try {
    const content = await getSkillFileContent(node.skillFileId)
    node.content = content || ''
    node.loaded = true
  } finally {
    node.loading = false
  }
}

function isSkillMdNode(node) {
  return node?.id === 'skill-md' || node?.fileRole === 'SKILL_MD' || node?.relativePath === 'SKILL.md'
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

function findFirstFile(nodes) {
  for (const node of nodes || []) {
    if (node.type === 'file') {
      return node
    }
    const child = findFirstFile(node.children)
    if (child) {
      return child
    }
  }
  return null
}

function insertTreeNode(node, parentNode) {
  if (parentNode?.type === 'folder') {
    parentNode.children = parentNode.children || []
    parentNode.children.push(node)
    return
  }
  editorTree.value.push(node)
}

function removeTreeNode(nodes, id) {
  const index = (nodes || []).findIndex((node) => node.id === id)
  if (index > -1) {
    nodes.splice(index, 1)
    return true
  }

  for (const node of nodes || []) {
    if (removeTreeNode(node.children || [], id)) {
      return true
    }
  }
  return false
}

function hasSiblingName(parentNode, name) {
  const siblings = parentNode?.type === 'folder' ? parentNode.children || [] : editorTree.value
  return siblings.some((node) => node.name === name)
}

function sortTreeNodes(nodes) {
  ;(nodes || []).sort((left, right) => {
    if (left.type !== right.type) {
      return left.type === 'folder' ? -1 : 1
    }
    return left.name.localeCompare(right.name)
  })
  ;(nodes || []).forEach((node) => sortTreeNodes(node.children))
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

onMounted(() => {
  loadDashboard()
  document.addEventListener('click', hideTreeContextMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', hideTreeContextMenu)
  setEditorScrollLock(false)
})
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

    <div v-if="editorVisible" class="skill-editor-overlay" @wheel="handleEditorWheel">
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
          <el-tree
            class="editor-el-tree"
            :data="editorTree"
            :props="treeProps"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="selectEditorFile"
            @node-contextmenu="openTreeContextMenu"
          >
            <template #default="{ data }">
              <div class="tree-node-content" :class="{ active: data.id === activeFileId, folder: data.type === 'folder' }">
                <span class="file-type-icon">
                  <el-icon v-if="data.type === 'folder'"><FolderOpened /></el-icon>
                  <el-icon v-else><Document /></el-icon>
                </span>
                <span class="tree-node-label">{{ data.name }}</span>
                <el-icon v-if="data.id === activeFileId" class="tree-active-icon"><CircleCheck /></el-icon>
              </div>
            </template>
          </el-tree>
        </div>

        <input
          ref="uploadInputRef"
          class="hidden-upload-input"
          type="file"
          @change="handleUploadFile"
        />

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

      <div
        v-if="contextMenu.visible"
        class="tree-context-menu"
        :style="{ left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }"
        @click.stop
        @contextmenu.prevent
      >
        <button
          v-for="action in contextMenuActions"
          :key="action.command"
          type="button"
          :class="{ danger: action.danger }"
          @click="handleContextAction(action.command)"
        >
          <el-icon><component :is="action.icon" /></el-icon>
          <span>{{ action.label }}</span>
        </button>
      </div>

      <el-dialog
        v-model="nodeDialogVisible"
        :title="nodeDialogTitle"
        width="520px"
        destroy-on-close
        append-to-body
        class="skill-node-dialog"
      >
        <el-form label-position="top" class="skill-node-form">
          <el-form-item label="父目录">
            <el-input :model-value="nodeForm.parentPath || '(根目录)'" disabled />
          </el-form-item>
          <el-form-item :label="nodeDialogNameLabel" required>
            <div v-if="nodeDialogType === 'folder'" class="quick-folder-row">
              <span>快捷选择：</span>
              <button type="button" @click="applyQuickFolderName('scripts')">scripts</button>
              <button type="button" @click="applyQuickFolderName('references')">references</button>
              <button type="button" @click="applyQuickFolderName('examples')">examples</button>
            </div>
            <el-input v-model="nodeForm.name" :placeholder="nodeDialogPlaceholder" @keyup.enter="handleCreateNode" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="nodeDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="nodeDialogSaving" @click="handleCreateNode">确定</el-button>
        </template>
      </el-dialog>
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

.editor-el-tree {
  --el-tree-node-hover-bg-color: #edf4ff;
  color: #15283f;
  background: transparent;
}

.editor-el-tree :deep(.el-tree-node__content) {
  height: 38px;
  border-radius: 6px;
}

.editor-el-tree :deep(.el-tree-node__expand-icon) {
  color: #8a9aaf;
}

.editor-el-tree :deep(.is-current > .el-tree-node__content) {
  background: #edf4ff;
}

.tree-node-content {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: 24px minmax(0, 1fr) 20px;
  align-items: center;
  gap: 8px;
}

.tree-node-content.folder {
  color: #536a85;
}

.tree-node-content.active {
  color: #0b63f6;
}

.file-type-icon {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  color: #1484d8;
}

.tree-node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-active-icon {
  color: #a8b5c6;
}

.hidden-upload-input {
  display: none;
}

.tree-context-menu {
  position: fixed;
  z-index: 2400;
  display: grid;
  min-width: 156px;
  gap: 4px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(16, 38, 68, 0.16);
}

.tree-context-menu button {
  display: grid;
  height: 36px;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: 5px;
  color: #2f3c4d;
  background: transparent;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.tree-context-menu button:hover {
  color: #0b63f6;
  background: #edf4ff;
}

.tree-context-menu button.danger {
  margin-top: 8px;
  color: #ff4242;
}

.tree-context-menu button.danger:hover {
  color: #ff4242;
  background: #fff4f4;
}

.skill-node-form :deep(.el-input__wrapper),
.skill-node-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  background: #f3f6fa;
  box-shadow: none;
}

.quick-folder-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  color: #8a98aa;
  font-size: 13px;
}

.quick-folder-row button {
  border: 0;
  color: #303846;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.quick-folder-row button:hover {
  color: #0b63f6;
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
