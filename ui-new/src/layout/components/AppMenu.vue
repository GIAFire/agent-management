<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as Icons from '@element-plus/icons-vue'
import { moduleRoutes } from '@/router'

const route = useRoute()
const router = useRouter()

const activePath = computed(() => {
  const first = `/${route.path.split('/').filter(Boolean)[0] || 'agent'}`
  return first
})

const go = (path) => {
  router.push(path)
}
</script>

<template>
  <nav class="app-menu" aria-label="module navigation">
    <button
      v-for="item in moduleRoutes"
      :key="item.path"
      class="menu-pill"
      :class="{ active: activePath === item.path }"
      type="button"
      @click="go(item.path)"
    >
      <el-icon>
        <component :is="Icons[item.icon]" />
      </el-icon>
      <span>{{ item.title }}</span>
    </button>
  </nav>
</template>

