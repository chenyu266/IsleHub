<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-left">
        <h2>IsleHub</h2>
        <el-menu
          mode="horizontal"
          :default-active="activeTopMenu"
          @select="handleTopSelect"
          :ellipsis="false"
          class="top-menu"
        >
          <el-menu-item index="dashboard">仪表盘</el-menu-item>
          <el-menu-item index="user">用户管理</el-menu-item>
          <el-menu-item index="product">商品管理</el-menu-item>
          <el-menu-item index="order">订单管理</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <span>{{ userInfo && userInfo.realName }}</span>
        <el-button type="danger" text @click="logout">退出</el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside :width="sidebarWidth" class="sidebar" :class="{ 'is-collapsed': isCollapsed }">
        <!-- 侧边栏收起/展开切换按钮 -->
        <div class="sidebar-toggle" @click="toggleSidebar">
          <el-icon :size="18">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
        <el-menu :default-active="activeSideMenu" @select="handleSideSelect">
          <template v-if="activeTopMenu == 'dashboard'">
            <el-menu-item index="/dashboard">
              <el-icon><Odometer /></el-icon>
              <span>工作台</span>
            </el-menu-item>
          </template>
          <template v-if="activeTopMenu == 'user'">
            <el-menu-item index="/user">
              <el-icon><User /></el-icon>
              <span>用户列表</span>
            </el-menu-item>
          </template>
          <template v-if="activeTopMenu == 'product'">
            <el-menu-item index="/product">
              <el-icon><Goods /></el-icon>
              <span>商品列表</span>
            </el-menu-item>
            <el-menu-item index="/product/category">
              <el-icon><Files /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
          </template>
          <template v-if="activeTopMenu == 'order'">
            <el-menu-item index="/order">
              <el-icon><Document /></el-icon>
              <span>订单列表</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <el-main>
        <!-- 顶部 Tab 切换：router-view 包裹 transition，内容淡入淡出；激活高亮保留原样式不换色 -->
        <router-view v-slot="{ Component, route }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getInfo } from '../api/user'
import { Fold, Expand, Odometer, User, Goods, Files, Document } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userInfo = ref(null)

/* 侧边栏收起/展开：isCollapsed 控制状态，sidebarWidth 响应式宽度 */
const isCollapsed = ref(false)
const sidebarWidth = computed(() => isCollapsed.value ? '64px' : '200px')
function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

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
/* Logo：轻微字重区分（600），hover 柔和放大 1.05 倍 + 透明度过渡 */
  .header-left h2 {
    margin-right: 15px;
    padding-right: 15px;
    color: #409eff;
    font-size: 20px;
    font-weight: 600;
    opacity: 0.85;
    cursor: default;
    position: relative;
    transform-origin: left center;
    transition: transform 0.25s ease, opacity 0.25s ease;
  }
  .header-left h2:hover {
    transform: scale(1.05);
    opacity: 1;
  }
  /* Logo 右侧细微分隔竖线：区分品牌区与功能 Tab 区 */
  .header-left h2::after {
    content: '';
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 1px;
    height: 20px;
    background-color: #e4e7ed;
  }
.top-menu { border-bottom: none !important; }
.header-right { display: flex; align-items: center; gap: 15px; }
/* 侧边栏：宽度平滑过渡 + 收起时溢出隐藏（防止文字溢出闪现） */
.sidebar {
  background: #fafafa;
  border-right: 1px solid #e4e7ed;
  transition: width 0.28s ease;
  overflow: hidden;
}

/* 收起/展开切换按钮：顶部居中图标 */
.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  cursor: pointer;
  color: #909399;
  border-bottom: 1px solid #e4e7ed;
  transition: background-color 0.25s ease;
}
.sidebar-toggle:hover {
  background-color: #f0f2f5;
}

/* el-menu 去除默认右边框，宽度跟随 aside */
.sidebar :deep(.el-menu) {
  border-right: none;
}

/* 菜单项：padding + 底色过渡（底色沿用 EP 默认，仅加平滑过渡） */
.sidebar :deep(.el-menu-item) {
  transition: padding 0.28s ease, background-color 0.25s ease, color 0.25s ease;
}
/* 收起时菜单项：缩小 padding 使图标视觉居中 */
.sidebar.is-collapsed :deep(.el-menu-item) {
  padding-left: 22px !important;
}

/* 菜单文字：收起时淡出、展开时延迟淡入，与宽度过渡错开避免突兀闪现 */
.sidebar :deep(.el-menu-item > span) {
  transition: opacity 0.2s ease 0.05s;
  white-space: nowrap;
}
.sidebar.is-collapsed :deep(.el-menu-item > span) {
  opacity: 0;
}

/* 顶部 Tab 切换：内容区淡入淡出动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease;
}
.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>
