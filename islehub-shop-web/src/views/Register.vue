<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册</h2>
      <el-form :model="form" label-width="0">
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item>
          <div style="display:flex;gap:8px">
            <el-input v-model="form.emailConfirmCode" placeholder="验证码" style="flex:1" />
            <el-button :disabled="countdown > 0" @click="sendCode" style="width:130px">
              {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" placeholder="密码 (6-20位，含字母+数字)" show-password /></el-form-item>
        <el-form-item><el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password /></el-form-item>
        <el-form-item><el-button type="primary" style="width:100%" @click="handleRegister">注 册</el-button></el-form-item>
      </el-form>
      <p class="switch-text">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendEmailCode } from '../api/auth'

const router = useRouter()
const form = reactive({
  email: '',
  emailConfirmCode: '',
  username: '',
  password: '',
  confirmPassword: ''
})
const countdown = ref(0)

async function sendCode() {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  try {
    await sendEmailCode(form.email)
    ElMessage.success('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch { /* 错误消息已由拦截器处理 */ }
}

async function handleRegister() {
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch { /* 错误消息已由拦截器处理 */ }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f7fa; }
.auth-card { width: 420px; padding: 40px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.auth-card h2 { text-align: center; margin-bottom: 24px; color: #333; }
.switch-text { text-align: center; color: #999; font-size: 14px; }
.switch-text a { color: #409eff; }
</style>
