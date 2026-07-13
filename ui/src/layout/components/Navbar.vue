<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Bell, Search, SwitchButton } from '@element-plus/icons-vue'
import { clearAuth, getUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const title = computed(() => route.meta.title || '后台管理')
const section = computed(() => route.meta.section || '')
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
      <span v-if="section" class="navbar-path">{{ section }}</span>
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
        <span>搜索知识库、文档、任务...</span>
      </div>
      <el-tooltip content="通知" placement="bottom">
        <button class="icon-button notification-button" type="button">
          <el-icon><Bell /></el-icon>
          <span>5</span>
        </button>
      </el-tooltip>
      <el-dropdown trigger="click">
        <button class="avatar-button" type="button" :title="userName">
          {{ avatarText }}
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item :icon="SwitchButton" @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>
