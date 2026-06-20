<script setup>
import {computed, nextTick, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Delete, Edit, Plus, Refresh, Search} from '@element-plus/icons-vue'
import {
    addModelConfig,
    deleteModelConfig,
    getModelConfig,
    listModelConfig,
    updateModelConfig
} from '@/axios/model'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增模型配置')
const formRef = ref()
const page = ref(1)
const pageSize = ref(10)
const modelRows = ref([])

const queryParams = reactive({
    keyword: '',
    status: ''
})

const form = reactive({
    id: null,
    credentialId: null,
    apiKeyCipher: '',
    baseURL: '',
    provider: '',
    modelName: '',
    temperature: 0.7,
    topP: 0.8,
    maxTokens: 2048,
    timeoutMs: 60000,
    maxAttempts: 3,
    fallbackModelConfigId: null,
    status: 1
})

const rules = {
    apiKeyCipher: [
        {required: true, message: '请输入apiKey', trigger: 'blur'}
    ],
    baseURL: [
        {required: true, message: '请输入模型URL地址', trigger: 'blur'}
    ],
    provider: [
        {required: true, message: '请输入模型供应商', trigger: 'blur'}
    ],
    modelName: [
        {required: true, message: '请输入真实模型名称', trigger: 'blur'}
    ],
    status: [
        {required: true, message: '请选择状态', trigger: 'change'}
    ]
}

