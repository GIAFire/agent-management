<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Box,
  Collection,
  Connection,
  Cpu,
  Document,
  Link,
  OfficeBuilding,
  Share,
  Tools,
  User,
  Warning
} from '@element-plus/icons-vue'
import { adminRoutes } from '@/router/modules/admin'

const route = useRoute()
const menus = computed(() => {
  return adminRoutes[0].children
    .filter((item) => !item.meta?.hidden)
    .map((item) => ({
      ...item,
      children: item.children?.filter((child) => !child.meta?.hidden)
    }))
})
const activeMenu = computed(() => route.meta.activeMenu || route.path)
const defaultOpeneds = computed(() => route.matched.map((item) => item.path).filter(Boolean))

const iconMap = {
  Box,
  Collection,
  Connection,
  Cpu,
  Document,
  Link,
  OfficeBuilding,
  Share,
  Tools,
  User,
  Warning
}

const resolvePath = (parentPath, childPath = '') => {
  const path = [parentPath, childPath].filter(Boolean).join('/')
  return path.startsWith('/') ? path : `/${path}`
}
</script>

<template>
  <aside class="sidebar-container">
    <div class="sidebar-logo">
      <div class="logo-mark">A</div>
      <div>
        <strong>Agent Admin</strong>
        <span>管理控制台</span>
      </div>
    </div>

    <el-scrollbar class="sidebar-scrollbar">
      <el-menu
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        class="sidebar-menu"
        router
      >
        <template
          v-for="item in menus"
          :key="item.path"
        >
          <el-sub-menu
            v-if="item.children?.length"
            :index="resolvePath(item.path)"
          >
            <template #title>
              <el-icon>
                <component :is="iconMap[item.meta.icon]" />
              </el-icon>
              <span>{{ item.meta.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="resolvePath(item.path, child.path)"
            >
              <el-icon>
                <component :is="iconMap[child.meta.icon]" />
              </el-icon>
              <span>{{ child.meta.title }}</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item
            v-else
            :index="resolvePath(item.path)"
          >
            <el-icon>
              <component :is="iconMap[item.meta.icon]" />
            </el-icon>
            <span>{{ item.meta.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>
