<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '知识库名称', prop: 'name', minWidth: 170 },
  { label: '向量模型', prop: 'embedding', minWidth: 150 },
  { label: '文档数', prop: 'docCount' },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { name: '产品手册库', embedding: 'text-embedding-v3', docCount: 128, status: '启用', updatedAt: '2026-06-20 09:35' },
  { name: '售后 FAQ', embedding: 'bge-large-zh', docCount: 54, status: '启用', updatedAt: '2026-06-19 13:12' },
  { name: '历史资料库', embedding: 'text-embedding-v3', docCount: 310, status: '停用', updatedAt: '2026-06-15 17:08' }
]
</script>

<template>
  <ResourcePage
    title="知识库管理"
    keyword-placeholder="请输入知识库名称"
    :columns="columns"
    :rows="rows"
  />
</template>
