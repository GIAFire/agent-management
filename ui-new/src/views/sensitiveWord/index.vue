<script setup>
import StaticModulePage from '@/components/common/StaticModulePage.vue'

const items = [
  { id: 'sw-1', title: '客服安全词库', category: '客服', status: '启用', description: '过滤辱骂、隐私、联系方式等高风险词，适合在线客服智能体。', meta: '128 个词' },
  { id: 'sw-2', title: '金融合规词库', category: '合规', status: '启用', description: '覆盖收益承诺、违规荐股、敏感资质等金融场景表达。', meta: '76 个词' },
  { id: 'sw-3', title: '营销审核词库', category: '营销', status: '草稿', description: '用于生成内容前置审核，减少夸大宣传和禁用表达。', meta: '42 个词' }
]
</script>

<template>
  <StaticModulePage
    title="敏感词"
    description="敏感词模块负责为智能体输出和工具调用前后的文本增加安全过滤层，可按业务场景组织词库、命中策略和替换动作。"
    create-title="新建词库"
    accent="red"
    icon="Warning"
    :categories="['全部', '客服', '合规', '营销']"
    :items="items"
  />
</template>

