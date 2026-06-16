<template>
  <header class="shop-header">
    <div class="header-left">
      <router-link to="/" class="logo">IsleHub 商城</router-link>
      <div class="search-bar">
        <input v-model="keyword" placeholder="搜索商品..." @keyup.enter="search" />
        <button @click="search">搜索</button>
      </div>
    </div>
    <div class="header-right">
      <template v-if="user">
        <router-link to="/cart" class="cart-btn">🛒 购物车</router-link>
        <el-dropdown>
          <span class="user-name">{{ user.realName || user.username }}</span>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getInfo } from '../api/auth'

const router = useRouter()
const user = ref(null)
const keyword = ref('')

onMounted(async () => {
  if (localStorage.getItem('shop-token')) {
    try { user.value = (await getInfo()).data } catch {}
  }
})

function search() {
  if (keyword.value.trim()) {
    router.push({ path: '/', query: { keyword: keyword.value } })
  }
}

function logout() {
  localStorage.removeItem('shop-token')
  user.value = null
  router.push('/')
}
</script>

<style scoped>
.shop-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 40px; height: 60px; background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); position: sticky; top: 0; z-index: 100;
}
.header-left { display: flex; align-items: center; gap: 40px; }
.logo { font-size: 20px; font-weight: bold; color: #409eff; text-decoration: none; }
.search-bar { display: flex; }
.search-bar input { width: 240px; padding: 8px 12px; border: 1px solid #dcdfe6; border-radius: 4px 0 0 4px; outline: none; }
.search-bar button { padding: 8px 16px; background: #409eff; color: #fff; border: none; border-radius: 0 4px 4px 0; cursor: pointer; }
.header-right { display: flex; align-items: center; gap: 20px; }
.cart-btn { text-decoration: none; color: #333; font-size: 14px; }
.login-link { text-decoration: none; color: #409eff; }
.register-btn { padding: 6px 16px; background: #409eff; color: #fff; border-radius: 4px; text-decoration: none; font-size: 14px; }
.user-name { cursor: pointer; color: #409eff; }
</style>
