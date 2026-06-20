<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, FullScreen, Refresh, UserFilled } from '@element-plus/icons-vue'

const route = useRoute()
const title = computed(() => route.meta.title || '后台管理')
</script>

<template>
  <header class="navbar">
    <div class="navbar-title">
      <span class="navbar-path">AgentScope Console</span>
      <h1>{{ title }}</h1>
    </div>
    <div class="navbar-actions">
      <el-tooltip content="刷新" placement="bottom">
        <el-button :icon="Refresh" circle />
      </el-tooltip>
      <el-tooltip content="全屏" placement="bottom">
        <el-button :icon="FullScreen" circle />
      </el-tooltip>
      <el-tooltip content="通知" placement="bottom">
        <el-button :icon="Bell" circle />
      </el-tooltip>
      <div class="navbar-user">
        <el-icon><UserFilled /></el-icon>
        <span>管理员</span>
      </div>
    </div>
  </header>
</template>
