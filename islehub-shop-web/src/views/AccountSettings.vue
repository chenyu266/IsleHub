<template>
  <div class="account-settings-page">
    <div class="settings-header">
      <div>
        <h2>账户设置</h2>
        <p>管理登录密码和绑定邮箱</p>
      </div>
      <router-link to="/profile" class="profile-link">返回个人资料</router-link>
    </div>

    <div class="settings-grid">
      <section class="settings-section">
        <h3>修改密码</h3>
        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="88px">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              autocomplete="new-password"
              show-password
              placeholder="6-20 位"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              autocomplete="new-password"
              show-password
              @keyup.enter="savePassword"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="passwordSaving" @click="savePassword">保存密码</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="settings-section">
        <h3>换绑邮箱</h3>
        <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="100px">
          <el-form-item label="当前邮箱">
            <el-input :model-value="user?.email || ''" disabled />
          </el-form-item>
          <el-form-item label="验证邮箱" prop="oldCode">
            <div class="code-line">
              <el-input
                v-model="emailForm.oldCode"
                maxlength="6"
                placeholder="请输入当前邮箱验证码"
                @keyup.enter="goToChangeEmail"
              />
              <el-button :disabled="emailCodeCountdown > 0 || emailCodeSending" :loading="emailCodeSending" @click="sendCurrentEmailCode">
                {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="emailVerifying" @click="goToChangeEmail">下一步</el-button>
          </el-form-item>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getInfo,
  sendChangeEmailCode,
  updatePassword,
  verifyOldEmailCode
} from '../api/auth'

const EMAIL_CHANGE_VERIFIED_KEY = 'islehub-email-change-old-verified'

const router = useRouter()
const user = ref(null)
const passwordSaving = ref(false)
const emailVerifying = ref(false)
const emailCodeSending = ref(false)
const emailCodeCountdown = ref(0)
let emailCodeTimer = null

const passwordFormRef = ref(null)
const emailFormRef = ref(null)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const emailForm = reactive({
  oldCode: ''
})

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码长度需为 6-20 位', trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: ['blur', 'change'] }
  ]
}

const emailRules = {
  oldCode: [
    { required: true, message: '请输入当前邮箱验证码', trigger: 'blur' },
    { len: 6, message: '验证码为 6 位', trigger: ['blur', 'change'] }
  ]
}

onMounted(() => {
  sessionStorage.removeItem(EMAIL_CHANGE_VERIFIED_KEY)
  loadUser()
})
onUnmounted(() => {
  if (emailCodeTimer) clearInterval(emailCodeTimer)
})

async function loadUser() {
  const res = await getInfo()
  user.value = res.data
}

async function savePassword() {
  if (passwordSaving.value) return
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  passwordSaving.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    passwordFormRef.value?.clearValidate()
    ElMessage.success('密码已更新')
  } catch (error) {
    ElMessage.error(error.message || '密码更新失败')
  } finally {
    passwordSaving.value = false
  }
}

async function sendCurrentEmailCode() {
  if (emailCodeSending.value || emailCodeCountdown.value > 0) return
  emailCodeSending.value = true
  try {
    sessionStorage.removeItem(EMAIL_CHANGE_VERIFIED_KEY)
    await sendChangeEmailCode()
    ElMessage.success('验证码已发送至当前邮箱')
    startEmailCountdown()
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    emailCodeSending.value = false
  }
}

async function goToChangeEmail() {
  if (emailVerifying.value) return
  const valid = await emailFormRef.value?.validate().catch(() => false)
  if (!valid) return
  emailVerifying.value = true
  try {
    await verifyOldEmailCode(emailForm.oldCode.trim())
    sessionStorage.setItem(EMAIL_CHANGE_VERIFIED_KEY, '1')
    ElMessage.success('当前邮箱验证通过')
    router.push('/account-settings/change-email')
  } catch (error) {
    ElMessage.error(error.message || '当前邮箱验证失败')
  } finally {
    emailVerifying.value = false
  }
}

function startEmailCountdown() {
  emailCodeCountdown.value = 60
  if (emailCodeTimer) clearInterval(emailCodeTimer)
  emailCodeTimer = setInterval(() => {
    emailCodeCountdown.value--
    if (emailCodeCountdown.value <= 0) {
      clearInterval(emailCodeTimer)
      emailCodeTimer = null
    }
  }, 1000)
}

function validateConfirmPassword(_rule, value, callback) {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}
</script>

<style scoped>
.account-settings-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.settings-header,
.settings-section {
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  padding: 26px 30px;
  box-shadow: var(--shop-shadow-sm);
}
.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.settings-header h2 {
  margin: 0 0 8px;
  color: var(--shop-text);
  font-size: 24px;
}
.settings-header p {
  margin: 0;
  color: var(--shop-text-subtle);
  font-size: 14px;
}
.profile-link {
  flex-shrink: 0;
  color: var(--shop-primary);
  font-size: 14px;
  text-decoration: none;
  font-weight: 700;
}
.profile-link:hover {
  color: var(--shop-primary-hover);
}
.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.settings-section h3 {
  margin: 0 0 18px;
  color: var(--shop-text);
  font-size: 17px;
  font-weight: 800;
}
.code-line {
  display: flex;
  gap: 10px;
  width: 100%;
}
.code-line .el-input {
  min-width: 0;
}
.code-line .el-button {
  flex-shrink: 0;
  min-width: 104px;
}
@media (max-width: 860px) {
  .settings-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 560px) {
  .settings-header,
  .settings-section {
    padding: 22px 18px;
  }
  .code-line {
    flex-direction: column;
  }
  .code-line .el-button {
    width: 100%;
  }
}
</style>
