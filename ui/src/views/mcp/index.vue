<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '服务名称', prop: 'name', minWidth: 160 },
  { label: '服务地址', prop: 'endpoint', minWidth: 220 },
  { label: '传输协议', prop: 'transport' },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { name: '文件系统 MCP', endpoint: 'http://localhost:7010/mcp', transport: 'SSE', status: '在线', updatedAt: '2026-06-20 09:12' },
  { name: '数据库 MCP', endpoint: 'stdio://db-mcp', transport: 'STDIO', status: '在线', updatedAt: '2026-06-19 22:30' },
  { name: '浏览器 MCP', endpoint: 'http://localhost:7020/mcp', transport: 'Streamable HTTP', status: '离线', updatedAt: '2026-06-18 17:40' }
]
</script>

<template>
  <ResourcePage
    title="MCP管理"
    keyword-placeholder="请输入 MCP 服务名称"
    :columns="columns"
    :rows="rows"
  />
</template>
