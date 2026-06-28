<script setup>
import StaticModulePage from '@/components/common/StaticModulePage.vue'

const items = [
  { id: 'sk-1', title: '订单处理技能包', category: '业务流程', status: '启用', description: '包含订单查询、退款说明、物流跟踪等多步任务模板。', meta: '6 个技能' },
  { id: 'sk-2', title: '数据分析技能包', category: '数据', status: '启用', description: '封装指标解释、异常定位、报表生成等分析工作流。', meta: '4 个技能' },
  { id: 'sk-3', title: '内容运营技能包', category: '运营', status: '草稿', description: '提供选题扩写、内容校对、渠道适配等能力组合。', meta: '5 个技能' }
]
</script>

<template>
  <StaticModulePage
    title="技能包"
    description="技能包页面先以模拟数据呈现能力市场，后端实体完成后可直接替换为真实导入、版本和绑定接口。"
    create-title="导入技能包"
    accent="green"
    icon="Box"
    :categories="['全部', '业务流程', '数据', '运营']"
    :items="items"
  />
</template>

