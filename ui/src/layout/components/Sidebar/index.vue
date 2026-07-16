<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Box,
  Briefcase,
  Connection,
  Collection,
  Grid,
  MagicStick,
  Operation,
  Tools,
  User
} from '@element-plus/icons-vue'

const route = useRoute()

const navItems = [
  { title: '总览', path: '/overview', icon: Grid },
  { title: '智能体', path: '/agent/manage', icon: Briefcase },
  { title: '子智能体', path: '/agent/subagent', icon: Connection },
  { title: '知识库', path: '/agent/knowledge', icon: Collection },
  { title: '工具', path: '/agent/tool', icon: Tools },
  { title: '技能', path: '/agent/skill', icon: MagicStick, match: ['/agent/skill', '/agent/skill-package'] },
  { title: '模型', path: '/agent/model', icon: Box },
  { title: '沙箱', path: '/agent/mcp', icon: Operation, match: ['/agent/mcp', '/agent/hook', '/agent/sensitive-word'] },
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

const isActive = (item) => {
  const matches = item.match || [item.path]
  return matches.some((path) => activePath.value.startsWith(path))
}

const isOpen = (item) => openGroups.value.has(item.path)

const toggleGroup = (item) => {
  const next = new Set(openGroups.value)
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
      <template v-for="item in navItems" :key="item.path">
        <div v-if="item.children" class="sidebar-nav-group">
          <button
            class="sidebar-nav-item sidebar-nav-parent"
            :class="{ active: isActive(item) }"
            type="button"
            :aria-expanded="isOpen(item)"
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
        >
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
        </RouterLink>
      </template>
    </nav>

    <div class="sidebar-footer">
      <div class="status-pill">
        <span />
        全部服务运行正常
      </div>
      <button class="collapse-button" type="button" aria-label="收起侧栏">
        &lt;&lt;
      </button>
    </div>
  </aside>
</template>
