<template>
  <div class="profile-page">
    <section class="profile-card">
      <div class="avatar-column">
        <div class="avatar-wrap">
          <img v-if="avatarSrc && !avatarFailed" :src="avatarSrc" :alt="user?.username || '用户头像'" @error="avatarFailed = true" />
          <span v-else>{{ avatarInitial }}</span>
        </div>
        <label class="avatar-button">
          <input type="file" accept="image/*" :disabled="avatarUploading" @change="handleAvatarChange" />
          {{ avatarUploading ? '上传中...' : '更换头像' }}
        </label>
      </div>

      <div class="profile-content">
        <div class="profile-heading">
          <div>
            <h2>个人资料</h2>
            <p>{{ user?.email || '未绑定邮箱' }}</p>
          </div>
          <router-link to="/account-settings" class="settings-link">账户设置</router-link>
        </div>

        <el-form ref="usernameFormRef" class="username-form" :model="usernameForm" :rules="usernameRules">
          <el-form-item prop="username">
            <el-input
              v-model="usernameForm.username"
              maxlength="32"
              show-word-limit
              placeholder="请输入 3-32 位用户名"
              @keyup.enter="saveUsername"
            />
          </el-form-item>
          <el-button type="primary" :loading="usernameSaving" @click="saveUsername">保存用户名</el-button>
        </el-form>
      </div>
    </section>

    <el-dialog
      v-model="avatarCropVisible"
      title="裁剪头像"
      width="520px"
      :close-on-click-modal="!avatarUploading"
      :close-on-press-escape="!avatarUploading"
      :show-close="!avatarUploading"
      @closed="resetAvatarCrop"
    >
      <div class="avatar-cropper">
        <div class="crop-stage" @mousedown="startCropDrag" @wheel.prevent="handleCropWheel">
          <img
            v-if="cropImageUrl"
            ref="cropImageRef"
            :src="cropImageUrl"
            :style="cropImageStyle"
            draggable="false"
            alt="待裁剪头像"
            @load="handleCropImageLoad"
          />
          <div class="crop-ring"></div>
        </div>
        <div class="crop-control">
          <span>缩放</span>
          <input
            v-model.number="cropScale"
            type="range"
            min="1"
            max="3"
            step="0.01"
            @input="clampCropPosition"
          />
        </div>
      </div>
      <template #footer>
        <el-button :disabled="avatarUploading" @click="avatarCropVisible = false">取消</el-button>
        <el-button type="primary" :loading="avatarUploading" :disabled="!cropReady" @click="confirmAvatarCrop">
          确认上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import {
  getInfo,
  updateUsername,
  uploadAvatar
} from '../api/auth'

const CROP_STAGE_SIZE = 320
const CROP_OUTPUT_SIZE = 512

const user = ref(null)
const avatarFailed = ref(false)
const avatarUploading = ref(false)
const avatarCropVisible = ref(false)
const avatarVersion = ref(Date.now())
const usernameSaving = ref(false)
let cropObjectUrl = ''
let cropDragState = null

const usernameFormRef = ref(null)
const cropImageRef = ref(null)

const usernameForm = reactive({ username: '' })
const cropImage = reactive({
  name: 'avatar.jpg',
  width: 0,
  height: 0
})
const cropOffset = reactive({
  x: 0,
  y: 0
})
const cropScale = ref(1)
const cropImageUrl = ref('')

const avatarInitial = computed(() => (user.value?.username || user.value?.email || 'U').charAt(0).toUpperCase())
const avatarSrc = computed(() => withAvatarCacheBuster(user.value?.avatar, avatarVersion.value))
const cropReady = computed(() => cropImage.width > 0 && cropImage.height > 0 && Boolean(cropImageUrl.value))
const cropBaseScale = computed(() => {
  if (!cropImage.width || !cropImage.height) return 1
  return Math.max(CROP_STAGE_SIZE / cropImage.width, CROP_STAGE_SIZE / cropImage.height)
})
const cropDisplaySize = computed(() => {
  const scale = cropBaseScale.value * cropScale.value
  return {
    width: cropImage.width * scale,
    height: cropImage.height * scale
  }
})
const cropImageStyle = computed(() => ({
  width: `${cropDisplaySize.value.width}px`,
  height: `${cropDisplaySize.value.height}px`,
  left: `calc(50% + ${cropOffset.x}px)`,
  top: `calc(50% + ${cropOffset.y}px)`
}))

const usernameRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度需为 3-32 位', trigger: ['blur', 'change'] }
  ]
}

