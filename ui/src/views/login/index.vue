<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Cpu, Lock, User } from '@element-plus/icons-vue'
import { login } from '@/axios/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  userName: 'admin',
  password: 'admin'
})

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    await login({
      userName: form.userName.trim(),
      password: form.password
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/agent/manage'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-shell">
      <div class="login-brand">
        <div class="login-mark">
          <el-icon><Cpu /></el-icon>
        </div>
        <div>
          <span>AgentScope Console</span>
          <strong>管理控制台</strong>
        </div>
      </div>

      <el-form
        ref="formRef"
        class="login-form"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <h1>登录</h1>
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="form.userName"
            :prefix-icon="User"
            autocomplete="username"
            placeholder="请输入用户名"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            size="large"
            type="password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button
          class="login-button"
          :loading="loading"
          native-type="submit"
          size="large"
          type="primary"
        >
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.12), rgba(37, 99, 235, 0.08)),
    var(--page-bg);
}

.login-shell {
  display: grid;
  width: min(920px, 100%);
  min-height: 520px;
  grid-template-columns: minmax(260px, 0.9fr) minmax(320px, 1fr);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: var(--shadow);
}

.login-brand {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 36px;
  color: #ffffff;
  background:
    linear-gradient(160deg, rgba(15, 118, 110, 0.94), rgba(31, 41, 55, 0.96)),
    var(--primary);
}

.login-brand span,
.login-brand strong {
  display: block;
  line-height: 1.3;
}

.login-brand span {
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.login-brand strong {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 760;
  letter-spacing: 0;
}

.login-mark {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 22px;
}

.login-form {
  display: flex;
  justify-content: center;
  flex-direction: column;
  padding: 52px 56px;
}

.login-form h1 {
  margin: 0 0 28px;
  color: var(--ink);
  font-size: 26px;
  line-height: 1.2;
  letter-spacing: 0;
}

.login-button {
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 760px) {
  .login-page {
    padding: 14px;
  }

  .login-shell {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .login-brand {
    padding: 24px;
  }

  .login-form {
    padding: 28px 24px 32px;
  }
}
</style>
