<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '技能包名称', prop: 'name', minWidth: 160 },
  { label: '版本', prop: 'version' },
  { label: '适用场景', prop: 'scene', minWidth: 160 },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { name: '售前咨询包', version: 'v1.2.0', scene: '客服、线索收集', status: '启用', updatedAt: '2026-06-20 12:10' },
  { name: '数据分析包', version: 'v0.9.1', scene: '报表、指标解读', status: '草稿', updatedAt: '2026-06-19 10:42' },
  { name: '流程审批包', version: 'v1.0.0', scene: 'OA、工单流转', status: '停用', updatedAt: '2026-06-16 11:25' }
]
</script>

<template>
  <ResourcePage
    title="技能包管理"
    keyword-placeholder="请输入技能包名称"
    :columns="columns"
    :rows="rows"
  />
</template>
