<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '工具名称', prop: 'name', minWidth: 160 },
  { label: '工具类型', prop: 'type' },
  { label: '鉴权方式', prop: 'authType' },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { name: '订单查询', type: 'HTTP API', authType: 'API Key', status: '启用', updatedAt: '2026-06-20 08:55' },
  { name: '库存校验', type: 'Function', authType: '无', status: '启用', updatedAt: '2026-06-19 15:21' },
  { name: '发票开具', type: 'Workflow', authType: 'OAuth2', status: '草稿', updatedAt: '2026-06-18 20:11' }
]
</script>

<template>
  <ResourcePage
    title="工具管理"
    keyword-placeholder="请输入工具名称"
    :columns="columns"
    :rows="rows"
  />
</template>
