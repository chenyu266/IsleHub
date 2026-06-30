<template>
  <header class="shop-header">
    <div class="header-left">
      <span class="logo" @click="goHome">IsleHub 商城</span>
    </div>
    <div class="header-center">
      <div class="search-bar">
        <input v-model="keyword" placeholder="搜索商品..." @keyup.enter="search" />
        <button type="button" @click="search">搜索</button>
      </div>
    </div>
    <div class="header-right">
      <template v-if="user">
        <router-link to="/cart" class="cart-btn">🛒 购物车</router-link>
        <el-dropdown>
          <span class="user-name">{{user.username }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/orders')">我的订单</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/address')">地址管理</el-dropdown-item>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
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
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getInfo } from '../api/auth'

const router = useRouter()
const route = useRoute()
const user = ref(null)
const keyword = ref(String(route.query.keyword || ''))

onMounted(async () => {
  if (localStorage.getItem('shop-token')) {
    try { user.value = (await getInfo()).data } catch {}
  }
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
</script>

<style scoped>
.shop-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 40px; height: 60px; background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); position: sticky; top: 0; z-index: 100;
}
.header-left { display: flex; align-items: center; width: 200px; flex-shrink: 0; }
.header-center { flex: 1; display: flex; justify-content: center; }
.header-right { display: flex; align-items: center; gap: 20px; width: 200px; flex-shrink: 0; justify-content: flex-end; }
.logo { font-size: 22px; font-weight: bold; color: #409eff; cursor: pointer; letter-spacing: 1px; }
.logo:hover { opacity: 0.75; }
.search-bar { display: flex; width: 100%; max-width: 480px; }
.search-bar input { flex: 1; min-width: 0; padding: 8px 12px; border: 1px solid #dcdfe6; border-radius: 4px 0 0 4px; outline: none; }
.search-bar input:focus { border-color: #409eff; }
.search-bar button { padding: 8px 16px; background: #409eff; color: #fff; border: none; border-radius: 0 4px 4px 0; cursor: pointer; transition: background .2s; }
.search-bar button:hover { background: #337ecc; }
.header-right { display: flex; align-items: center; gap: 20px; }
.cart-btn { text-decoration: none; color: #333; font-size: 14px; }
.login-link { text-decoration: none; color: #409eff; }
.register-btn { padding: 6px 16px; background: #409eff; color: #fff; border-radius: 4px; text-decoration: none; font-size: 14px; }
.user-name { cursor: pointer; color: #409eff; }
</style>
