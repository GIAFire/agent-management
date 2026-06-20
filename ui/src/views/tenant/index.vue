<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '租户名称', prop: 'name', minWidth: 160 },
  { label: '租户编码', prop: 'code', minWidth: 150 },
  { label: '管理员', prop: 'admin' },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '创建时间', prop: 'createdAt', minWidth: 170 }
]

const rows = [
  { name: '默认租户', code: 'default', admin: 'admin', status: '启用', createdAt: '2026-06-20 09:00' },
  { name: '华东业务组', code: 'east-cn', admin: 'zhangsan', status: '启用', createdAt: '2026-06-19 10:24' },
  { name: '测试租户', code: 'sandbox', admin: 'tester', status: '停用', createdAt: '2026-06-18 14:45' }
]
</script>

<template>
  <ResourcePage
    title="租户管理"
    keyword-placeholder="请输入租户名称或编码"
    :columns="columns"
    :rows="rows"
  />
</template>
