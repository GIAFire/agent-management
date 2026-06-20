<script setup>
import { computed, ref } from 'vue'
import { Delete, Download, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  keywordPlaceholder: {
    type: String,
    default: '请输入关键字'
  },
  columns: {
    type: Array,
    default: () => []
  },
  rows: {
    type: Array,
    default: () => []
  }
})

const keyword = ref('')
const status = ref('')
const page = ref(1)
const pageSize = ref(10)

const filteredRows = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return props.rows.filter((row) => {
    const matchKeyword = !text || Object.values(row).some((value) => String(value).toLowerCase().includes(text))
    const matchStatus = !status.value || row.status === status.value
    return matchKeyword && matchStatus
  })
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const enabledCount = computed(() => props.rows.filter((item) => item.status === '启用').length)
const disabledCount = computed(() => props.rows.filter((item) => item.status === '停用').length)

const tagType = (value) => {
  if (value === '启用' || value === '已发布' || value === '在线') {
    return 'success'
  }
  if (value === '停用' || value === '禁用' || value === '离线') {
    return 'info'
  }
  if (value === '草稿' || value === '审核中') {
    return 'warning'
  }
  return ''
}

const resetQuery = () => {
  keyword.value = ''
  status.value = ''
  page.value = 1
}
</script>

<template>
  <section class="resource-page">
    <div class="query-bar">
      <el-form class="query-form" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="keyword"
            :placeholder="keywordPlaceholder"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="status"
            clearable
            placeholder="全部"
          >
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
            <el-option label="草稿" value="草稿" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-panel">
      <div class="table-toolbar">
        <div class="table-title">
          <h2>{{ title }}列表</h2>
          <span>共 {{ filteredRows.length }} 条</span>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="Plus">新增</el-button>
          <el-button :icon="Download">导出</el-button>
        </div>
      </div>

      <div class="metric-strip">
        <div class="metric-item">
          <span>全部</span>
          <strong>{{ rows.length }}</strong>
        </div>
        <div class="metric-item">
          <span>启用</span>
          <strong>{{ enabledCount }}</strong>
        </div>
        <div class="metric-item">
          <span>停用</span>
          <strong>{{ disabledCount }}</strong>
        </div>
      </div>

      <el-table
        :data="pagedRows"
        stripe
        class="data-table"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column
          v-for="column in columns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :min-width="column.minWidth || 120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-tag
              v-if="column.type === 'tag'"
              :type="tagType(row[column.prop])"
              effect="light"
            >
              {{ row[column.prop] }}
            </el-tag>
            <span v-else>{{ row[column.prop] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default>
            <el-button link type="primary" :icon="Edit">编辑</el-button>
            <el-button link type="danger" :icon="Delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="filteredRows.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </div>
  </section>
</template>
