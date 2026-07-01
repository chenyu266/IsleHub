<template>
  <aside class="hero-right">
    <div class="user-panel">
      <PageSkeleton v-if="loading" variant="user-panel" />

      <!-- 未登录态 -->
      <template v-else-if="!user">
        <div class="user-greeting">
          <p class="greet-text">Hi，欢迎来到 IsleHub</p>
          <div class="auth-btns">
            <router-link to="/login" class="btn-login">登录</router-link>
            <router-link to="/register" class="btn-reg">注册</router-link>
          </div>
        </div>
        <el-divider style="margin: 10px 0" />
        <ul class="quick-links">
          <li><router-link to="/orders"><Document class="link-icon" />我的订单</router-link></li>
          <li><router-link to="/cart"><ShoppingCart class="link-icon" />购物车</router-link></li>
        </ul>
      </template>

      <!-- 已登录态 -->
      <template v-else>
        <div class="user-avatar-row">
          <router-link to="/profile" title="查看个人资料">
            <el-avatar :size="42" :src="homeAvatarSrc || undefined" @error="homeAvatarFailed = true">
              {{ avatarInitial }}
            </el-avatar>
          </router-link>
          <div class="user-info-text">
            <b>{{ user.username }}</b>
            <small>欢迎回来</small>
          </div>
        </div>
        <el-divider style="margin: 10px 0" />

        <!-- 默认收货地址 -->
        <div v-if="defaultAddr" class="addr-card">
          <p class="addr-label"><Location class="inline-icon" />默认收货地址</p>
          <p class="addr-name">{{ defaultAddr.receiverName }} {{ defaultAddr.receiverPhone }}</p>
          <p class="addr-detail">{{ defaultAddr.province }}{{ defaultAddr.city }}{{ defaultAddr.district }} {{ defaultAddr.detail }}</p>
        </div>
        <div v-else class="addr-empty">
          <p><Location class="inline-icon" />暂无收货地址</p>
          <router-link to="/address" class="link-add">去添加 →</router-link>
        </div>
        <el-divider style="margin: 10px 0" />
        <ul class="quick-links">
          <li><router-link to="/orders"><Document class="link-icon" />我的订单</router-link></li>
          <li><router-link to="/profile"><User class="link-icon" />个人资料</router-link></li>
          <li><router-link to="/cart"><ShoppingCart class="link-icon" />购物车</router-link></li>
          <li><router-link to="/address"><Location class="link-icon" />地址管理</router-link></li>
          <li><a href="#" @click.prevent="doLogout"><SwitchButton class="link-icon" />退出登录</a></li>
        </ul>
      </template>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, Location, ShoppingCart, SwitchButton, User } from '@element-plus/icons-vue'
import { getAddresses } from '../api/address'
import { getInfo } from '../api/auth'
import PageSkeleton from './PageSkeleton.vue'

const router = useRouter()
const user = ref(null)
const defaultAddr = ref(null)
const loading = ref(Boolean(localStorage.getItem('shop-token')))
const homeAvatarFailed = ref(false)
const homeAvatarVersion = ref(Date.now())

const avatarInitial = computed(() => (user.value?.username || user.value?.email || 'U').charAt(0).toUpperCase())

const homeAvatarSrc = computed(() => withAvatarCacheBuster(user.value?.avatar, homeAvatarVersion.value))

onMounted(async () => {
  window.addEventListener('app-user-logout', handleLogout)
  window.addEventListener('app-user-updated', handleUserUpdated)
  if (localStorage.getItem('shop-token')) {
    await fetchUserPanel()
  }
})

onUnmounted(() => {
  window.removeEventListener('app-user-logout', handleLogout)
  window.removeEventListener('app-user-updated', handleUserUpdated)
})

async function fetchUserPanel() {
  loading.value = true
  try {
    user.value = (await getInfo()).data
    homeAvatarFailed.value = false
    homeAvatarVersion.value = Date.now()
    await fetchDefaultAddr()
  } catch { /* 未登录静默处理 */ }
  finally { loading.value = false }
}

