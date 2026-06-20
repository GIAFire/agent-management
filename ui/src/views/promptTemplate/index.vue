<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '模板名称', prop: 'name', minWidth: 170 },
  { label: '场景', prop: 'scene', minWidth: 150 },
  { label: '版本', prop: 'version' },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { name: '通用客服提示词', scene: '客服问答', version: 'v1.0', status: '启用', updatedAt: '2026-06-20 10:02' },
  { name: '数据分析提示词', scene: 'BI 分析', version: 'v1.1', status: '启用', updatedAt: '2026-06-19 12:31' },
  { name: '审批助手提示词', scene: '流程审批', version: 'v0.8', status: '草稿', updatedAt: '2026-06-18 16:26' }
]
</script>

<template>
  <ResourcePage
    title="系统提示词模板"
    keyword-placeholder="请输入模板名称"
    :columns="columns"
    :rows="rows"
  />
</template>
