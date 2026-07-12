<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Box,
  Briefcase,
  Grid,
  Monitor,
  Operation,
  Reading,
  Tools,
  User
} from '@element-plus/icons-vue'

const route = useRoute()

const navItems = [
  { title: '总览', path: '/overview', icon: Grid },
  { title: '智能体', path: '/agent/manage', icon: Briefcase },
  { title: '配置中心', path: '/agent/agent-config', icon: Operation },
  { title: '知识库', path: '/agent/knowledge', icon: Reading },
  { title: '工具与技能', path: '/agent/tool', icon: Tools, match: ['/agent/tool', '/agent/skill-package'] },
  { title: '模型', path: '/agent/model', icon: Box },
  { title: '沙箱', path: '/agent/mcp', icon: Monitor, match: ['/agent/mcp', '/agent/hook', '/agent/sensitive-word'] },
  { title: '租户与权限', path: '/user/manage', icon: User, match: ['/user/manage', '/user/tenant'] }
]

const activePath = computed(() => route.meta.activeMenu || route.path)

const isActive = (item) => {
  const matches = item.match || [item.path]
  return matches.some((path) => activePath.value.startsWith(path))
}
</script>

<template>
  <aside class="sidebar-container">
    <RouterLink class="sidebar-logo" to="/overview">
      <div class="logo-symbol" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
      <strong>Agent<span>OS</span></strong>
    </RouterLink>

    <nav class="sidebar-nav" aria-label="主导航">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        class="sidebar-nav-item"
        :class="{ active: isActive(item) }"
        :to="item.path"
      >
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.title }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar-footer">
      <div class="status-pill">
        <span />
        全部服务运行正常
      </div>
      <button class="collapse-button" type="button" aria-label="收起侧栏">
        ‹
      </button>
    </div>
  </aside>
</template>
