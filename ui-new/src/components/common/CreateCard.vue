<script setup>
import { Plus } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: '新建'
  },
  description: {
    type: String,
    default: '创建一条新的资源配置'
  }
})

defineEmits(['click'])
</script>

<template>
  <button class="create-card" type="button" @click="$emit('click')">
    <span class="create-icon">
      <el-icon><Plus /></el-icon>
    </span>
    <span class="create-title">{{ title }}</span>
    <span class="create-desc">{{ description }}</span>
  </button>
</template>

