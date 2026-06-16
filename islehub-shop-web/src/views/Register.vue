<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册</h2>
      <el-form :model="form" label-width="0">
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" show-password /></el-form-item>
        <el-form-item><el-input v-model="form.realName" placeholder="真实姓名" /></el-form-item>
        <el-form-item><el-input v-model="form.phone" placeholder="手机号" /></el-form-item>
        <el-form-item><el-button type="primary" style="width:100%" @click="handleRegister">注 册</el-button></el-form-item>
      </el-form>
      <p class="switch-text">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'

const router = useRouter()
const form = reactive({ username: '', password: '', realName: '', phone: '' })

async function handleRegister() {
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {}
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f7fa; }
.auth-card { width: 380px; padding: 40px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.auth-card h2 { text-align: center; margin-bottom: 24px; color: #333; }
.switch-text { text-align: center; color: #999; font-size: 14px; }
.switch-text a { color: #409eff; }
</style>
