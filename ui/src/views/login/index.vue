<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Box,
  Collection,
  Connection,
  Lock,
  User
} from '@element-plus/icons-vue'
import loginAgentMap from '@/assets/images/login-agent-map.png'
import { login } from '@/axios/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  userName: 'admin',
  password: 'admin',
  remember: false
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
    <header class="login-topbar">
      <div class="brand-lockup">
        <span class="brand-symbol"><i /></span>
        <div>
          <strong>AgentOS</strong>
          <small>AI AGENT PLATFORM</small>
        </div>
      </div>
      <div class="system-state">
        <i />
        <span>SYSTEM READY</span>
        <b>·</b>
        <span>V1.0</span>
      </div>
    </header>

    <section class="login-shell">
      <section class="login-intro">
        <div class="intro-copy">
          <span class="intro-kicker">BUILD · ORCHESTRATE · EVOLVE</span>
          <h1>让每一个智能体，<br>都成为可靠的数字同事</h1>
          <p>连接模型、知识、工具与业务流程，构建可协作、可进化的企业级 AI Agent。</p>
        </div>

        <figure class="agent-map" aria-label="智能体能力编排示意图">
          <img :src="loginAgentMap" alt="智能体能力编排示意图" />
        </figure>

        <div class="feature-strip">
          <span><el-icon><Box /></el-icon> 多模型接入</span>
          <i />
          <span><el-icon><Collection /></el-icon> RAG 知识库</span>
          <i />
          <span><el-icon><Connection /></el-icon> 多智能体协作</span>
        </div>
      </section>

      <section class="login-card-wrap">
      <el-form
        ref="formRef"
        class="login-form"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <span class="login-step">01 / LOGIN</span>
        <div class="form-title">
          <span class="form-logo"><i /></span>
          <div>
            <h2>欢迎回来</h2>
            <p>登录 AgentOS，开始构建你的智能体</p>
          </div>
        </div>
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
        <div class="form-options">
          <el-checkbox v-model="form.remember">记住我</el-checkbox>
          <button type="button">忘记密码?</button>
        </div>
        <el-button
          class="login-button"
          :loading="loading"
          native-type="submit"
          size="large"
          type="primary"
        >
          登录 AgentOS <el-icon><ArrowRight /></el-icon>
        </el-button>
        <div class="secure-note">
          <span><el-icon><Lock /></el-icon> 全链路加密</span>
          <i />
          <span>多租户安全隔离</span>
        </div>
      </el-form>
        <p class="card-caption">AGENTOS · ENTERPRISE AI AGENT OPERATING SYSTEM</p>
      </section>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  position: relative;
  overflow-x: hidden;
  min-height: 100vh;
  padding: 84px 64px 48px;
  color: #13233c;
  background:
    radial-gradient(circle at 35% 48%, rgba(47, 117, 255, 0.15), transparent 28%),
    radial-gradient(circle at 78% 42%, rgba(82, 190, 255, 0.14), transparent 24%),
    linear-gradient(135deg, #f4f9ff 0%, #eef6ff 48%, #f8fbff 100%);
}

.login-page::before {
  position: absolute;
  inset: 72px 0 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(124, 160, 210, 0.16) 1px, transparent 1px),
    linear-gradient(90deg, rgba(124, 160, 210, 0.16) 1px, transparent 1px);
  background-size: 26px 26px;
  content: '';
  mask-image: linear-gradient(90deg, #000 0%, rgba(0, 0, 0, 0.5) 58%, transparent 100%);
}

.login-page::after {
  position: absolute;
  right: 34%;
  bottom: 10%;
  width: 980px;
  height: 420px;
  pointer-events: none;
  background:
    repeating-radial-gradient(ellipse at center, rgba(84, 142, 255, 0.14) 0 1px, transparent 1px 34px);
  content: '';
  opacity: 0.56;
  transform: rotate(-9deg);
}

.login-topbar {
  position: absolute;
  z-index: 2;
  top: 0;
  right: 0;
  left: 0;
  display: flex;
  height: 72px;
  align-items: center;
  justify-content: space-between;
  padding: 0 52px;
  border-bottom: 1px solid rgba(181, 202, 232, 0.72);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
}

.brand-lockup,
.system-state,
.feature-strip,
.secure-note {
  display: flex;
  align-items: center;
}

.brand-lockup {
  gap: 14px;
}

