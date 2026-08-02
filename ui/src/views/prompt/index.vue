<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  Delete,
  Document,
  Edit,
  Link,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  User,
  View
} from '@element-plus/icons-vue'
import {
  createPrompt,
  deletePrompt,
  getPrompt,
  getPromptAnalytics,
  getPromptMetrics,
  pagePrompts,
  updatePrompt
} from '@/axios/prompt'

const loading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const previewVisible = ref(false)
const formRef = ref()
const rows = ref([])
const total = ref(0)
const metricsData = ref({})
const analytics = ref({ bindingRanking: [], recentlyUpdated: [] })
const previewPrompt = ref(null)

const query = reactive({
  current: 1,
  size: 8,
  keyword: ''
})

const createForm = () => ({
  id: null,
  promptName: '',
  description: '',
  sysPrompt: '',
  version: null,
  bindingCount: 0
})

const form = reactive(createForm())

const rules = {
  promptName: [
    { required: true, message: '请输入提示词名称', trigger: 'blur' },
    { min: 1, max: 100, message: '名称长度不能超过 100 个字符', trigger: 'blur' }
  ],
  sysPrompt: [
    { required: true, message: '请输入提示词内容', trigger: 'blur' },
    { min: 1, max: 50000, message: '内容不能超过 50000 个字符', trigger: 'blur' }
  ]
}

const drawerTitle = computed(() => form.id ? '编辑系统提示词' : '新建系统提示词')

const metricCards = computed(() => [
  {
    label: '提示词总数',
    value: formatNumber(metricsData.value.total),
    note: '当前租户未删除记录',
    icon: Document,
    tone: 'blue'
  },
  {
    label: '今日新增',
    value: formatNumber(metricsData.value.newToday),
    note: '今天创建的提示词',
    icon: Calendar,
    tone: 'violet'
  },
  {
    label: '已绑定提示词',
    value: formatNumber(metricsData.value.boundPrompts),
    note: `未绑定 ${formatNumber(unboundCount.value)} 个`,
    icon: Link,
    tone: 'green'
  },
  {
    label: '绑定智能体',
    value: formatNumber(metricsData.value.boundAgents),
    note: '按当前有效智能体统计',
    icon: User,
    tone: 'amber'
  }
])

const unboundCount = computed(() => Math.max(
  Number(metricsData.value.total || 0) - Number(metricsData.value.boundPrompts || 0),
  0
))

const maxBindingCount = computed(() => Math.max(
  ...(analytics.value.bindingRanking || []).map(item => Number(item.bindingCount || 0)),
  1
))

const contentLength = computed(() => form.sysPrompt?.length || 0)

const loadDashboard = async () => {
  loading.value = true
  try {
    const [metricResult, analyticsResult, pageResult] = await Promise.all([
      getPromptMetrics(),
      getPromptAnalytics(5),
      pagePrompts(pageParams())
    ])
    metricsData.value = metricResult || {}
    analytics.value = analyticsResult || { bindingRanking: [], recentlyUpdated: [] }
    rows.value = pageResult?.records || []
    total.value = Number(pageResult?.total || 0)
  } finally {
    loading.value = false
  }
}

const loadPage = async () => {
  loading.value = true
  try {
    const result = await pagePrompts(pageParams())
    rows.value = result?.records || []
    total.value = Number(result?.total || 0)
  } finally {
    loading.value = false
  }
}

const pageParams = () => ({
  current: query.current,
  size: query.size,
  keyword: query.keyword || undefined
})

const search = () => {
  query.current = 1
  loadPage()
}

const resetForm = () => Object.assign(form, createForm())

const openCreate = async () => {
  resetForm()
  drawerVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async (row) => {
  const detail = await getPrompt(row.id)
  Object.assign(form, createForm(), detail || {}, {
    id: detail?.id || row.id,
    description: detail?.description || '',
    sysPrompt: detail?.sysPrompt || ''
  })
  drawerVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openPreview = async (row) => {
  previewPrompt.value = await getPrompt(row.id)
  previewVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      id: form.id || undefined,
      promptName: form.promptName,
      description: form.description,
      sysPrompt: form.sysPrompt,
      version: form.version
    }
    if (form.id) {
      await updatePrompt(payload)
      ElMessage.success('系统提示词已保存，后续运行将使用新内容')
    } else {
      await createPrompt(payload)
      ElMessage.success('系统提示词已创建')
    }
    drawerVisible.value = false
    await loadDashboard()
  } finally {
    submitting.value = false
  }
}

