<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { addTenant, deleteTenant, getTenant, listTenant, updateTenant } from '@/axios/tenant'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增租户')
const formRef = ref()
const page = ref(1)
const pageSize = ref(10)
const tenantRows = ref([])

const queryParams = reactive({
  keyword: '',
  status: ''
})

const form = reactive({
  id: null,
  tenantCode: '',
  tenantName: '',
  status: 1,
  nacosNamespaceId: '',
  remark: ''
})

const rules = {
  tenantCode: [
    { required: true, message: '请输入租户编码', trigger: 'blur' }
  ],
  tenantName: [
    { required: true, message: '请输入租户名称', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const filteredRows = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  return tenantRows.value.filter((row) => {
    const matchKeyword = !keyword || [row.tenantName, row.tenantCode, row.nacosNamespaceId, row.remark]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchStatus = queryParams.status === '' || Number(row.status) === Number(queryParams.status)
    return matchKeyword && matchStatus
  })
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const formatStatus = (status) => {
  return Number(status) === 1 ? '启用' : '停用'
}

const formatTime = (row) => {
  return row.createTime || row.updatedAt || row.createdAt || '-'
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    tenantCode: '',
    tenantName: '',
    status: 1,
    nacosNamespaceId: '',
    remark: ''
  })
}

const loadTenantList = async () => {
  loading.value = true
  try {
    const data = await listTenant()
    tenantRows.value = Array.isArray(data) ? data : []
  } catch {
    tenantRows.value = []
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  page.value = 1
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.status = ''
  page.value = 1
}

const handleAdd = async () => {
  dialogTitle.value = '新增租户'
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑租户'
  resetForm()
  const data = await getTenant(row.id)
  Object.assign(form, {
    id: data?.id,
    tenantCode: data?.tenantCode || '',
    tenantName: data?.tenantName || '',
    status: Number(data?.status ?? 1),
    nacosNamespaceId: data?.nacosNamespaceId || '',
    remark: data?.remark || ''
  })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      id: form.id,
      tenantCode: form.tenantCode,
      tenantName: form.tenantName,
      status: form.status,
      nacosNamespaceId: form.nacosNamespaceId,
      remark: form.remark
    }

    if (form.id) {
      await updateTenant(payload)
      ElMessage.success('租户更新成功')
    } else {
      await addTenant(payload)
      ElMessage.success('租户新增成功')
    }

    dialogVisible.value = false
    await loadTenantList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除租户「${row.tenantName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteTenant(row.id)
  ElMessage.success('租户删除成功')
  await loadTenantList()
}

onMounted(() => {
  loadTenantList()
})
</script>

<template>
  <section class="resource-page">
    <div class="query-bar">
      <el-form class="query-form" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            clearable
            placeholder="请输入租户名称或编码"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryParams.status"
            clearable
            placeholder="全部"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-panel">
      <div class="table-toolbar">
        <div class="table-title">
          <h2>租户管理列表</h2>
          <span>共 {{ filteredRows.length }} 条</span>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
          <el-button :icon="Refresh" @click="loadTenantList">刷新</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedRows"
        stripe
        class="data-table"
      >
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="tenantName" label="租户名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="tenantCode" label="租户编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="nacosNamespaceId" label="Nacos命名空间" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="112px"
      >
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="form.tenantName" placeholder="请输入租户名称" />
        </el-form-item>
        <el-form-item label="租户编码" prop="tenantCode">
          <el-input v-model="form.tenantCode" placeholder="请输入租户编码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Nacos命名空间">
          <el-input v-model="form.nacosNamespaceId" placeholder="请输入 Nacos 命名空间ID" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