.brand-symbol,
.form-logo {
  position: relative;
  display: grid;
  place-items: center;
  color: #ffffff;
  background: linear-gradient(135deg, #0c6dff, #58bdff);
  box-shadow: 0 12px 30px rgba(18, 104, 255, 0.24);
}

.brand-symbol {
  width: 44px;
  height: 44px;
  border-radius: 12px;
}

.brand-symbol i,
.form-logo i {
  width: 18px;
  height: 18px;
  border: 5px solid #ffffff;
  border-radius: 5px;
  transform: rotate(30deg);
}

.brand-lockup strong {
  display: block;
  color: #10213a;
  font-size: 23px;
  font-weight: 900;
  line-height: 1;
}

.brand-lockup small {
  display: block;
  margin-top: 8px;
  color: #8a9bb4;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 4px;
}

.system-state {
  gap: 12px;
  color: #8a98ae;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 4px;
}

.system-state i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #20c66a;
  box-shadow: 0 0 0 4px rgba(32, 198, 106, 0.12);
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  min-height: calc(100vh - 132px);
  grid-template-columns: minmax(560px, 1.15fr) minmax(420px, 0.85fr);
  align-items: center;
  gap: 76px;
  max-width: 1500px;
  margin: 0 auto;
}

.login-intro {
  min-width: 0;
  padding-left: 44px;
}

.intro-kicker {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  color: #126cff;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 3px;
}

.intro-kicker::after {
  width: 86px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, #126cff, transparent);
  content: '';
}

.intro-copy h1 {
  max-width: 720px;
  margin: 26px 0 18px;
  color: #12223a;
  font-size: clamp(42px, 4vw, 68px);
  font-weight: 950;
  letter-spacing: 0;
  line-height: 1.14;
}

.intro-copy p {
  max-width: 760px;
  margin: 0;
  color: #4d607a;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.8;
}

.agent-map {
  width: min(720px, 100%);
  margin: 34px 0 0;
  border: 1px solid rgba(197, 216, 243, 0.9);
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.35);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 18px 45px rgba(47, 90, 154, 0.08);
  backdrop-filter: blur(14px);
}

.agent-map img {
  display: block;
  width: 100%;
  height: auto;
}

.feature-strip {
  gap: 42px;
  margin: 34px 0 0 30px;
  color: #243a5b;
  font-size: 15px;
  font-weight: 850;
}

.feature-strip span {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.feature-strip .el-icon {
  color: #1d75ff;
  font-size: 27px;
}

.feature-strip > i,
.secure-note > i {
  width: 1px;
  height: 22px;
  background: #c9d7ea;
}

.login-card-wrap {
  display: grid;
  justify-items: center;
  gap: 26px;
}

.login-form {
  position: relative;
  width: min(520px, 100%);
  padding: 58px 42px 38px;
  border: 1px solid rgba(199, 216, 239, 0.95);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow:
    0 26px 70px rgba(45, 89, 148, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(22px);
}

.login-step {
  position: absolute;
  top: 28px;
  right: 30px;
  color: #9aa8bc;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 2px;
}

.form-title {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-bottom: 30px;
  margin-bottom: 36px;
  border-bottom: 1px solid #e1ebf7;
  box-shadow: 0 18px 24px -22px rgba(41, 114, 255, 0.7);
}

.form-logo {
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
  border-radius: 15px;
}

.form-title h2 {
  margin: 0;
  color: #17243a;
  font-size: 28px;
  font-weight: 950;
  line-height: 1.1;
}

.form-title p {
  margin: 8px 0 0;
  color: #7a8a9f;
  font-size: 13px;
  font-weight: 700;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-form-item__label) {
  color: #1b2c45;
  font-size: 13px;
  font-weight: 900;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 50px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d6e1ef inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2680ff inset, 0 0 0 4px rgba(38, 128, 255, 0.1);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 28px;
}

.form-options button {
  border: 0;
  color: #1d75ff;
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 850;
}

.login-button {
  width: 100%;
  height: 56px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #1672ff 0%, #53bdff 100%);
  box-shadow: 0 18px 30px rgba(29, 117, 255, 0.22);
  font-size: 18px;
  font-weight: 900;
}

.login-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.secure-note {
  justify-content: center;
  gap: 18px;
  margin-top: 30px;
  color: #8797ad;
  font-size: 12px;
  font-weight: 800;
}

.secure-note span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.card-caption {
  margin: 0;
  color: #8394ad;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 7px;
  text-align: center;
}

@media (max-width: 1180px) {
  .login-page {
    padding: 96px 26px 38px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    gap: 34px;
  }

  .login-intro {
    padding-left: 0;
  }

  .agent-map {
    margin-right: auto;
    margin-left: auto;
  }

  .feature-strip {
    justify-content: center;
    margin-left: 0;
  }
}

@media (max-width: 760px) {
  .login-topbar {
    height: auto;
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    padding: 14px 18px;
  }

  .system-state {
    letter-spacing: 2px;
  }

  .login-page {
    padding: 120px 14px 28px;
  }

  .intro-copy h1 {
    font-size: 36px;
  }

  .intro-copy p {
    font-size: 14px;
  }

  .agent-map {
    display: none;
  }

  .feature-strip {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
    margin-top: 24px;
  }

  .feature-strip > i {
    display: none;
  }

  .login-form {
    padding: 54px 22px 30px;
  }

  .form-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .secure-note {
    flex-wrap: wrap;
  }
}
</style>
