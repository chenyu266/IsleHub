<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @keyup.enter="handleRegister">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱" autocomplete="email" />
        </el-form-item>
        <el-form-item prop="emailConfirmCode">
          <div class="code-row">
            <el-input v-model="form.emailConfirmCode" placeholder="验证码" style="flex:1" />
            <el-button :disabled="countdown > 0 || codeSending" :loading="codeSending" @click="sendCode" style="width:130px">
              {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="username"><el-input v-model="form.username" placeholder="用户名" autocomplete="username" /></el-form-item>
        <el-form-item prop="password"><el-input v-model="form.password" type="password" placeholder="密码 (6-20位，含字母+数字)" autocomplete="new-password" show-password /></el-form-item>
        <el-form-item prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" autocomplete="new-password" show-password /></el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="submitting" @click="handleRegister">
            {{ submitting ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p class="switch-text">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { register, sendEmailCode } from '../api/auth'

const router = useRouter()
const form = reactive({
  email: '',
  emailConfirmCode: '',
  username: '',
  password: '',
  confirmPassword: ''
})
const formRef = ref(null)
const countdown = ref(0)
const codeSending = ref(false)
const submitting = ref(false)
let countdownTimer = null

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱', trigger: ['blur', 'change'] }
  ],
  emailConfirmCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: ['blur', 'change'] }
  ]
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

async function sendCode() {
  let emailValid = true
  try {
    await formRef.value?.validateField('email')
  } catch {
    emailValid = false
  }
  if (!emailValid) return
  if (codeSending.value || countdown.value > 0) return
  codeSending.value = true
  try {
    await sendEmailCode(form.email.trim())
    ElMessage.success('验证码已发送')
    countdown.value = 60
    if (countdownTimer) clearInterval(countdownTimer)
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  }
  finally { codeSending.value = false }
}

async function handleRegister() {
  if (submitting.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await register({
      email: form.email.trim(),
      emailConfirmCode: form.emailConfirmCode.trim(),
      username: form.username.trim(),
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
  }
  finally { submitting.value = false }
}

function validatePassword(_rule, value, callback) {
  if (!/^(?=.*[A-Za-z])(?=.*\d).{6,20}$/.test(value || '')) {
    callback(new Error('密码需为6-20位，并包含字母和数字'))
    return
  }
  callback()
}

function validateConfirmPassword(_rule, value, callback) {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
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
  width: 420px;
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

.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
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
