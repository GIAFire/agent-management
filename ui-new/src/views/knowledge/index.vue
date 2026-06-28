<script setup>
import StaticModulePage from '@/components/common/StaticModulePage.vue'

const items = [
  { id: 'kb-1', title: '售后政策知识库', category: '本地文档', status: '启用', description: '收录退换货、质保、发票和物流异常处理政策。', meta: '238 个切片' },
  { id: 'kb-2', title: '产品手册知识库', category: '向量库', status: '启用', description: '用于产品参数、兼容性和安装步骤问答。', meta: '612 个切片' },
  { id: 'kb-3', title: '内部制度知识库', category: '本地文档', status: '草稿', description: '覆盖组织制度、审批流程和安全规范。', meta: '94 个切片' }
]
</script>

<template>
  <StaticModulePage
    title="知识库"
    description="知识库页面先模拟本地文档和向量库资源，保留后续接入检索、切片、召回测试和智能体绑定的位置。"
    create-title="新建知识库"
    accent="blue"
    icon="Collection"
    :categories="['全部', '本地文档', '向量库']"
    :items="items"
  />
</template>

