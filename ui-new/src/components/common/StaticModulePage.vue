<script setup>
import { computed, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import ModuleShell from '@/components/common/ModuleShell.vue'
import ResourceCard from '@/components/common/ResourceCard.vue'
import CreateCard from '@/components/common/CreateCard.vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  createTitle: {
    type: String,
    default: '新建配置'
  },
  accent: {
    type: String,
    default: 'blue'
  },
  icon: {
    type: String,
    default: 'Document'
  },
  items: {
    type: Array,
    default: () => []
  },
  categories: {
    type: Array,
    default: () => ['全部']
  }
})

const keyword = ref('')
const selectedCategory = ref('全部')
const previewVisible = ref(false)
const current = ref({})

const categoryOptions = computed(() => props.categories.map((item) => ({ label: item, value: item })))

const filteredItems = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return props.items.filter((item) => {
    const categoryMatched = selectedCategory.value === '全部' || item.category === selectedCategory.value
    const wordMatched = !word ||
      item.title.toLowerCase().includes(word) ||
      item.description.toLowerCase().includes(word)
    return categoryMatched && wordMatched
  })
})

const showPreview = (item = {}) => {
  current.value = item
  previewVisible.value = true
}
</script>

<template>
  <ModuleShell :title="title" :description="description">
    <template #filters>
      <div class="filter-left">
        <el-segmented v-model="selectedCategory" :options="categoryOptions" />
      </div>
      <div class="filter-right">
        <el-input v-model="keyword" clearable placeholder="搜索名称或描述" style="width: 300px">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </template>

    <section class="card-grid">
      <CreateCard :title="createTitle" description="后端实体接入后即可替换为真实保存" @click="showPreview({ title: createTitle, description: '当前页面使用前端模拟数据。' })" />
      <ResourceCard
        v-for="item in filteredItems"
        :key="item.id"
        :title="item.title"
        :description="item.description"
        :tags="[item.category, item.status]"
        :meta="item.meta"
        :icon="icon"
        :accent="accent"
        :actions="[{ key: 'view', label: '查看' }]"
        @action="showPreview(item)"
      />
    </section>

    <el-drawer v-model="previewVisible" :title="current.title || title" size="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="分类">{{ current.category || '未分类' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ current.status || '草稿' }}</el-descriptions-item>
        <el-descriptions-item label="说明">{{ current.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.meta || '模拟数据' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </ModuleShell>
</template>
