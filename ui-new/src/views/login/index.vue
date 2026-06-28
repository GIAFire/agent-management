<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { saveAccessToken } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const tokenText = ref('')
const form = reactive({
  userName: 'admin',
  password: ''
})

const redirectBack = () => {
  router.push(route.query.redirect || '/agent')
}

const handleLogin = async () => {
  if (!form.userName || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    await login(form)
    ElMessage.success('登录成功')
    redirectBack()
  } finally {
    loading.value = false
  }
}

const handleToken = () => {
  const token = tokenText.value.replace(/^Bearer\s+/i, '').trim()
  if (!token) {
    ElMessage.warning('请粘贴访问令牌')
    return
  }
  saveAccessToken(token, 'Bearer')
  ElMessage.success('令牌已保存')
  redirectBack()
}
</script>

<template>
  <div class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <span class="brand-mark">AS</span>
        <div>
          <h1>AgentScope</h1>
          <p>智能体编排控制台</p>
        </div>
      </div>

      <el-tabs model-value="token" stretch>
        <el-tab-pane label="访问令牌" name="token">
          <el-input
            v-model="tokenText"
            type="textarea"
            :rows="6"
            placeholder="粘贴 Bearer Token"
          />
          <el-button class="login-button" type="primary" @click="handleToken">进入工作台</el-button>
        </el-tab-pane>
        <el-tab-pane label="账号登录" name="account">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="form.userName" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button class="login-button" type="primary" :loading="loading" @click="handleLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

