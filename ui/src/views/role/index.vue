<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listTenant } from '@/axios/tenant'
import { addRole, deleteRole, getRole, listRole, updateRole } from '@/axios/role'
import { listUserRole } from '@/axios/userRole'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const formRef = ref()
const page = ref(1)
const pageSize = ref(10)
const roleRows = ref([])
const tenantRows = ref([])
const userRoleRows = ref([])

const queryParams = reactive({
  keyword: '',
  tenantId: '',
  status: ''
})

const form = reactive({
  id: null,
  roleCode: '',
  roleName: '',
  description: '',
  tenantId: '',
  status: 1
})

const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  tenantId: [{ required: true, message: '请选择所属租户', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const tenantMap = computed(() => {
  return tenantRows.value.reduce((map, item) => {
    map[String(item.id)] = item.tenantName
    return map
  }, {})
})

const filteredRows = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  return roleRows.value.filter((row) => {
    const matchKeyword = !keyword || [row.roleCode, row.roleName, row.description]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchTenant = !queryParams.tenantId || String(row.tenantId || '') === String(queryParams.tenantId)
    const matchStatus = queryParams.status === '' || Number(row.status) === Number(queryParams.status)
    return matchKeyword && matchTenant && matchStatus
  })
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const defaultTenantId = computed(() => {
  return tenantRows.value[0]?.id ? String(tenantRows.value[0].id) : '1'
})

const normalizeId = (value) => {
  return value === '' || value === undefined || value === null ? null : String(value).trim()
}

const formatStatus = (status) => Number(status) === 1 ? '启用' : '停用'

const formatTime = (row) => row.updatedAt || row.createdAt || '-'

const tenantName = (tenantId) => tenantMap.value[String(tenantId)] || `租户 ${tenantId || '-'}`

const memberCount = (roleId) => {
  return userRoleRows.value.filter((item) => String(item.roleId) === String(roleId)).length
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    roleCode: '',
    roleName: '',
    description: '',
    tenantId: defaultTenantId.value,
    status: 1
  })
}

const loadRoleList = async () => {
  loading.value = true
  try {
    const [roleResult, tenantResult, userRoleResult] = await Promise.allSettled([
      listRole(),
      listTenant(),
      listUserRole()
    ])
    roleRows.value = roleResult.status === 'fulfilled' && Array.isArray(roleResult.value) ? roleResult.value : []
    tenantRows.value = tenantResult.status === 'fulfilled' && Array.isArray(tenantResult.value) ? tenantResult.value : []
    userRoleRows.value = userRoleResult.status === 'fulfilled' && Array.isArray(userRoleResult.value) ? userRoleResult.value : []
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  page.value = 1
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.tenantId = ''
  queryParams.status = ''
  page.value = 1
}

const handleAdd = async () => {
  dialogTitle.value = '新增角色'
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑角色'
  resetForm()
  const data = await getRole(row.id)
  Object.assign(form, {
    id: data?.id,
    roleCode: data?.roleCode || '',
    roleName: data?.roleName || '',
    description: data?.description || '',
    tenantId: data?.tenantId ? String(data.tenantId) : defaultTenantId.value,
    status: Number(data?.status ?? 1)
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
      id: normalizeId(form.id),
      roleCode: form.roleCode,
      roleName: form.roleName,
      description: form.description,
      tenantId: normalizeId(form.tenantId),
      status: form.status
    }

    if (form.id) {
      await updateRole(payload)
      ElMessage.success('角色更新成功')
    } else {
      await addRole(payload)
      ElMessage.success('角色新增成功')
    }

    dialogVisible.value = false
    await loadRoleList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteRole(row.id)
  ElMessage.success('角色删除成功')
  await loadRoleList()
}

onMounted(loadRoleList)
</script>

<template>
  <section class="resource-page">
    <div class="query-bar">
      <el-form class="query-form" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            clearable
            placeholder="请输入角色编码或名称"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="租户">
          <el-select v-model="queryParams.tenantId" clearable placeholder="全部租户">
            <el-option
              v-for="tenant in tenantRows"
              :key="tenant.id"
              :label="tenant.tenantName"
              :value="String(tenant.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部">
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
          <h2>角色管理列表</h2>
          <span>共 {{ filteredRows.length }} 条</span>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
          <el-button :icon="Refresh" @click="loadRoleList">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="pagedRows" stripe class="data-table">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="roleCode" label="角色编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="roleName" label="角色名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="所属租户" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ tenantName(row.tenantId) }}</template>
        </el-table-column>
        <el-table-column label="成员数" width="100">
          <template #default="{ row }">{{ memberCount(row.id) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="角色描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row) }}</template>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="104px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入唯一角色编码，如 ADMIN" />
        </el-form-item>
        <el-form-item label="所属租户" prop="tenantId">
          <el-select v-model="form.tenantId" placeholder="请选择所属租户">
            <el-option
              v-for="tenant in tenantRows"
              :key="tenant.id"
              :label="tenant.tenantName"
              :value="String(tenant.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
