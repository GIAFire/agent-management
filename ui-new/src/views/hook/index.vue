<script setup>
import StaticModulePage from '@/components/common/StaticModulePage.vue'

const items = [
  { id: 'hk-1', title: '请求前参数校验', category: 'PRE_RUN', status: '启用', description: '在智能体运行前检查用户输入、会话上下文和权限边界。', meta: '低风险' },
  { id: 'hk-2', title: '工具调用审计', category: 'TOOL_CALL', status: '启用', description: '记录工具调用参数和结果摘要，便于追踪执行过程。', meta: '中风险' },
  { id: 'hk-3', title: '输出后脱敏', category: 'POST_RUN', status: '草稿', description: '在最终回答前隐藏手机号、邮箱、订单号等敏感信息。', meta: '低风险' }
]
</script>

<template>
  <StaticModulePage
    title="钩子"
    description="钩子模块用于把校验、审计、脱敏、告警等横切逻辑接入智能体生命周期，让编排流程更可控。"
    create-title="新建钩子"
    accent="amber"
    icon="Switch"
    :categories="['全部', 'PRE_RUN', 'TOOL_CALL', 'POST_RUN']"
    :items="items"
  />
</template>

