<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, FullScreen, Refresh, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { clearAuth, getUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const title = computed(() => route.meta.title || '后台管理')
const user = getUser()
const userName = user?.userName || '管理员'

const logout = () => {
  clearAuth()
  router.replace('/login')
}
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
        <span>{{ userName }}</span>
      </div>
      <el-tooltip content="退出登录" placement="bottom">
        <el-button :icon="SwitchButton" circle @click="logout" />
      </el-tooltip>
    </div>
  </header>
</template>
