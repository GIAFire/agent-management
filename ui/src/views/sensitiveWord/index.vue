<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '敏感词', prop: 'word', minWidth: 150 },
  { label: '分类', prop: 'category' },
  { label: '处理方式', prop: 'strategy', minWidth: 150 },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { word: '示例词 A', category: '合规', strategy: '拦截', status: '启用', updatedAt: '2026-06-20 15:04' },
  { word: '示例词 B', category: '隐私', strategy: '脱敏', status: '启用', updatedAt: '2026-06-19 11:45' },
  { word: '示例词 C', category: '业务', strategy: '提醒', status: '停用', updatedAt: '2026-06-18 13:33' }
]
</script>

<template>
  <ResourcePage
    title="敏感词管理"
    keyword-placeholder="请输入敏感词"
    :columns="columns"
    :rows="rows"
  />
</template>
