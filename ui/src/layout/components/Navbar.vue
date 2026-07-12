<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Bell, Search, SwitchButton } from '@element-plus/icons-vue'
import { clearAuth, getUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const title = computed(() => route.meta.title || '后台管理')
const section = computed(() => route.meta.section || 'AGENT MANAGEMENT')
const user = getUser()
const userName = user?.userName || 'Fire'
const avatarText = computed(() => userName.charAt(0).toUpperCase())

const logout = () => {
  clearAuth()
  router.replace('/login')
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-title">
      <span class="navbar-path">{{ section }}</span>
      <h1>{{ title }}</h1>
    </div>
    <div class="navbar-actions">
      <el-button class="env-button">
        <span class="env-dot" />
        生产环境
        <el-icon><ArrowDown /></el-icon>
      </el-button>
      <div class="global-search">
        <el-icon><Search /></el-icon>
        <span>搜索智能体、知识库、工具等</span>
        <kbd>⌘ K</kbd>
      </div>
      <el-tooltip content="通知" placement="bottom">
        <button class="icon-button notification-button" type="button">
          <el-icon><Bell /></el-icon>
          <span>3</span>
        </button>
      </el-tooltip>
      <div class="user-avatar" :title="userName">
        {{ avatarText }}
      </div>
      <el-tooltip content="退出登录" placement="bottom">
        <button class="icon-button logout-button" type="button" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </button>
      </el-tooltip>
    </div>
  </header>
</template>