onMounted(loadUser)
onUnmounted(() => {
  removeCropDragListeners()
  releaseCropObjectUrl()
})

async function loadUser() {
  const res = await getInfo()
  user.value = res.data
  usernameForm.username = res.data?.username || ''
  avatarFailed.value = false
  avatarVersion.value = Date.now()
}

function emitUserUpdated() {
  window.dispatchEvent(new CustomEvent('app-user-updated', {
    detail: {
      ...user.value,
      avatarUpdatedAt: avatarVersion.value
    }
  }))
}

async function refreshUser() {
  await loadUser()
  emitUserUpdated()
}

async function saveUsername() {
  if (usernameSaving.value) return
  const valid = await usernameFormRef.value?.validate().catch(() => false)
  if (!valid) return
  usernameSaving.value = true
  try {
    await updateUsername(usernameForm.username.trim())
    await refreshUser()
    ElMessage.success('用户名已更新')
  } catch {
    ElMessage.error('用户名更新失败')
  } finally {
    usernameSaving.value = false
  }
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || avatarUploading.value) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning('请选择 8MB 以内的图片')
    return
  }
  openAvatarCrop(file)
}

function openAvatarCrop(file) {
  releaseCropObjectUrl()
  cropObjectUrl = URL.createObjectURL(file)
  cropImageUrl.value = cropObjectUrl
  cropImage.name = file.name || 'avatar.jpg'
  cropImage.width = 0
  cropImage.height = 0
  cropScale.value = 1
  cropOffset.x = 0
  cropOffset.y = 0
  avatarCropVisible.value = true
}

function handleCropImageLoad(event) {
  cropImage.width = event.target.naturalWidth
  cropImage.height = event.target.naturalHeight
  cropScale.value = 1
  cropOffset.x = 0
  cropOffset.y = 0
  clampCropPosition()
}

function startCropDrag(event) {
  if (!cropReady.value || avatarUploading.value) return
  cropDragState = {
    startX: event.clientX,
    startY: event.clientY,
    startOffsetX: cropOffset.x,
    startOffsetY: cropOffset.y
  }
  window.addEventListener('mousemove', handleCropDrag)
  window.addEventListener('mouseup', stopCropDrag)
}

function handleCropDrag(event) {
  if (!cropDragState) return
  cropOffset.x = cropDragState.startOffsetX + event.clientX - cropDragState.startX
  cropOffset.y = cropDragState.startOffsetY + event.clientY - cropDragState.startY
  clampCropPosition()
}

function stopCropDrag() {
  cropDragState = null
  removeCropDragListeners()
}

function removeCropDragListeners() {
  window.removeEventListener('mousemove', handleCropDrag)
  window.removeEventListener('mouseup', stopCropDrag)
}

function handleCropWheel(event) {
  if (!cropReady.value || avatarUploading.value) return
  const nextScale = cropScale.value + (event.deltaY > 0 ? -0.06 : 0.06)
  cropScale.value = Math.min(3, Math.max(1, Number(nextScale.toFixed(2))))
  clampCropPosition()
}

function clampCropPosition() {
  const displaySize = cropDisplaySize.value
  const maxX = Math.max(0, (displaySize.width - CROP_STAGE_SIZE) / 2)
  const maxY = Math.max(0, (displaySize.height - CROP_STAGE_SIZE) / 2)
  cropOffset.x = Math.min(maxX, Math.max(-maxX, cropOffset.x))
  cropOffset.y = Math.min(maxY, Math.max(-maxY, cropOffset.y))
}

async function confirmAvatarCrop() {
  if (!cropReady.value || avatarUploading.value) return
  avatarUploading.value = true
  try {
    const file = await createCroppedAvatarFile()
    const res = await uploadAvatar(file)
    if (user.value) user.value.avatar = res.data || user.value.avatar
    avatarVersion.value = Date.now()
    avatarFailed.value = false
    emitUserUpdated()
    avatarCropVisible.value = false
    ElMessage.success('头像已更新')
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function createCroppedAvatarFile() {
  const imageEl = cropImageRef.value
  if (!imageEl) throw new Error('图片未加载')

  clampCropPosition()
  const scale = cropBaseScale.value * cropScale.value
  const displaySize = cropDisplaySize.value
  const imageLeft = (CROP_STAGE_SIZE - displaySize.width) / 2 + cropOffset.x
  const imageTop = (CROP_STAGE_SIZE - displaySize.height) / 2 + cropOffset.y
  const sourceX = Math.max(0, -imageLeft / scale)
  const sourceY = Math.max(0, -imageTop / scale)
  const sourceSize = CROP_STAGE_SIZE / scale

  const canvas = document.createElement('canvas')
  canvas.width = CROP_OUTPUT_SIZE
  canvas.height = CROP_OUTPUT_SIZE
  const context = canvas.getContext('2d')
  context.fillStyle = '#fff'
  context.fillRect(0, 0, CROP_OUTPUT_SIZE, CROP_OUTPUT_SIZE)
  context.drawImage(
    imageEl,
    sourceX,
    sourceY,
    sourceSize,
    sourceSize,
    0,
    0,
    CROP_OUTPUT_SIZE,
    CROP_OUTPUT_SIZE
  )

  const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9))
  if (!blob) throw new Error('头像裁剪失败')
  return new File([blob], normalizeAvatarFileName(cropImage.name), { type: 'image/jpeg' })
}

