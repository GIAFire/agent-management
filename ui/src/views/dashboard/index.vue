<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Briefcase,
  Clock,
  DataLine,
  Lightning,
  MagicStick,
  Monitor,
  More,
  Odometer,
  TrendCharts,
  VideoPlay
} from '@element-plus/icons-vue'
import { getRunOverview } from '@/axios/overview'
import { getUser } from '@/utils/auth'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const rangeMode = ref('近7天')
const customRange = ref([])
let requestSequence = 0

const emptyOverview = () => ({
  metrics: {},
  trend: { points: [], summary: {} },
  recentRuns: [],
  quickAgents: []
})

const overview = ref(emptyOverview())
const rangeOptions = ['近7天', '近30天', '自定义']
const quickIcons = [DataLine, Monitor, Briefcase, Odometer, MagicStick]
const quickTones = ['blue', 'indigo', 'violet', 'orange', 'teal']
const currentUser = getUser()
const userName = currentUser?.userName || '用户'

const pad = value => String(value).padStart(2, '0')

const toDateText = date => (
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
)

const relativeDate = days => {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() - days)
  return toDateText(date)
}

const todayText = relativeDate(0)
const weekdayText = new Intl.DateTimeFormat('zh-CN', { weekday: 'long' }).format(new Date())
const calendarDateText = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric'
}).format(new Date())
const heroDate = `${weekdayText} · ${calendarDateText}`

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const formatNumber = value => Number(value || 0).toLocaleString('zh-CN')

const formatDuration = value => {
  if (value == null) return '--'
  const milliseconds = Number(value)
  if (milliseconds >= 60000) return `${(milliseconds / 60000).toFixed(1)}min`
  if (milliseconds >= 1000) return `${(milliseconds / 1000).toFixed(2)}s`
  return `${Math.round(milliseconds)}ms`
}

const formatPercentChange = (value, prefix = '较昨日同期') => {
  if (value == null) return `${prefix}暂无数据`
  const number = Number(value)
  const arrow = number > 0 ? '↑' : number < 0 ? '↓' : '→'
  return `${prefix} ${arrow} ${Math.abs(number).toFixed(1)}%`
}

const formatPointChange = (value, prefix = '较昨日同期') => {
  if (value == null) return `${prefix}暂无数据`
  const number = Number(value)
  const arrow = number > 0 ? '↑' : number < 0 ? '↓' : '→'
  return `${prefix} ${arrow} ${Math.abs(number).toFixed(1)} 个百分点`
}

const formatDurationChange = (value, prefix = '较昨日同期') => {
  if (value == null) return `${prefix}暂无数据`
  const number = Number(value)
  const arrow = number > 0 ? '↑' : number < 0 ? '↓' : '→'
  return `${prefix} ${arrow} ${formatDuration(Math.abs(number))}`
}

const statCards = computed(() => {
  const metrics = overview.value.metrics || {}
  return [
    {
      label: '智能体总数',
      value: formatNumber(metrics.totalAgents),
      delta: `今日新增 ${formatNumber(metrics.newToday)}`,
      icon: Briefcase,
      tone: 'blue'
    },
    {
      label: '今日运行',
      value: formatNumber(metrics.todayRuns),
      delta: formatPercentChange(metrics.runChangePercent),
      icon: TrendCharts,
      tone: 'cyan'
    },
    {
      label: '今日成功率',
      value: metrics.successRate == null ? '--' : `${Number(metrics.successRate).toFixed(1)}%`,
      delta: formatPointChange(metrics.successRateChange),
      icon: Monitor,
      tone: 'indigo'
    },
    {
      label: '今日平均耗时',
      value: formatDuration(metrics.averageDurationMs),
      delta: formatDurationChange(metrics.averageDurationChangeMs),
      icon: Clock,
      tone: 'teal'
    }
  ]
})

const healthText = computed(() => {
  const rate = overview.value.metrics?.successRate
  if (rate == null) return '今日暂无已结束运行。'
  if (Number(rate) >= 95) return '当前系统运行平稳，智能体表现良好。'
  if (Number(rate) >= 80) return '当前系统运行存在波动，请持续关注。'
  return '当前系统运行需要关注，请检查近期失败记录。'
})

