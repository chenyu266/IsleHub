<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>登录</h2>
      <el-form :model="form" label-width="0">
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" show-password /></el-form-item>
        <el-form-item><el-button type="primary" style="width:100%" @click="handleLogin">登 录</el-button></el-form-item>
      </el-form>
      <p class="switch-text">还没有账号？<router-link to="/register">去注册</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'

const router = useRouter()
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  try {
    const res = await login(form)
    localStorage.setItem('shop-token', res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch { ElMessage.error('登录失败') }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f7fa; }
.auth-card { width: 380px; padding: 40px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.auth-card h2 { text-align: center; margin-bottom: 24px; color: #333; }
.switch-text { text-align: center; color: #999; font-size: 14px; }
.switch-text a { color: #409eff; }
</style>
