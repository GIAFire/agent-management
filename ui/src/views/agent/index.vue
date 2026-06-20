<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '智能体名称', prop: 'name', minWidth: 160 },
  { label: '类型', prop: 'type' },
  { label: '默认模型', prop: 'model', minWidth: 150 },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { name: '客服助手', type: 'Harness', model: 'qwen-plus', status: '启用', updatedAt: '2026-06-20 10:20' },
  { name: '订单分析助手', type: 'ReAct', model: 'gpt-4.1', status: '草稿', updatedAt: '2026-06-19 18:42' },
  { name: '知识库问答', type: 'Harness', model: 'claude-sonnet-4', status: '停用', updatedAt: '2026-06-18 09:16' }
]
</script>

<template>
  <ResourcePage
    title="智能体管理"
    keyword-placeholder="请输入智能体名称"
    :columns="columns"
    :rows="rows"
  />
</template>
