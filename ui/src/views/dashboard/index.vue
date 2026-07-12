<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
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
import { listAgent } from '@/axios/agent'

const router = useRouter()
const agentRows = ref([])

const statCards = computed(() => [
  {
    label: '活跃智能体',
    value: agentRows.value.length || 24,
    delta: '较上周 ↑ 12.5%',
    icon: Briefcase,
    tone: 'blue'
  },
  {
    label: '今日运行',
    value: '1,286',
    delta: '较昨日 ↑ 11.8%',
    icon: TrendCharts,
    tone: 'cyan'
  },
  {
    label: '成功率',
    value: '98.7%',
    delta: '较昨日 ↑ 0.6%',
    icon: Monitor,
    tone: 'indigo'
  },
  {
    label: '平均响应',
    value: '2.4s',
    delta: '较昨日 ↓ 0.2s',
    icon: Clock,
    tone: 'teal'
  }
])

const flowEvents = [
  { name: '数据分析助手', time: '10:24:18', status: '成功', tone: 'success' },
  { name: '合同审查助手', time: '10:24:12', status: '运行中', tone: 'running' },
  { name: '客服应答助手', time: '10:24:05', status: '成功', tone: 'success' },
  { name: '报告生成助手', time: '10:23:58', status: '失败', tone: 'danger' },
  { name: '数据分析助手', time: '10:23:51', status: '运行中', tone: 'running' },
  { name: '知识检索助手', time: '10:23:45', status: '成功', tone: 'success' },
  { name: '合同审查助手', time: '10:23:38', status: '成功', tone: 'success' }
]

const quickAgents = [
  { name: '数据分析助手', desc: '数据分析与可视化', icon: DataLine, tone: 'blue' },
  { name: '合同审查助手', desc: '合同风险识别与审查', icon: Monitor, tone: 'indigo' },
  { name: '客服应答助手', desc: '智能客服与问答', icon: Briefcase, tone: 'violet' },
  { name: '报告生成助手', desc: '自动生成业务报告', icon: Odometer, tone: 'orange' },
  { name: '知识检索助手', desc: '企业知识检索与问答', icon: MagicStick, tone: 'teal' }
]

const loadAgents = async () => {
  try {
    const data = await listAgent()
    agentRows.value = Array.isArray(data) ? data : []
  } catch {
    agentRows.value = []
  }
}

const handleCreateAgent = () => {
  router.push({
    path: '/agent/manage',
    query: { create: '1' }
  })
}

onMounted(loadAgents)
</script>

<template>
  <section class="dashboard-page">
    <div class="page-hero overview-hero">
      <div>
        <span class="hero-date">星期日 · 7月12日</span>
        <h2>早上好，Fire <el-icon><MagicStick /></el-icon></h2>
        <p>当前系统运行平稳，智能体表现良好。</p>
      </div>
      <el-button type="primary" size="large" :icon="Lightning" @click="handleCreateAgent">创建智能体</el-button>
    </div>

    <div class="stats-grid">
      <article
        v-for="card in statCards"
        :key="card.label"
        class="metric-card"
      >
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
          <el-segmented :model-value="'近7天'" :options="['近7天', '近30天', '自定义']" />
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
            <path class="area-path" d="M20 220 C95 178 122 142 150 126 C215 88 228 125 262 96 C315 50 350 78 382 74 C458 66 450 168 520 174 C589 180 604 111 686 107 L686 250 L20 250 Z" />
            <path class="line success-line" d="M20 196 C95 156 126 150 166 136 C232 114 255 86 320 82 C405 76 424 128 492 158 C568 190 592 124 684 126" />
            <path class="line run-line" d="M20 174 C88 145 114 114 150 104 C205 80 224 94 262 76 C315 35 345 55 382 48 C452 62 462 134 522 130 C590 126 604 68 686 58" />
            <g class="chart-points">
              <circle cx="20" cy="174" r="6" />
              <circle cx="150" cy="104" r="6" />
              <circle cx="262" cy="76" r="6" />
              <circle cx="382" cy="48" r="6" />
              <circle cx="502" cy="88" r="6" />
              <circle cx="622" cy="114" r="6" />
              <circle cx="686" cy="58" r="6" />
            </g>
          </svg>
          <div class="chart-axis">
            <span>05-09</span>
            <span>05-10</span>
            <span>05-11</span>
            <span>05-12</span>
            <span>05-13</span>
            <span>05-14</span>
            <span>05-15</span>
          </div>
        </div>

        <div class="trend-summary">
          <div>
            <span>总运行次数</span>
            <strong>8,932</strong>
            <small>↑ 12.5%</small>
          </div>
          <div>
            <span>总成功次数</span>
            <strong>8,807</strong>
            <small>↑ 11.8%</small>
          </div>
          <div>
            <span>成功率</span>
            <strong>98.7%</strong>
            <small>↑ 0.6%</small>
          </div>
          <div>
            <span>平均响应</span>
            <strong>2.4s</strong>
            <small>↓ 0.2s</small>
          </div>
        </div>
      </article>

      <article class="panel flow-panel">
        <div class="panel-header">
          <div>
            <h3>实时运行流</h3>
            <p>最近 Agent 运行事件</p>
          </div>
          <el-button link type="primary">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <div class="flow-list">
          <div v-for="event in flowEvents" :key="`${event.name}-${event.time}`" class="flow-row">
            <span class="flow-dot" :class="event.tone" />
            <strong>{{ event.name }}</strong>
            <time>{{ event.time }}</time>
            <em :class="event.tone">{{ event.status }}</em>
          </div>
        </div>
      </article>

      <article class="panel quick-panel">
        <div class="panel-header">
          <div>
            <h3>快捷启动 Agent</h3>
            <p>常用智能体</p>
          </div>
          <el-button link type="primary">管理</el-button>
        </div>
        <div class="quick-list">
          <div v-for="agent in quickAgents" :key="agent.name" class="quick-row">
            <span class="quick-icon" :class="agent.tone">
              <el-icon><component :is="agent.icon" /></el-icon>
            </span>
            <div>
              <strong>{{ agent.name }}</strong>
              <small>{{ agent.desc }}</small>
            </div>
            <button type="button" class="round-action" aria-label="启动">
              <el-icon><VideoPlay /></el-icon>
            </button>
            <button type="button" class="round-action" aria-label="更多">
              <el-icon><More /></el-icon>
            </button>
          </div>
        </div>
        <el-button class="more-agents" text bg type="primary">
          查看更多 Agent
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </article>
    </div>
  </section>
</template>
