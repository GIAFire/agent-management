<script setup>
import { computed } from 'vue'
import { MoreFilled } from '@element-plus/icons-vue'
import * as Icons from '@element-plus/icons-vue'

const props = defineProps({
  icon: {
    type: String,
    default: 'Document'
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  tags: {
    type: Array,
    default: () => []
  },
  meta: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  accent: {
    type: String,
    default: 'blue'
  },
  actions: {
    type: Array,
    default: () => [
      { key: 'view', label: '查看' },
      { key: 'edit', label: '编辑' },
      { key: 'delete', label: '删除', danger: true }
    ]
  }
})

const emit = defineEmits(['action', 'click'])

const iconComponent = computed(() => Icons[props.icon] || Icons.Document)
</script>

<template>
  <article class="resource-card" :class="[`accent-${accent}`, { disabled }]" @click="emit('click')">
    <div class="card-head">
      <div class="card-avatar">
        <el-icon><component :is="iconComponent" /></el-icon>
      </div>
      <button class="card-title" type="button" :title="title" @click.stop="emit('action', 'view')">
        {{ title }}
      </button>
      <el-dropdown trigger="click" @command="(command) => emit('action', command)">
        <el-button text class="icon-button" @click.stop>
          <el-icon><MoreFilled /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="item in actions"
              :key="item.key"
              :command="item.key"
              :divided="item.divided"
              :class="{ danger: item.danger }"
            >
              {{ item.label }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <p class="card-desc" :title="description">{{ description || '暂无描述' }}</p>

    <footer class="card-foot">
      <div class="tag-list">
        <el-tag v-for="tag in tags" :key="tag" round effect="plain">{{ tag }}</el-tag>
      </div>
      <span class="card-meta">{{ meta }}</span>
    </footer>
  </article>
</template>
