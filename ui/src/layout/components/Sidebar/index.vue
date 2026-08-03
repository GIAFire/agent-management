<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Box,
  Briefcase,
  ChatLineSquare,
  Connection,
  Collection,
  Expand,
  Fold,
  Grid,
  MagicStick,
  Operation,
  Tools,
  User
} from '@element-plus/icons-vue'

const route = useRoute()
const SIDEBAR_COLLAPSED_KEY = 'agentScope:sidebar-collapsed'

const readCollapsedPreference = () => {
  if (typeof window === 'undefined') {
    return false
  }

  try {
    return window.localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true'
  } catch {
    return false
  }
}

const navItems = [
  { title: '总览', path: '/overview', icon: Grid },
  { title: '智能体', path: '/agent/manage', icon: Briefcase },
  { title: '子智能体', path: '/agent/subagent', icon: Connection },
  { title: '模型', path: '/agent/model', icon: Box },
  { title: '系统提示词', path: '/agent/prompt', icon: ChatLineSquare },
  { title: '知识库', path: '/agent/knowledge', icon: Collection },
  { title: '技能', path: '/agent/skill', icon: MagicStick, match: ['/agent/skill', '/agent/skill-package'] },
  { title: '工具', path: '/agent/tool', icon: Tools },
  {
    title: '系统管理',
    path: '/user/manage',
    icon: User,
    match: ['/user/manage', '/user/tenant', '/user/role'],
    children: [
      { title: '租户管理', path: '/user/tenant' },
      { title: '用户管理', path: '/user/manage' },
      { title: '角色管理', path: '/user/role' }
    ]
  }
]

const activePath = computed(() => route.meta.activeMenu || route.path)
const openGroups = ref(new Set())
const isCollapsed = ref(readCollapsedPreference())

const setCollapsed = (collapsed) => {
  isCollapsed.value = collapsed

  try {
    window.localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(collapsed))
  } catch {
    // The sidebar still works when browser storage is unavailable.
  }
}

const toggleSidebar = () => {
  setCollapsed(!isCollapsed.value)
}

const isActive = (item) => {
  const matches = item.match || [item.path]
  return matches.some((path) => activePath.value.startsWith(path))
}

const isOpen = (item) => openGroups.value.has(item.path)

const toggleGroup = (item) => {
  const next = new Set(openGroups.value)
  if (isCollapsed.value) {
    next.add(item.path)
    openGroups.value = next
    setCollapsed(false)
    return
  }

  if (next.has(item.path)) {
    next.delete(item.path)
  } else {
    next.add(item.path)
  }
  openGroups.value = next
}

watch(
  activePath,
  () => {
    navItems.forEach((item) => {
      if (item.children && isActive(item)) {
        openGroups.value = new Set([...openGroups.value, item.path])
      }
    })
  },
  { immediate: true }
)
</script>

<template>
  <aside class="sidebar-container" :class="{ 'is-collapsed': isCollapsed }">
    <RouterLink
      class="sidebar-logo"
      to="/overview"
      :aria-label="isCollapsed ? 'AgentOS 总览' : undefined"
      :title="isCollapsed ? 'AgentOS 总览' : undefined"
    >
      <div class="logo-symbol" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
      <strong>zhiran<span>AI</span></strong>
    </RouterLink>

    <nav class="sidebar-nav" aria-label="主导航">
      <template v-for="item in navItems" :key="item.path">
        <div v-if="item.children" class="sidebar-nav-group">
          <button
            class="sidebar-nav-item sidebar-nav-parent"
            :class="{ active: isActive(item) }"
            type="button"
            :aria-expanded="!isCollapsed && isOpen(item)"
            :aria-label="isCollapsed ? item.title : undefined"
            :title="isCollapsed ? item.title : undefined"
            @click="toggleGroup(item)"
          >
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.title }}</span>
            <i class="sidebar-group-arrow" />
          </button>
          <div v-if="isOpen(item)" class="sidebar-sub-nav">
            <RouterLink
              v-for="child in item.children"
              :key="child.path"
              class="sidebar-sub-nav-item"
              :class="{ active: activePath.startsWith(child.path) }"
              :to="child.path"
            >
              <span>{{ child.title }}</span>
            </RouterLink>
          </div>
        </div>
        <RouterLink
          v-else
          class="sidebar-nav-item"
          :class="{ active: isActive(item) }"
          :to="item.path"
          :aria-label="isCollapsed ? item.title : undefined"
          :title="isCollapsed ? item.title : undefined"
        >
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
        </RouterLink>
      </template>
    </nav>

    <div class="sidebar-footer">
      <button
        class="collapse-button"
        type="button"
        :aria-label="isCollapsed ? '展开侧栏' : '收起侧栏'"
        :title="isCollapsed ? '展开侧栏' : '收起侧栏'"
        @click="toggleSidebar"
      >
        <el-icon>
          <Expand v-if="isCollapsed" />
          <Fold v-else />
        </el-icon>
      </button>
    </div>
  </aside>
</template>