const filteredRows = computed(() => {
    const keyword = queryParams.keyword.trim().toLowerCase()
    return modelRows.value.filter((row) => {
        const matchKeyword = !keyword || [row.provider, row.modelName]
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

const formatValue = (value) => {
    return value ?? '-'
}

const resetForm = () => {
    Object.assign(form, {
        id: null,
        credentialId: null,
        apiKeyCipher: '',
        baseURL: '',
        provider: '',
        modelName: '',
        temperature: 0.7,
        topP: 0.8,
        maxTokens: 2048,
        timeoutMs: 60000,
        maxAttempts: 3,
        fallbackModelConfigId: null,
        status: 1
    })
}

const normalizeNumber = (value) => {
    return value === '' || value === undefined ? null : value
}

const loadModelList = async () => {
    loading.value = true
    try {
        const data = await listModelConfig()
        modelRows.value = Array.isArray(data) ? data : []
    } catch {
        modelRows.value = []
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
    dialogTitle.value = '新增模型配置'
    resetForm()
    dialogVisible.value = true
    await nextTick()
    formRef.value?.clearValidate()
}

const handleEdit = async (row) => {
    dialogTitle.value = '编辑模型配置'
    resetForm()
    const data = await getModelConfig(row.id)
    Object.assign(form, {
        id: data?.id,
        credentialId: data?.credentialId ?? null,
        apiKeyCipher: data?.apiKeyCipher || '',
        baseURL: data?.baseURL || '',
        provider: data?.provider || '',
        modelName: data?.modelName || '',
        temperature: Number(data?.temperature ?? 0.7),
        topP: Number(data?.topP ?? 0.8),
        maxTokens: data?.maxTokens ?? 2048,
        timeoutMs: data?.timeoutMs ?? 60000,
        maxAttempts: data?.maxAttempts ?? 3,
        fallbackModelConfigId: data?.fallbackModelConfigId ?? null,
        status: Number(data?.status ?? 1)
    })
    dialogVisible.value = true
    await nextTick()
    formRef.value?.clearValidate()
}

const buildPayload = () => {
    return {
        id: form.id,
        credentialId: normalizeNumber(form.credentialId),
        apiKeyCipher: form.apiKeyCipher,
        baseURL: form.baseURL,
        provider: form.provider,
        modelName: form.modelName,
        temperature: normalizeNumber(form.temperature),
        topP: normalizeNumber(form.topP),
        maxTokens: normalizeNumber(form.maxTokens),
        timeoutMs: normalizeNumber(form.timeoutMs),
        maxAttempts: normalizeNumber(form.maxAttempts),
        fallbackModelConfigId: normalizeNumber(form.fallbackModelConfigId),
        status: form.status
    }
}

const submitForm = async () => {
    await formRef.value.validate()
    submitting.value = true
    try {
        if (form.id) {
            await updateModelConfig(buildPayload())
            ElMessage.success('模型配置更新成功')
        } else {
            await addModelConfig(buildPayload())
            ElMessage.success('模型配置新增成功')
        }

        dialogVisible.value = false
        await loadModelList()
    } finally {
        submitting.value = false
    }
}

const handleDelete = async (row) => {
    try {
        await ElMessageBox.confirm(`确认删除模型配置「${row.provider}」吗？`, '删除确认', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消'
        })
    } catch {
        return
    }

    await deleteModelConfig(row.id)
    ElMessage.success('模型配置删除成功')
    await loadModelList()
}

onMounted(() => {
    loadModelList()
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
                        placeholder="请输入模型别名、供应商或名称"
                        @keyup.enter="handleQuery"
                    />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select
                        v-model="queryParams.status"
                        clearable
                        placeholder="全部"
                    >
                        <el-option label="启用" :value="1"/>
                        <el-option label="停用" :value="0"/>
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
                    <h2>模型管理列表</h2>
                    <span>共 {{ filteredRows.length }} 条</span>
                </div>
                <div class="toolbar-actions">
                    <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
                    <el-button :icon="Refresh" @click="loadModelList">刷新</el-button>
                </div>
            </div>

            <el-table
                v-loading="loading"
                :data="pagedRows"
                stripe
                class="data-table"
            >
                <el-table-column type="index" label="序号" width="70"/>
                <el-table-column prop="provider" label="供应商" min-width="120" show-overflow-tooltip/>
                <el-table-column prop="modelName" label="模型名称" min-width="170" show-overflow-tooltip/>
                <el-table-column prop="baseURL" label="baseURL" min-width="160" show-overflow-tooltip/>
                <el-table-column prop="apiKeyCipher" label="apiKey" min-width="160" show-overflow-tooltip/>
                <el-table-column label="温度" width="90">
                    <template #default="{ row }">
                        {{ formatValue(row.temperature) }}
                    </template>
                </el-table-column>
                <el-table-column label="Top P" width="90">
                    <template #default="{ row }">
                        {{ formatValue(row.topP) }}
                    </template>
                </el-table-column>
                <el-table-column label="Max Tokens" width="120">
                    <template #default="{ row }">
                        {{ formatValue(row.maxTokens) }}
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
                            {{ formatStatus(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="更新时间" min-width="170">
                    <template #default="{ row }">
                        {{ row.updateTime || row.updatedAt || row.createTime || '-' }}
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
            width="680px"
            destroy-on-close
        >
            <el-form
                ref="formRef"
                :model="form"
                :rules="rules"
                label-width="132px"
            >
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="模型URL地址" prop="baseUrl">
                            <el-input v-model="form.baseUrl" placeholder="https://api.modelarts-maas.com/v1"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="api_key" prop="apiKeyCipher">
                            <el-input v-model="form.apiKeyCipher"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="供应商" prop="provider">
                            <el-input v-model="form.provider" placeholder="如 dashscope/openai"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="模型名称" prop="modelName">
                            <el-input v-model="form.modelName" placeholder="如 qwen-plus"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="温度">
                            <el-input-number
                                v-model="form.temperature"
                                class="model-number"
                                :min="0"
                                :max="2"
                                :step="0.1"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="Top P">
                            <el-input-number
                                v-model="form.topP"
                                class="model-number"
                                :min="0"
                                :max="1"
                                :step="0.1"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="最大Token数">
                            <el-input-number
                                v-model="form.maxTokens"
                                class="model-number"
                                :min="1"
                                :step="128"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="超时时间(ms)">
                            <el-input-number
                                v-model="form.timeoutMs"
                                class="model-number"
                                :min="1000"
                                :step="1000"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="最大重试次数">
                            <el-input-number
                                v-model="form.maxAttempts"
                                class="model-number"
                                :min="0"
                                :step="1"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="兜底模型配置ID">
                            <el-input-number
                                v-model="form.fallbackModelConfigId"
                                class="model-number"
                                :min="0"
                                :controls="false"
                                placeholder="可为空"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="状态" prop="status">
                            <el-radio-group v-model="form.status">
                                <el-radio :value="1">启用</el-radio>
                                <el-radio :value="0">停用</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
            </template>
        </el-dialog>
    </section>
</template>

<style scoped>
.model-number {
    width: 100%;
}
</style>
