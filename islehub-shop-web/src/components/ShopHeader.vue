<template>
  <header class="shop-header">
    <div class="header-left">
      <button type="button" class="logo" @click="goHome">IsleHub 商城</button>
    </div>
    <div class="header-center">
      <div class="search-bar">
        <input v-model="keyword" placeholder="搜索商品..." @keyup.enter="search" />
        <button type="button" @click="search">搜索</button>
      </div>
    </div>
    <div class="header-right">
      <template v-if="user">
        <router-link to="/cart" class="cart-btn"><ShoppingCart class="shop-icon" />购物车</router-link>
        <el-dropdown>
          <span class="user-name">{{user.username }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/orders')"><Document class="menu-icon" />我的订单</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/profile')"><User class="menu-icon" />个人资料</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/account-settings')"><Setting class="menu-icon" />账户设置</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/address')"><Location class="menu-icon" />地址管理</el-dropdown-item>
              <el-dropdown-item @click="logout"><SwitchButton class="menu-icon" />退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <template v-else>
        <router-link to="/login" class="login-link">登录</router-link>
        <router-link to="/register" class="register-btn">注册</router-link>
      </template>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Document, Location, Setting, ShoppingCart, SwitchButton, User } from '@element-plus/icons-vue'
import { getInfo } from '../api/auth'

const router = useRouter()
const route = useRoute()
const user = ref(null)
const keyword = ref(String(route.query.keyword || ''))

onMounted(async () => {
  if (localStorage.getItem('shop-token')) {
    try { user.value = (await getInfo()).data } catch {}
  }
  window.addEventListener('app-user-updated', handleUserUpdated)
})

onUnmounted(() => {
  window.removeEventListener('app-user-updated', handleUserUpdated)
})

watch(() => route.query.keyword, value => {
  keyword.value = String(value || '')
})

function goHome() {
  if (route.path === '/' && Object.keys(route.query).length === 0) {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } else {
    router.push('/')
  }
}

function search() {
  const value = keyword.value.trim()
  if (value) {
    router.push({ path: '/', query: { keyword: value } })
  } else {
    router.push('/')
  }
}

function logout() {
  localStorage.removeItem('shop-token')
  user.value = null
  window.dispatchEvent(new Event('app-user-logout'))
  router.push('/')
}

function handleUserUpdated(event) {
  user.value = event.detail || user.value
}
</script>

<style scoped>
.shop-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 40px; height: 64px; background: rgba(255,255,255,.94);
  border-bottom: 1px solid var(--shop-border);
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 18px rgba(31,41,55,.05); position: sticky; top: 0; z-index: 100;
}
.header-left { display: flex; align-items: center; width: 200px; flex-shrink: 0; }
.header-center { flex: 1; display: flex; justify-content: center; }
.header-right { display: flex; align-items: center; gap: 20px; width: 200px; flex-shrink: 0; justify-content: flex-end; }
.logo { padding: 0; border: 0; background: transparent; font-size: 22px; font-weight: 800; color: var(--shop-primary); cursor: pointer; letter-spacing: .5px; }
.logo:hover { opacity: 0.75; }
.search-bar { display: flex; width: 100%; max-width: 480px; }
.search-bar input { flex: 1; min-width: 0; padding: 9px 14px; border: 1px solid var(--shop-border); border-radius: var(--shop-radius-sm) 0 0 var(--shop-radius-sm); outline: none; background: #fff; color: var(--shop-text); }
.search-bar input:focus { border-color: var(--shop-primary); box-shadow: var(--shop-focus); }
.search-bar button { padding: 9px 18px; background: var(--shop-primary); color: #fff; border: none; border-radius: 0 var(--shop-radius-sm) var(--shop-radius-sm) 0; cursor: pointer; transition: background var(--shop-transition); font-weight: 600; }
.search-bar button:hover { background: var(--shop-primary-hover); }
.header-right { display: flex; align-items: center; gap: 20px; }
.cart-btn { display: inline-flex; align-items: center; gap: 6px; text-decoration: none; color: var(--shop-text); font-size: 14px; font-weight: 600; }
.cart-btn:hover { color: var(--shop-primary); }
.login-link { text-decoration: none; color: var(--shop-primary); font-weight: 600; }
.register-btn { padding: 7px 16px; background: var(--shop-primary); color: #fff; border-radius: var(--shop-radius-sm); text-decoration: none; font-size: 14px; font-weight: 600; }
.register-btn:hover { color: #fff; background: var(--shop-primary-hover); }
.user-name { cursor: pointer; color: var(--shop-primary); font-weight: 600; }
.menu-icon { width: 14px; height: 14px; margin-right: 6px; }
</style>
