<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-left">
        <h2>IsleHub</h2>
        <el-menu
          mode="horizontal"
          :default-active="activeTopMenu"
          @select="handleTopSelect"
          class="top-menu"
        >
          <el-menu-item index="dashboard">仪表盘</el-menu-item>
          <el-menu-item index="user">用户管理</el-menu-item>
          <el-menu-item index="product">商品管理</el-menu-item>
          <el-menu-item index="order">订单管理</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <span>{{ userInfo?.realName }}</span>
        <el-button type="danger" text @click="logout">退出</el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px" class="sidebar">
        <el-menu :default-active="activeSideMenu" @select="handleSideSelect">
          <template v-if="activeTopMenu === 'dashboard'">
            <el-menu-item index="/dashboard">工作台</el-menu-item>
          </template>
          <template v-if="activeTopMenu === 'user'">
            <el-menu-item index="/user">用户列表</el-menu-item>
          </template>
          <template v-if="activeTopMenu === 'product'">
            <el-menu-item index="/product">商品列表</el-menu-item>
            <el-menu-item index="/product/category">分类管理</el-menu-item>
          </template>
          <template v-if="activeTopMenu === 'order'">
            <el-menu-item index="/order">订单列表</el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getInfo } from '../api/user'

const router = useRouter()
const route = useRoute()
const userInfo = ref(null)

const activeTopMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/user')) return 'user'
  if (path.startsWith('/product')) return 'product'
  if (path.startsWith('/order')) return 'order'
  return 'dashboard'
})

const activeSideMenu = computed(() => {
  if (route.path.startsWith('/product/category')) return '/product/category'
  return '/' + route.path.split('/')[1]
})

function handleTopSelect(index) {
  router.push('/' + index)
}

function handleSideSelect(index) {
  router.push(index)
}

async function fetchInfo() {
  try { userInfo.value = (await getInfo()).data } catch {}
}

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(fetchInfo)
</script>

<style scoped>
.layout { height: 100vh; user-select: none;}
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e4e7ed; padding: 0 20px; }
.header-left { display: flex; align-items: center; }
.header-left h2 { margin-right: 30px; color: #409eff; font-size: 20px; }
.top-menu { border-bottom: none !important; }
.header-right { display: flex; align-items: center; gap: 15px; }
.sidebar { background: #fafafa; border-right: 1px solid #e4e7ed; }
</style>
