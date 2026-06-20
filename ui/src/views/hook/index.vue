<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '钩子名称', prop: 'name', minWidth: 160 },
  { label: '触发点', prop: 'triggerPoint', minWidth: 150 },
  { label: '执行动作', prop: 'action', minWidth: 180 },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { name: '运行前参数校验', triggerPoint: 'Before Run', action: '校验输入变量', status: '启用', updatedAt: '2026-06-20 14:22' },
  { name: '工具调用审计', triggerPoint: 'Tool Call', action: '写入审计日志', status: '启用', updatedAt: '2026-06-19 09:05' },
  { name: '异常告警', triggerPoint: 'Run Failed', action: '发送通知', status: '草稿', updatedAt: '2026-06-17 21:18' }
]
</script>

<template>
  <ResourcePage
    title="钩子管理"
    keyword-placeholder="请输入钩子名称"
    :columns="columns"
    :rows="rows"
  />
</template>