async function fetchDefaultAddr() {
  try {
    const res = await getAddresses()
    const list = res.data || []
    defaultAddr.value = list.find(a => a.isDefault === 1) || list[0] || null
  } catch { /* 无地址静默处理 */ }
}

function handleLogout() {
  user.value = null
  defaultAddr.value = null
  homeAvatarFailed.value = false
}

function handleUserUpdated(event) {
  user.value = event.detail || user.value
  homeAvatarFailed.value = false
  homeAvatarVersion.value = event.detail?.avatarUpdatedAt || Date.now()
}

function doLogout() {
  // 触发全局退出事件，通知其他组件
  window.dispatchEvent(new CustomEvent('app-user-logout'))
  localStorage.removeItem('shop-token')
  user.value = null
  defaultAddr.value = null
  homeAvatarFailed.value = false
  router.push('/')
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
.hero-right { width: 250px; height: 100%; flex-shrink: 0; }
.user-panel {
  background: linear-gradient(180deg, #f0f5fb 0%, var(--shop-surface) 45%);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  height: 100%;
  box-shadow: var(--shop-shadow-sm);
  padding: 18px 16px;
  display: flex; flex-direction: column; gap: 4px;
  overflow-y: auto;
}

/* ---- 未登录 ---- */
.user-greeting { text-align: center; padding-top: 10px; }
.greet-text { font-size: 15px; color: var(--shop-text-muted); margin: 0 0 14px; }
.auth-btns { display: flex; gap: 10px; justify-content: center; }
.btn-login {
  padding: 7px 22px; background: var(--shop-primary); color: #fff;
  border-radius: 20px; text-decoration: none; font-size: 13px; font-weight: 500;
  transition: all var(--shop-transition);
}
.btn-login:hover { background: var(--shop-primary-hover); }
.btn-reg {
  padding: 7px 22px; background: #fff; color: var(--shop-primary);
  border: 1px solid var(--shop-primary); border-radius: 20px;
  text-decoration: none; font-size: 13px; font-weight: 500;
  transition: all var(--shop-transition);
}
.btn-reg:hover { background: var(--shop-primary-soft); }

/* ---- 已登录：头像行 ---- */
.user-avatar-row { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.user-avatar-row :deep(.el-avatar) { flex-shrink: 0; }
.user-info-text { flex: 1; min-width: 0; }
.user-info-text b { display: block; font-size: 15px; color: var(--shop-text); }
.user-info-text small { color: var(--shop-text-subtle); font-size: 11px; }

/* ---- 地址卡片 ---- */
.addr-card { padding: 8px 4px; }
.addr-label {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; color: var(--shop-text-subtle); margin: 0 0 6px;
}
.addr-name { font-size: 13px; color: var(--shop-text); margin: 0 0 4px; font-weight: 700; }
.addr-detail {
  font-size: 12px; color: var(--shop-text-muted); margin: 0;
  line-height: 1.45; word-break: break-all;
}
.addr-empty { text-align: center; padding: 12px 0; }
.addr-empty p {
  display: flex; align-items: center; justify-content: center; gap: 5px;
  font-size: 13px; color: var(--shop-text-subtle); margin: 0 0 8px;
}
.link-add { font-size: 13px; color: var(--shop-primary); text-decoration: none; font-weight: 600; }

/* ---- 快捷链接 ---- */
.quick-links { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 2px; }
.quick-links li a {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 8px;
  color: var(--shop-text-muted); font-size: 13px;
  text-decoration: none; border-radius: var(--shop-radius-sm);
  transition: background var(--shop-transition), color var(--shop-transition);
}
.quick-links li a:hover { background: var(--shop-primary-soft); color: var(--shop-primary); }
.link-icon, .inline-icon { width: 14px; height: 14px; flex-shrink: 0; }
</style>
