<script setup>
import ResourcePage from '@/components/ResourcePage.vue'

const columns = [
  { label: '用户名', prop: 'username', minWidth: 150 },
  { label: '昵称', prop: 'nickname' },
  { label: '手机号', prop: 'phone', minWidth: 140 },
  { label: '角色', prop: 'role' },
  { label: '状态', prop: 'status', type: 'tag' },
  { label: '更新时间', prop: 'updatedAt', minWidth: 170 }
]

const rows = [
  { username: 'admin', nickname: '系统管理员', phone: '13800000000', role: '管理员', status: '启用', updatedAt: '2026-06-20 08:32' },
  { username: 'operator', nickname: '运营同学', phone: '13900000000', role: '运营', status: '启用', updatedAt: '2026-06-19 17:08' },
  { username: 'auditor', nickname: '审计员', phone: '13700000000', role: '审计', status: '停用', updatedAt: '2026-06-18 19:22' }
]
</script>

<template>
  <ResourcePage
    title="用户管理"
    keyword-placeholder="请输入用户名或昵称"
    :columns="columns"
    :rows="rows"
  />
</template>
