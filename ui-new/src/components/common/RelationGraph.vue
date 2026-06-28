<script setup>
defineProps({
  agentName: {
    type: String,
    default: '智能体'
  },
  modelName: {
    type: String,
    default: '模型'
  },
  toolCount: {
    type: Number,
    default: 0
  },
  configName: {
    type: String,
    default: '配置'
  }
})
</script>

<template>
  <div class="relation-graph">
    <div class="graph-node center">
      <span>Agent</span>
      <strong>{{ agentName }}</strong>
    </div>
    <div class="graph-line"></div>
    <div class="graph-node">
      <span>Model</span>
      <strong>{{ modelName }}</strong>
    </div>
    <div class="graph-line"></div>
    <div class="graph-node">
      <span>Config</span>
      <strong>{{ configName }}</strong>
    </div>
    <div class="graph-line"></div>
    <div class="graph-node">
      <span>Tools</span>
      <strong>{{ toolCount }} 个工具</strong>
    </div>
  </div>
</template>

