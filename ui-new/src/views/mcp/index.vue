<script setup>
import StaticModulePage from '@/components/common/StaticModulePage.vue'

const items = [
  { id: 'mcp-1', title: '文件系统 MCP', category: 'STDIO', status: '启用', description: '通过 MCP 暴露受限目录的文件读取、搜索和摘要能力。', meta: '12 个工具' },
  { id: 'mcp-2', title: '浏览器自动化 MCP', category: 'SSE', status: '启用', description: '为智能体提供页面打开、点击、截图和表单填写能力。', meta: '8 个工具' },
  { id: 'mcp-3', title: '企业知识 MCP', category: 'HTTP', status: '草稿', description: '连接企业内部服务目录，统一暴露查询接口。', meta: '5 个工具' }
]
</script>

<template>
  <StaticModulePage
    title="MCP"
    description="MCP 页面先展示服务器、协议和工具暴露方式，后端接入后可与智能体配置中的能力选择直接联动。"
    create-title="新增 MCP 服务"
    accent="cyan"
    icon="Cloudy"
    :categories="['全部', 'STDIO', 'SSE', 'HTTP']"
    :items="items"
  />
</template>