const removePrompt = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除“${row.promptName}”后不可恢复，引用它的智能体将按未配置系统提示词运行。确认继续吗？`,
      '删除系统提示词',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  await deletePrompt(row.id)
  ElMessage.success('系统提示词已删除')
  if (rows.value.length === 1 && query.current > 1) {
    query.current -= 1
  }
  await loadDashboard()
}

const bindingWidth = (count) => `${Math.max(
  (Number(count || 0) / maxBindingCount.value) * 100,
  Number(count || 0) > 0 ? 8 : 0
)}%`

const formatNumber = (value) => Number(value || 0).toLocaleString('zh-CN')

const formatDate = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="prompt-page management-page">
    <div class="management-metrics">
      <article
        v-for="item in metricCards"
        :key="item.label"
        class="management-metric-card"
      >
        <span class="management-metric-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <div>
          <small>{{ item.label }}</small>
          <strong>{{ item.value }}</strong>
          <p>{{ item.note }}</p>
        </div>
      </article>
    </div>

    <div class="management-content-grid">
      <main class="management-panel">
        <div class="management-panel-title">
          <div>
            <h3>系统提示词</h3>
            <p>共 {{ total }} 条，编辑后的内容会直接用于智能体后续运行</p>
          </div>
          <div class="management-filter-bar">
            <el-input
              v-model="query.keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索名称或描述"
              @clear="search"
              @keyup.enter="search"
            />
            <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
            <el-button :icon="Plus" type="primary" @click="openCreate">新建提示词</el-button>
          </div>
        </div>

        <div v-if="rows.length" class="prompt-list">
          <article
            v-for="row in rows"
            :key="row.id"
            class="prompt-card management-data-card"
          >
            <el-dropdown class="management-card-menu" trigger="click">
              <button class="management-card-menu-button" type="button" aria-label="提示词操作">
                <el-icon class="management-card-menu-icon"><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="View" @click="openPreview(row)">查看内容</el-dropdown-item>
                  <el-dropdown-item :icon="Edit" @click="openEdit(row)">编辑</el-dropdown-item>
                  <el-dropdown-item
                    :icon="Delete"
                    divided
                    class="danger-item"
                    @click="removePrompt(row)"
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <header class="prompt-card-head">
              <span class="prompt-mark"><el-icon><Document /></el-icon></span>
              <div>
                <h4>{{ row.promptName }}</h4>
                <p>{{ row.description || '暂无描述' }}</p>
              </div>
            </header>

            <button class="content-preview" type="button" @click="openPreview(row)">
              <span>内容预览</span>
              <p>{{ row.contentPreview || '暂无内容' }}</p>
            </button>

            <div class="prompt-facts">
              <span>
                <small>内容长度</small>
                <b>{{ formatNumber(row.contentLength) }} 字符</b>
              </span>
              <span>
                <small>绑定智能体</small>
                <b>{{ formatNumber(row.bindingCount) }} 个</b>
              </span>
            </div>

            <footer>
              <div>
                <small>最近更新</small>
                <b>{{ formatDate(row.updatedAt) }}</b>
              </div>
              <div>
                <el-button :icon="View" @click="openPreview(row)">预览</el-button>
                <el-button type="primary" plain :icon="Edit" @click="openEdit(row)">编辑</el-button>
              </div>
            </footer>
          </article>
        </div>

        <el-empty v-else description="暂无符合条件的系统提示词">
          <el-button type="primary" @click="openCreate">新建提示词</el-button>
        </el-empty>

        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          class="management-pagination"
          layout="total, prev, pager, next, sizes"
          :page-sizes="[8, 16, 32]"
          :total="total"
          @current-change="loadPage"
          @size-change="search"
        />
      </main>

      <aside class="management-side-column">
        <section class="management-side-card">
          <header>
            <h3>绑定排行</h3>
            <p>按当前有效智能体引用数统计</p>
          </header>
          <div v-if="analytics.bindingRanking?.length" class="binding-list">
            <button
              v-for="(item, index) in analytics.bindingRanking"
              :key="item.id"
              type="button"
              @click="openPreview(item)"
            >
              <span class="rank-index">{{ index + 1 }}</span>
              <span class="rank-main">
                <span><b>{{ item.promptName }}</b><em>{{ item.bindingCount }} 个</em></span>
                <i><u :style="{ width: bindingWidth(item.bindingCount) }" /></i>
              </span>
            </button>
          </div>
          <el-empty v-else description="暂无提示词数据" :image-size="68" />
        </section>

        <section class="management-side-card">
          <header>
            <h3>最近更新</h3>
            <p>最近变更内容的系统提示词</p>
          </header>
          <div v-if="analytics.recentlyUpdated?.length" class="recent-list">
            <button
              v-for="item in analytics.recentlyUpdated"
              :key="item.id"
              type="button"
              @click="openPreview(item)"
            >
              <span class="recent-icon"><el-icon><Document /></el-icon></span>
              <span>
                <b>{{ item.promptName }}</b>
                <small>{{ formatNumber(item.contentLength) }} 字符 · {{ formatDate(item.updatedAt) }}</small>
              </span>
            </button>
          </div>
          <el-empty v-else description="暂无更新记录" :image-size="68" />
        </section>
      </aside>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="min(820px, 96vw)"
      destroy-on-close
      class="prompt-drawer"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <section class="form-section">
          <div class="section-heading">
            <span>01</span>
            <div>
              <h4>基础信息</h4>
              <p>同一租户内名称不可重复，名称用于智能体配置时识别。</p>
            </div>
          </div>
          <div class="form-grid">
            <el-form-item label="提示词名称" prop="promptName">
              <el-input
                v-model="form.promptName"
                maxlength="100"
                show-word-limit
                placeholder="例如：数据分析助手"
              />
            </el-form-item>
            <el-form-item label="绑定情况">
              <div class="binding-summary">
                <el-icon><Link /></el-icon>
                <span v-if="form.id">当前被 {{ form.bindingCount || 0 }} 个智能体引用</span>
                <span v-else>创建后可在智能体配置中选择</span>
              </div>
            </el-form-item>
            <el-form-item class="span-2" label="描述">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="说明适用场景、职责或使用约束"
              />
            </el-form-item>
          </div>
        </section>

        <section class="form-section">
          <div class="section-heading content-heading">
            <span>02</span>
            <div>
              <h4>提示词内容</h4>
              <p>保存后会直接影响所有引用它的智能体之后的运行。</p>
            </div>
            <strong>{{ formatNumber(contentLength) }} / 50,000</strong>
          </div>
          <el-form-item prop="sysPrompt" class="content-form-item">
            <el-input
              v-model="form.sysPrompt"
              type="textarea"
              :rows="16"
              maxlength="50000"
              resize="vertical"
              placeholder="输入智能体的角色、目标、边界和行为要求……"
            />
          </el-form-item>
        </section>

        <section class="form-section preview-section">
          <div class="section-heading">
            <span>03</span>
            <div>
              <h4>只读预览</h4>
              <p>按智能体实际读取时的纯文本内容展示。</p>
            </div>
          </div>
          <pre>{{ form.sysPrompt || '尚未输入提示词内容' }}</pre>
        </section>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="previewVisible"
      width="min(820px, 94vw)"
      class="prompt-preview-dialog"
      :title="previewPrompt?.promptName || '提示词预览'"
    >
      <div class="preview-meta">
        <span>{{ previewPrompt?.description || '暂无描述' }}</span>
        <el-tag effect="plain">{{ formatNumber(previewPrompt?.contentLength) }} 字符</el-tag>
        <el-tag effect="plain" type="info">绑定 {{ formatNumber(previewPrompt?.bindingCount) }} 个智能体</el-tag>
      </div>
      <pre class="dialog-preview-content">{{ previewPrompt?.sysPrompt || '暂无内容' }}</pre>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :icon="Edit"
          @click="previewVisible = false; openEdit(previewPrompt)"
        >
          编辑
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.prompt-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.prompt-card {
  display: flex;
  box-sizing: border-box;
  height: 355px;
  min-width: 0;
  overflow: hidden;
  flex-direction: column;
  padding: 18px;
  border: 1px solid #e7ebf3;
  border-radius: 16px;
  background: #fff;
}

.prompt-card-head {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 12px;
  padding-right: 32px;
}

.prompt-card-head > div {
  min-width: 0;
}

.prompt-mark,
.recent-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  color: #526fe5;
  background: #edf1ff;
}

.prompt-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  font-size: 20px;
}

.prompt-card h4 {
  overflow: hidden;
  margin: 1px 0 5px;
  color: #172033;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prompt-card-head p {
  display: -webkit-box;
  overflow: hidden;
  min-height: 36px;
  color: #7f8a9f;
  font-size: 12px;
  line-height: 18px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.content-preview {
  display: block;
  width: 100%;
  min-height: 104px;
  padding: 13px 14px;
  margin: 16px 0 14px;
  border: 1px solid #edf0f6;
  border-radius: 12px;
  text-align: left;
  background: #f8f9fc;
  cursor: pointer;
  transition: border-color .18s ease, background-color .18s ease;
}

.content-preview:hover {
  border-color: #cfd9fa;
  background: #f4f7ff;
}

.content-preview span {
  display: block;
  margin-bottom: 7px;
  color: #8090a8;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: .04em;
}

.content-preview p {
  display: -webkit-box;
  overflow: hidden;
  color: #445168;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 20px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.prompt-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.prompt-facts span {
  padding: 10px 11px;
  border-radius: 10px;
  background: #fafbfe;
}

.prompt-facts small,
.prompt-facts b {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prompt-facts small {
  margin-bottom: 3px;
  color: #929caf;
  font-size: 11px;
}

.prompt-facts b {
  color: #354158;
  font-size: 13px;
}

.prompt-card footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  padding-top: 14px;
  margin-top: auto;
  border-top: 1px solid #eef1f6;
}

.prompt-card footer > div:first-child {
  min-width: 0;
}

.prompt-card footer small,
.prompt-card footer b {
  display: block;
}

.prompt-card footer small {
  margin-bottom: 3px;
  color: #98a1b2;
  font-size: 10px;
}

.prompt-card footer b {
  color: #68748a;
  font-size: 11px;
  font-weight: 500;
}

.prompt-card footer > div:last-child {
  display: flex;
  flex: 0 0 auto;
}

.binding-list,
.recent-list {
  display: grid;
  gap: 8px;
}

.binding-list > button,
.recent-list > button {
  display: flex;
  width: 100%;
  padding: 9px;
  border: 0;
  border-radius: 11px;
  text-align: left;
  background: transparent;
  cursor: pointer;
  transition: background-color .18s ease;
}

.binding-list > button:hover,
.recent-list > button:hover {
  background: #f7f9fd;
}

.binding-list > button {
  align-items: center;
  gap: 10px;
}

.rank-index {
  display: grid;
  width: 25px;
  height: 25px;
  flex: 0 0 25px;
  place-items: center;
  border-radius: 8px;
  color: #596982;
  font-size: 11px;
  font-weight: 700;
  background: #f0f3f9;
}

.binding-list > button:nth-child(-n + 3) .rank-index {
  color: #526fe5;
  background: #edf1ff;
}

.rank-main {
  min-width: 0;
  flex: 1;
}

.rank-main > span {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.rank-main b,
.rank-main em {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-main b {
  color: #354158;
}

.rank-main em {
  color: #8792a6;
  font-style: normal;
  font-weight: 500;
}

.rank-main > i {
  display: block;
  height: 5px;
  margin-top: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf0f6;
}

.rank-main > i > u {
  display: block;
  height: 100%;
  border-radius: inherit;
  text-decoration: none;
  background: linear-gradient(90deg, #5977ef, #8b68e9);
}

.recent-list > button {
  align-items: center;
  gap: 10px;
}

.recent-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  font-size: 15px;
}

.recent-list button > span:last-child {
  min-width: 0;
}

.recent-list b,
.recent-list small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-list b {
  margin-bottom: 3px;
  color: #354158;
  font-size: 12px;
}

.recent-list small {
  color: #939dae;
  font-size: 10px;
}

.form-section {
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid #e8ecf4;
  border-radius: 15px;
  background: #fff;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.section-heading > span {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  place-items: center;
  border-radius: 9px;
  color: #536fdf;
  font-size: 11px;
  font-weight: 700;
  background: #edf1ff;
}

.section-heading > div {
  min-width: 0;
  flex: 1;
}

.section-heading h4 {
  margin: 0 0 3px;
  color: #1d2638;
  font-size: 15px;
}

.section-heading p {
  color: #8a95a8;
  font-size: 12px;
  line-height: 18px;
}

.content-heading > strong {
  flex: 0 0 auto;
  color: #6f7c92;
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.span-2 {
  grid-column: 1 / -1;
}

.binding-summary {
  display: flex;
  width: 100%;
  min-height: 32px;
  align-items: center;
  gap: 8px;
  color: #66748b;
  font-size: 13px;
}

.content-form-item {
  margin-bottom: 0;
}

.content-form-item :deep(.el-textarea__inner) {
  min-height: 330px !important;
  padding: 15px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.75;
}

.preview-section pre,
.dialog-preview-content {
  overflow: auto;
  margin: 0;
  border: 1px solid #e5e9f2;
  border-radius: 12px;
  color: #334057;
  background: #f8f9fc;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-section pre {
  min-height: 130px;
  max-height: 320px;
  padding: 16px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.preview-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.preview-meta > span:first-child {
  min-width: 200px;
  flex: 1;
  color: #738097;
  font-size: 13px;
}

.dialog-preview-content {
  min-height: 280px;
  max-height: 58vh;
  padding: 18px;
}

:deep(.prompt-drawer .el-drawer__body) {
  padding: 18px 20px;
  background: #f7f9fc;
}

:deep(.prompt-drawer .el-drawer__footer) {
  padding: 14px 20px;
  border-top: 1px solid #edf0f5;
}

@media (max-width: 900px) {
  .prompt-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}

@media (max-width: 600px) {
  .prompt-card footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .prompt-card footer > div:last-child,
  .prompt-card footer .el-button {
    width: 100%;
  }
}
</style>

<style scoped src="../../assets/management-page.css"></style>
