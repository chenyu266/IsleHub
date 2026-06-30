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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { login } from '../api/auth'

const router = useRouter()
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
    router.push('/')
  } catch { /* 错误消息已由请求拦截器统一处理 */ }
  finally { submitting.value = false }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f7fa; }
.auth-card { width: 380px; padding: 40px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.auth-card h2 { text-align: center; margin-bottom: 24px; color: #333; }
.switch-text { text-align: center; color: #999; font-size: 14px; }
.switch-text a { color: #409eff; }
</style>