function normalizeAvatarFileName(name) {
  const baseName = String(name || 'avatar').replace(/\.[^.]+$/, '') || 'avatar'
  return `${baseName}.jpg`
}

function resetAvatarCrop() {
  if (avatarUploading.value) return
  releaseCropObjectUrl()
  removeCropDragListeners()
  cropDragState = null
  cropImageUrl.value = ''
  cropImage.width = 0
  cropImage.height = 0
  cropScale.value = 1
  cropOffset.x = 0
  cropOffset.y = 0
}

function releaseCropObjectUrl() {
  if (cropObjectUrl) {
    URL.revokeObjectURL(cropObjectUrl)
    cropObjectUrl = ''
  }
}

function withAvatarCacheBuster(url, version) {
  if (!url) return ''
  const value = String(url)
  if (value.startsWith('data:') || value.startsWith('blob:')) return value
  const hashIndex = value.indexOf('#')
  const base = hashIndex >= 0 ? value.slice(0, hashIndex) : value
  const hash = hashIndex >= 0 ? value.slice(hashIndex) : ''
  return `${base}${base.includes('?') ? '&' : '?'}v=${version}${hash}`
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.profile-card {
  display: flex;
  align-items: center;
  gap: 28px;
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  padding: 30px;
  box-shadow: var(--shop-shadow-sm);
}
.avatar-column {
  width: 120px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.avatar-wrap {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--shop-primary), var(--shop-success));
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(37, 99, 235, .2);
}
.avatar-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 14px;
  background: var(--shop-primary);
  color: #fff;
  border-radius: var(--shop-radius-sm);
  font-size: 13px;
  cursor: pointer;
  font-weight: 700;
  transition: background var(--shop-transition), opacity var(--shop-transition);
}
.avatar-button:hover {
  background: var(--shop-primary-hover);
}
.avatar-button input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.profile-content {
  flex: 1;
  min-width: 0;
}
.profile-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}
.profile-heading h2 {
  margin: 0 0 8px;
  color: var(--shop-text);
  font-size: 24px;
}
.profile-heading p {
  margin: 0;
  color: var(--shop-text-subtle);
  font-size: 14px;
}
.settings-link {
  flex-shrink: 0;
  color: var(--shop-primary);
  font-size: 14px;
  text-decoration: none;
  font-weight: 700;
}
.settings-link:hover {
  color: var(--shop-primary-hover);
}
.username-form {
  display: grid;
  grid-template-columns: minmax(260px, 420px) auto;
  gap: 12px;
  align-items: flex-start;
}
.username-form :deep(.el-form-item) {
  margin-bottom: 0;
}
.username-form .el-button {
  width: 112px;
}
.avatar-cropper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  padding: 4px 0 8px;
}
.crop-stage {
  position: relative;
  width: 320px;
  height: 320px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--shop-surface-muted);
  box-shadow: inset 0 0 0 1px rgba(31,41,55,.08);
  cursor: grab;
}
.crop-stage:active {
  cursor: grabbing;
}
.crop-stage img {
  position: absolute;
  transform: translate(-50%, -50%);
  max-width: none;
  user-select: none;
  pointer-events: none;
}
.crop-ring {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(255,255,255,.95);
  border-radius: 50%;
  box-shadow:
    inset 0 0 0 1px rgba(0,0,0,.16),
    0 0 0 999px rgba(0,0,0,.18);
  pointer-events: none;
}
.crop-control {
  width: 320px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--shop-text-muted);
  font-size: 13px;
}
.crop-control input {
  flex: 1;
  accent-color: var(--shop-primary);
}
</style>
