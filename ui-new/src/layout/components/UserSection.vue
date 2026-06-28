<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth, getUser } from '@/utils/auth'

const router = useRouter()
const user = computed(() => getUser() || {})

const logout = () => {
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <div class="user-section">
    <div class="user-chip">
      <span class="user-avatar">{{ (user.userName || 'A').slice(0, 1).toUpperCase() }}</span>
      <span class="user-name">{{ user.userName || 'admin' }}</span>
    </div>
    <el-button text size="small" @click="logout">退出</el-button>
  </div>
</template>