const trendPoints = computed(() => overview.value.trend?.points || [])

const chartGeometry = computed(() => {
  const points = trendPoints.value
  if (!points.length) {
    return { runPolyline: '', successPolyline: '', areaPolygon: '', runDots: [] }
  }
  const left = 20
  const right = 700
  const top = 30
  const bottom = 230
  const maximum = Math.max(
    1,
    ...points.flatMap(point => [Number(point.runCount || 0), Number(point.successCount || 0)])
  )
  const xAt = index => points.length === 1
    ? (left + right) / 2
    : left + ((right - left) * index / (points.length - 1))
  const yAt = value => bottom - ((bottom - top) * Number(value || 0) / maximum)
  const runDots = points.map((point, index) => ({
    x: xAt(index),
    y: yAt(point.runCount),
    date: point.date,
    value: Number(point.runCount || 0)
  }))
  const successDots = points.map((point, index) => ({
    x: xAt(index),
    y: yAt(point.successCount)
  }))
  const serialize = items => items.map(item => `${item.x},${item.y}`).join(' ')
  const runPolyline = serialize(runDots)
  const successPolyline = serialize(successDots)
  const areaPolygon = `${runPolyline} ${runDots.at(-1).x},250 ${runDots[0].x},250`
  return { runPolyline, successPolyline, areaPolygon, runDots }
})

const axisPoints = computed(() => {
  const points = trendPoints.value
  if (points.length <= 7) return points
  const size = 7
  return Array.from({ length: size }, (_, index) => (
    points[Math.round(index * (points.length - 1) / (size - 1))]
  ))
})

const formatAxisDate = value => value ? String(value).slice(5) : '--'

const trendSummary = computed(() => overview.value.trend?.summary || {})

const recentRuns = computed(() => (overview.value.recentRuns || []).map(run => ({
  ...run,
  ...runStatus(run.status)
})))

const quickAgents = computed(() => (overview.value.quickAgents || []).map((agent, index) => ({
  ...agent,
  icon: quickIcons[index % quickIcons.length],
  tone: quickTones[index % quickTones.length]
})))

function runStatus(status) {
  const statuses = {
    RUNNING: { statusLabel: '运行中', tone: 'running' },
    SUCCESS: { statusLabel: '成功', tone: 'success' },
    FAILED: { statusLabel: '失败', tone: 'danger' },
    CANCELLED: { statusLabel: '已取消', tone: 'cancelled' }
  }
  return statuses[String(status || '').toUpperCase()] || {
    statusLabel: status || '未知',
    tone: 'cancelled'
  }
}

const formatRunTime = value => {
  if (!value) return '--'
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
  const sameDay = toDateText(date) === todayText
  return new Intl.DateTimeFormat('zh-CN', sameDay
    ? { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }
    : { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }
  ).format(date)
}

const selectedRange = () => {
  if (rangeMode.value === '近30天') {
    return [relativeDate(29), todayText]
  }
  if (rangeMode.value === '自定义') {
    return customRange.value?.length === 2 ? customRange.value : null
  }
  return [relativeDate(6), todayText]
}

