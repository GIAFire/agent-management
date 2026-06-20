<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '模型别名', prop: 'alias', minWidth: 160 },
  { label: '供应商', prop: 'provider' },
  { label: '模型名称', prop: 'modelName', minWidth: 160 },
  { label: '温度', prop: 'temperature' },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { alias: '主力对话模型', provider: 'DashScope', modelName: 'qwen-plus', temperature: '0.7', status: '启用', updatedAt: '2026-06-20 11:05' },
  { alias: '推理模型', provider: 'OpenAI', modelName: 'gpt-4.1', temperature: '0.4', status: '启用', updatedAt: '2026-06-19 16:30' },
  { alias: '本地测试模型', provider: 'Ollama', modelName: 'qwen2.5', temperature: '0.8', status: '停用', updatedAt: '2026-06-17 14:18' }
]
</script>

<template>
  <ResourcePage
    title="模型管理"
    keyword-placeholder="请输入模型别名或名称"
    :columns="columns"
    :rows="rows"
  />
</template>
