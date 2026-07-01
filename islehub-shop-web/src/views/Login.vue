<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @keyup.enter="handleLogin">
        <el-form-item prop="username"><el-input v-model="form.username" placeholder="邮箱" autocomplete="email" /></el-form-item>
        <el-form-item prop="password"><el-input v-model="form.password" type="password" placeholder="密码" autocomplete="current-password" show-password /></el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="submitting" @click="handleLogin">
            {{ submitting ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p class="switch-text">还没有账号？<router-link to="/register">去注册</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'

const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '' })
const formRef = ref(null)
const submitting = ref(false)
const rules = {
  username: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (submitting.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await login({
      username: form.username.trim(),
      password: form.password
    })
    localStorage.setItem('shop-token', res.data)
    ElMessage.success('登录成功')
    const redirect = String(route.query.redirect || '/')
    router.push(redirect.startsWith('/') ? redirect : '/')
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  }
  finally { submitting.value = false }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px;
  background:
    radial-gradient(circle at 50% 0%, rgba(37,99,235,.10), transparent 30%),
    var(--shop-bg);
}

.auth-card {
  width: 380px;
  padding: 40px;
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  box-shadow: var(--shop-shadow);
}

.auth-card h2 {
  margin: 0 0 26px;
  text-align: center;
  color: var(--shop-text);
  font-size: 24px;
  font-weight: 800;
}

.auth-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.switch-text {
  margin: 12px 0 0;
  text-align: center;
  color: var(--shop-text-subtle);
  font-size: 14px;
}

.switch-text a {
  color: var(--shop-primary);
  font-weight: 700;
}
</style>