const loadOverview = async () => {
  const range = selectedRange()
  if (!range) return
  const sequence = ++requestSequence
  loading.value = true
  loadError.value = ''
  try {
    const data = await getRunOverview({ startDate: range[0], endDate: range[1] })
    if (sequence === requestSequence) {
      overview.value = data || emptyOverview()
    }
  } catch (error) {
    if (sequence === requestSequence) {
      loadError.value = error?.message || '总览数据加载失败'
    }
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}

const handleRangeModeChange = value => {
  rangeMode.value = value
  if (value === '自定义') {
    if (customRange.value?.length !== 2) {
      customRange.value = [overview.value.trend?.startDate || relativeDate(6), todayText]
    }
    return
  }
  loadOverview()
}

const handleCustomRangeChange = value => {
  customRange.value = value || []
  if (customRange.value.length !== 2) return
  const start = new Date(`${customRange.value[0]}T00:00:00`)
  const end = new Date(`${customRange.value[1]}T00:00:00`)
  const days = Math.round((end - start) / 86400000) + 1
  if (days > 90) {
    ElMessage.warning('自定义日期范围最多为 90 天')
    return
  }
  loadOverview()
}

const disableFutureDate = time => time.getTime() > new Date().setHours(23, 59, 59, 999)

const handleCreateAgent = () => {
  router.push({ path: '/agent/manage', query: { create: '1' } })
}

const openAgentManagement = () => router.push('/agent/manage')

const openRecentRun = run => {
  if (!run.agentId || run.agentName === '已删除智能体') {
    ElMessage.info('该智能体已删除，无法打开运行记录')
    return
  }
  router.push({
    path: '/agent/manage',
    query: { runAgentId: String(run.agentId), runAgentName: run.agentName }
  })
}

const startQuickAgent = agent => {
  if (!agent.modelId) {
    ElMessage.warning('该智能体未绑定可用模型配置，请先完成配置')
    router.push({ path: '/agent/manage', query: { editAgentId: String(agent.id) } })
    return
  }
  router.push({
    name: 'AgentChat',
    params: { agentId: String(agent.id) },
    query: { agentName: agent.agentName, agentKey: agent.agentCode }
  })
}

onMounted(loadOverview)
</script>

<template>
  <section v-loading="loading" class="dashboard-page">
    <el-alert
      v-if="loadError"
      class="overview-error"
      type="error"
      :title="loadError"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button link type="primary" @click="loadOverview">重新加载</el-button>
      </template>
    </el-alert>

    <div class="page-hero overview-hero">
      <div>
        <span class="hero-date">{{ heroDate }}</span>
        <h2>{{ greeting }}，{{ userName }} <el-icon><MagicStick /></el-icon></h2>
        <p>{{ healthText }}</p>
      </div>
      <el-button type="primary" size="large" :icon="Lightning" @click="handleCreateAgent">
        创建智能体
      </el-button>
    </div>

    <div class="stats-grid">
      <article v-for="card in statCards" :key="card.label" class="metric-card">
        <div class="metric-icon" :class="card.tone">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div>
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.delta }}</small>
        </div>
      </article>
    </div>

    <div class="dashboard-grid">
      <article class="panel trend-panel">
        <div class="panel-header">
          <div>
            <h3>运行趋势</h3>
            <p>运行次数与成功次数</p>
          </div>
          <div class="trend-controls">
            <el-segmented
              :model-value="rangeMode"
              :options="rangeOptions"
              @change="handleRangeModeChange"
            />
            <el-date-picker
              v-if="rangeMode === '自定义'"
              :model-value="customRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :disabled-date="disableFutureDate"
              :clearable="false"
              @update:model-value="handleCustomRangeChange"
            />
          </div>
        </div>

        <div class="chart-legend">
          <span><i class="dot run" />运行次数</span>
          <span><i class="dot success" />成功次数</span>
        </div>

        <div class="line-chart" aria-label="运行趋势图">
          <svg viewBox="0 0 720 270" role="img">
            <defs>
              <linearGradient id="areaBlue" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stop-color="#5b8cff" stop-opacity="0.24" />
                <stop offset="100%" stop-color="#5b8cff" stop-opacity="0.02" />
              </linearGradient>
            </defs>
            <path class="grid-line" d="M20 40H700M20 100H700M20 160H700M20 220H700" />
            <polygon v-if="chartGeometry.areaPolygon" class="area-path" :points="chartGeometry.areaPolygon" />
            <polyline
              v-if="chartGeometry.successPolyline"
              class="line success-line"
              :points="chartGeometry.successPolyline"
            />
            <polyline
              v-if="chartGeometry.runPolyline"
              class="line run-line"
              :points="chartGeometry.runPolyline"
            />
            <g v-if="trendPoints.length <= 30" class="chart-points">
              <circle
                v-for="point in chartGeometry.runDots"
                :key="point.date"
                :cx="point.x"
                :cy="point.y"
                r="5"
              >
                <title>{{ point.date }}：{{ point.value }} 次运行</title>
              </circle>
            </g>
          </svg>
          <div
            v-if="axisPoints.length"
            class="chart-axis"
            :style="{ gridTemplateColumns: `repeat(${axisPoints.length}, minmax(0, 1fr))` }"
          >
            <span v-for="point in axisPoints" :key="point.date">{{ formatAxisDate(point.date) }}</span>
          </div>
          <el-empty v-else description="暂无趋势数据" :image-size="54" />
        </div>

        <div class="trend-summary">
          <div>
            <span>总运行次数</span>
            <strong>{{ formatNumber(trendSummary.totalRuns) }}</strong>
            <small>{{ formatPercentChange(trendSummary.totalRunsChangePercent, '较上期') }}</small>
          </div>
          <div>
            <span>总成功次数</span>
            <strong>{{ formatNumber(trendSummary.successRuns) }}</strong>
            <small>{{ formatPercentChange(trendSummary.successRunsChangePercent, '较上期') }}</small>
          </div>
          <div>
            <span>成功率</span>
            <strong>{{ trendSummary.successRate == null ? '--' : `${Number(trendSummary.successRate).toFixed(1)}%` }}</strong>
            <small>{{ formatPointChange(trendSummary.successRateChange, '较上期') }}</small>
          </div>
          <div>
            <span>平均耗时</span>
            <strong>{{ formatDuration(trendSummary.averageDurationMs) }}</strong>
            <small>{{ formatDurationChange(trendSummary.averageDurationChangeMs, '较上期') }}</small>
          </div>
        </div>
      </article>

      <article class="panel flow-panel">
        <div class="panel-header">
          <div>
            <h3>近期运行</h3>
            <p>最新 7 次智能体运行</p>
          </div>
          <el-button link type="primary" @click="openAgentManagement">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-if="recentRuns.length" class="flow-list">
          <button
            v-for="run in recentRuns"
            :key="run.id"
            type="button"
            class="flow-row flow-row-button"
            @click="openRecentRun(run)"
          >
            <i class="flow-dot" :class="run.tone" />
            <strong>{{ run.agentName }}</strong>
            <time>{{ formatRunTime(run.startedAt) }}</time>
            <em :class="run.tone">{{ run.statusLabel }}</em>
          </button>
        </div>
        <el-empty v-else description="暂无运行记录" :image-size="64" />
      </article>

      <article class="panel quick-panel">
        <div class="panel-header">
          <div>
            <h3>快捷启动智能体</h3>
            <p>近 30 天常用智能体</p>
          </div>
          <el-button link type="primary" @click="openAgentManagement">管理</el-button>
        </div>
        <div v-if="quickAgents.length" class="quick-list">
          <div v-for="agent in quickAgents" :key="agent.id" class="quick-row">
            <span class="quick-icon" :class="agent.tone">
              <el-icon><component :is="agent.icon" /></el-icon>
            </span>
            <div>
              <strong>{{ agent.agentName }}</strong>
              <small :title="agent.description">
                {{ agent.description || '暂无描述' }} · {{ formatNumber(agent.runCount30Days) }} 次运行
              </small>
            </div>
            <button
              type="button"
              class="round-action"
              :aria-label="`启动${agent.agentName}`"
              @click="startQuickAgent(agent)"
            >
              <el-icon><VideoPlay /></el-icon>
            </button>
            <button
              type="button"
              class="round-action"
              :aria-label="`管理${agent.agentName}`"
              @click="openAgentManagement"
            >
              <el-icon><More /></el-icon>
            </button>
          </div>
        </div>
        <el-empty v-else description="暂无可用智能体" :image-size="64" />
        <el-button class="more-agents" text bg type="primary" @click="openAgentManagement">
          查看更多智能体
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </article>
    </div>
  </section>
</template>
