<template>
  <div class="home-container">
    <!-- ====== 三栏主区域 ====== -->
    <section class="hero-section">

      <!-- 左栏：分类导航（悬停展开子分类） -->
      <aside class="hero-left">
        <div class="category-nav">
          <h4 class="nav-title">分类</h4>
          <nav class="cat-list">
            <a href="#" :class="{ active: !route.query.categoryId }"
               @click.prevent="selectCat(null)">
              <HomeFilled class="cat-icon" /><span>主页</span>
            </a>
            <PageSkeleton v-if="categoriesLoading" variant="category-nav" :rows="8" />
            <template v-else v-for="cat in categories" :key="cat.id">
              <!-- 无子分类 → 直接可点击 -->
              <a v-if="!cat.children || !cat.children.length"
                 href="#"
                 :class="{ active: Number(route.query.categoryId) === cat.id }"
                 @click.prevent="selectCat(cat.id)">
                <Folder class="cat-icon" /><span>{{ cat.name }}</span>
              </a>
              <!-- 有子分类 → 悬停弹出子面板 -->
              <div v-else class="cat-item" :class="{ active: isParentActive(cat) }">
                <a href="#" class="cat-row" @click.prevent="selectCat(cat.id)">
                  <Folder class="cat-icon" />
                  <span>{{ cat.name }}</span>
                  <span class="arrow">▸</span>
                </a>
                <!-- 子分类悬浮面板 -->
                <div class="cat-sub-panel">
                  <div class="cat-panel-header">
                    <a href="#"
                       class="cat-panel-root"
                       :class="{ active: isCategoryActive(cat) }"
                       @click.prevent="selectCat(cat.id)">
                      全部 {{ cat.name }}
                    </a>
                  </div>

                  <div class="cat-groups">
                    <section class="cat-group" v-for="sub in secondLevelCats(cat)" :key="sub.id">
                      <a href="#"
                         class="cat-group-title"
                         :class="{ active: isCategoryActive(sub) }"
                         @click.prevent="selectCat(sub.id)">
                        {{ sub.name }}
                      </a>
                      <div v-if="thirdLevelCats(sub).length" class="cat-leaf-list">
                        <a v-for="leaf in thirdLevelCats(sub)"
                           :key="leaf.id"
                           href="#"
                           class="cat-leaf"
                           :class="{ active: isCategoryActive(leaf) }"
                           @click.prevent="selectCat(leaf.id)">
                          {{ leaf.name }}
                        </a>
                      </div>
                    </section>
                  </div>
                </div>
              </div>
            </template>
          </nav>
        </div>
      </aside>

      <!-- 中栏：推荐轮播 -->
      <main class="hero-center">
        <PageSkeleton v-if="recommendLoading" variant="banner" />
        <el-carousel
          v-else-if="recommendList.length"
          :key="carouselKey"
          class="recommend-carousel"
          height="380px"
          :interval="4500"
          arrow="hover"
          trigger="click"
          indicator-position="outside"
          pause-on-hover
        >
          <el-carousel-item v-for="(item, index) in recommendList" :key="item.id || index">
            <div class="spotlight-slide">
              <div class="spotlight-copy">
                <p class="spotlight-eyebrow">{{ item.categoryName || '精选好物' }}</p>
                <h3>{{ shortName(item.name) }}</h3>
                <p class="spotlight-price">¥{{ productMinPrice(item) }}</p>
                <button type="button" class="spotlight-action" @click="goProduct(item.id)">查看详情</button>
              </div>
              <button
                type="button"
                class="spotlight-media"
                :aria-label="`查看${item.name || '商品'}详情`"
                @click="goProduct(item.id)"
              >
                <img
                  v-if="!item.imageFailed"
                  :src="item.bannerImage"
                  :alt="item.name || '推荐商品'"
                  :loading="index === 0 ? 'eager' : 'lazy'"
                  :fetchpriority="index === 0 ? 'high' : 'auto'"
                  @error="handleBannerImageError(item)"
                />
                <span v-else class="spotlight-placeholder">图片暂不可用</span>
              </button>
            </div>
          </el-carousel-item>
        </el-carousel>
        <!-- 无推荐时的占位 -->
        <div v-else class="recommend-placeholder">
          <p>暂无推荐商品</p>
        </div>
      </main>

      <!-- 右栏：用户信息 & 收货地址 -->
      <aside class="hero-right">
        <div class="user-panel">
          <PageSkeleton v-if="userPanelLoading" variant="user-panel" />
          <!-- 未登录态 -->
          <template v-else-if="!user">
            <div class="user-greeting">
              <p class="greet-text">Hi，欢迎来到 IsleHub</p>
              <div class="auth-btns">
                <router-link to="/login" class="btn-login">登录</router-link>
                <router-link to="/register" class="btn-reg">注册</router-link>
              </div>
            </div>
            <div class="panel-divider"></div>
            <ul class="quick-links">
              <li><router-link to="/orders"><Document class="link-icon" />我的订单</router-link></li>
              <li><router-link to="/cart"><ShoppingCart class="link-icon" />购物车</router-link></li>
            </ul>
          </template>
          <!-- 已登录态 -->
          <template v-else>
            <div class="user-avatar-row">
              <router-link to="/profile" class="avatar-circle" title="查看个人资料">
                <img
                  v-if="homeAvatarSrc && !homeAvatarFailed"
                  :src="homeAvatarSrc"
                  :alt="user.username || '用户头像'"
                  @error="homeAvatarFailed = true"
                />
                <span v-else>{{ avatarInitial }}</span>
              </router-link>
              <div class="user-info-text">
                <b>{{ user.username }}</b>
                <small>欢迎回来</small>
              </div>
            </div>
            <div class="panel-divider"></div>
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
            <div class="panel-divider"></div>
            <ul class="quick-links">
              <li><router-link to="/orders"><Document class="link-icon" />我的订单</router-link></li>
              <li><router-link to="/profile"><User class="link-icon" />个人资料</router-link></li>
              <li><router-link to="/cart"><ShoppingCart class="link-icon" />购物车</router-link></li>
              <li><router-link to="/address"><Location class="link-icon" />地址管理</router-link></li>
              <li><a href="#" @click.prevent="logout"><SwitchButton class="link-icon" />退出登录</a></li>
            </ul>
          </template>
        </div>
      </aside>

    </section>

    <!-- 商品网格 -->
    <PageSkeleton v-if="productsLoading" variant="product-grid" :rows="8" />
    <div v-else class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>
    <div class="empty-state" v-if="!productsLoading && !products.length">
      <p>没有找到相关商品</p>
      <router-link to="/">返回首页</router-link>
    </div>
    <div class="pagination" v-if="!productsLoading && total > pageSize">
      <el-pagination background layout="prev, pager, next"
                     :total="total" :page-size="pageSize" v-model:current-page="pageNum"
                     @current-change="fetchProducts" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Folder, HomeFilled, Location, ShoppingCart, SwitchButton, User } from '@element-plus/icons-vue'
import { pageProducts, getCategoryTree } from '../api/product'
import { getAddresses } from '../api/address'
import { getInfo } from '../api/auth'
import ProductCard from '../components/ProductCard.vue'
import PageSkeleton from '../components/PageSkeleton.vue'

const route = useRoute()
const router = useRouter()
const products = ref([])
const categories = ref([])       // 分类树数据
const user = ref(null)
const defaultAddr = ref(null)    // 默认收货地址
const categoriesLoading = ref(true)
const recommendLoading = ref(true)
const productsLoading = ref(true)
const userPanelLoading = ref(Boolean(localStorage.getItem('shop-token')))
const homeAvatarFailed = ref(false)
const homeAvatarVersion = ref(Date.now())
const avatarInitial = computed(() => (user.value?.username || user.value?.email || 'U').charAt(0).toUpperCase())
const homeAvatarSrc = computed(() => withAvatarCacheBuster(user.value?.avatar, homeAvatarVersion.value))

// ---- 轮播 ----
const recommendList = ref([])
const carouselKey = computed(() => recommendList.value.map(item => item.id || item.bannerImage).join('|') || 'empty')

function normalizeImageUrl(url) {
  const value = String(url || '').trim()
  if (!value) return ''
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) return value
  if (value.startsWith('/uploads/')) return value
  if (value.startsWith('uploads/')) return `/${value}`
  if (value.startsWith('/')) return value
  return `/${value}`
}

function handleBannerImageError(item) {
  if (!item) return
  item.imageFailed = true
}

function shortName(name){ return name? (name.length>18? name.slice(0,18)+'…':name):'' }

function productMinPrice(product) {
  const skus = product?.skus || []
  const prices = skus
    .map(sku => Number(sku.price))
    .filter(price => Number.isFinite(price) && price >= 0)
  if (!prices.length) return '--'
  return Math.min(...prices).toFixed(2)
}

function updateRecommendsFromProducts(list) {
  recommendList.value = (list || [])
    .map(product => ({
      ...product,
      bannerImage: normalizeImageUrl(product?.mainImage),
      imageFailed: false
    }))
    .filter(product => product.bannerImage)
    .slice(0, 6)
}

function goProduct(productId) {
  if (productId) router.push(`/product/${productId}`)
}

// ---- 分类选择 ----
function selectCat(catId){
  if(!catId) router.push('/')
  else router.push({path:'/',query:{categoryId:catId}})
}

const selectedCategoryId = computed(() => {
  const id = Number(route.query.categoryId)
  return Number.isFinite(id) ? id : null
})

function secondLevelCats(cat) {
  return Array.isArray(cat?.children) ? cat.children : []
}

function thirdLevelCats(cat) {
  return Array.isArray(cat?.children) ? cat.children : []
}

function isCategoryActive(cat) {
  return selectedCategoryId.value === Number(cat?.id)
}

function containsCategory(cat, id) {
  if (!id || !cat) return false
  if (Number(cat.id) === id) return true
  return (cat.children || []).some(child => containsCategory(child, id))
}

// 判断父分类是否处于选中状态（其某个子分类被选中）
function isParentActive(parent){
  return containsCategory(parent, selectedCategoryId.value)
}

// ---- 分页 ----
const pageNum = ref(1)
const total = ref(0)
const pageSize = 20

function handleLogout(){
  user.value = null
  defaultAddr.value = null
  homeAvatarFailed.value = false
}

function handleUserUpdated(event){
  user.value = event.detail || user.value
  homeAvatarFailed.value = false
  homeAvatarVersion.value = event.detail?.avatarUpdatedAt || Date.now()
}

onMounted(async()=>{
  // 监听退出登录事件（ShopHeader 退出时触发）
  window.addEventListener('app-user-logout', handleLogout)
  window.addEventListener('app-user-updated', handleUserUpdated)

  Promise.all([
    fetchCategories(),
    fetchUserPanel(),
    fetchProducts()
  ])
})
onUnmounted(()=>{
  window.removeEventListener('app-user-logout', handleLogout)
  window.removeEventListener('app-user-updated', handleUserUpdated)
})

watch(()=>route.query.categoryId,()=>{pageNum.value=1;fetchProducts()})
watch(()=>route.query.keyword,()=>{pageNum.value=1;fetchProducts()})

async function fetchProducts(){
  productsLoading.value = true
  recommendLoading.value = true
  try{
    const params={page:pageNum.value,pageSize}
    if(route.query.categoryId) params.categoryId=route.query.categoryId
    if(route.query.keyword) params.keyword=route.query.keyword
    const res=await pageProducts(params)
    const records = res.data.records || []
    products.value = records
    total.value = res.data.total
    updateRecommendsFromProducts(records)
  }catch{
    products.value = []
    total.value = 0
    updateRecommendsFromProducts([])
    ElMessage.error('加载商品失败')
  }
  finally {
    productsLoading.value = false
    recommendLoading.value = false
  }
}

async function fetchCategories(){
  categoriesLoading.value = true
  try{
    const res=await getCategoryTree()
    categories.value=res.data||[]
  }catch{}
  finally { categoriesLoading.value = false }
}

async function fetchUserPanel(){
  if(!localStorage.getItem('shop-token')){
    userPanelLoading.value = false
    return
  }
  userPanelLoading.value = true
  try{
    user.value=(await getInfo()).data
    homeAvatarFailed.value = false
    homeAvatarVersion.value = Date.now()
    await fetchDefaultAddr()
  }catch{}
  finally { userPanelLoading.value = false }
}

async function fetchDefaultAddr(){
  try{
    const res=await getAddresses()
    const list=res.data||[]
    defaultAddr.value=list.find(a=>a.isDefault===1)||list[0]||null
  }catch{}
}

function logout(){
  localStorage.removeItem('shop-token'); user.value=null; defaultAddr.value=null; homeAvatarFailed.value=false; router.push('/')
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
/* ========== 三栏布局 ========== */
.home-container { padding-bottom: 40px; }

.hero-section {
  display: flex; gap: 14px; margin-bottom: 24px;
  height: 380px;
  position: relative;
}

/* ---- 左栏：分类导航（悬停展开） ---- */
.hero-left { width: 210px; height: 100%; flex-shrink: 0; }
.category-nav {
  background: var(--shop-surface); border: 1px solid var(--shop-border); border-radius: var(--shop-radius); height: 100%;
  box-shadow: var(--shop-shadow-sm);
  padding: 14px 0;
  overflow: visible;           /* 关键：允许子面板溢出到外部 */
}
.hero-left {
  width: 210px; height: 100%; flex-shrink: 0;
  overflow: visible;           /* 取消滑动条，允许子面板溢出，修复鼠标移入面板时消失的问题 */
  border-radius: 12px;
}
.nav-title { margin: 0 18px 10px; font-size: 15px; color: var(--shop-text); letter-spacing: 0.5px; font-weight: 800; }

.cat-list > a,
.cat-item > .cat-row {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 18px; font-size: 13px; color: var(--shop-text-muted);
  text-decoration: none; transition: all 0.15s;
  cursor: pointer; position: relative;
}
.cat-list > a:hover,
.cat-item > .cat-row:hover { background: var(--shop-primary-soft); color: var(--shop-primary); }
.cat-list > a.active { background: var(--shop-primary-soft); color: var(--shop-primary); font-weight: 700; border-left: 3px solid var(--shop-primary); padding-left: 15px; }
.cat-item.active > .cat-row { color: var(--shop-primary); font-weight: 700; }

.cat-icon { width: 16px; height: 16px; text-align: center; flex-shrink: 0; }
.arrow { margin-left: auto; font-size: 10px; color: var(--shop-text-subtle); transition: transform 0.2s; }
.cat-item:hover .arrow { transform: translateX(2px); color: var(--shop-primary); }

/* ====== 悬停子分类面板 ====== */
.cat-item {
  position: static;        /* 让面板相对于 .hero-section 定位 */
}

.cat-sub-panel {
  /* 定位到左栏右侧，覆盖中栏和右栏上方 */
  position: absolute;
  top: 0;                 /* 从三栏区域顶部开始 */
  left: 210px;            /* 紧贴左栏右边缘 */
  width: calc(100% - 224px);
  height: 380px;

  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: 0 var(--shop-radius) var(--shop-radius) 0;
  box-shadow: var(--shop-shadow);
  padding: 20px 24px;

  display: flex;
  flex-direction: column;
  gap: 16px;

  z-index: 9999;

  /* 隐藏 → 显示过渡（只用 visibility+opacity，不用 pointer-events） */
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease, visibility 0.2s ease;
}

/* 鼠标在分类项上，或在面板内，都保持显示 */
.cat-item:hover .cat-sub-panel,
.cat-item:focus-within .cat-sub-panel,
.cat-sub-panel:hover {
  opacity: 1;
  visibility: visible;
}

.cat-panel-header {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--shop-border);
}
.cat-panel-root {
  display: inline-block;
  padding: 7px 16px;
  font-size: 13px;
  color: var(--shop-primary);
  text-decoration: none;
  border-radius: 20px;
  background: var(--shop-primary-soft);
  font-weight: 600;
  transition: all 0.15s;
}
.cat-panel-root:hover,
.cat-panel-root.active {
  background: var(--shop-primary);
  color: #fff;
}
.cat-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 28px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}
.cat-group {
  min-width: 0;
}
.cat-group-title {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  margin-bottom: 8px;
  color: var(--shop-text);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  text-decoration: none;
}
.cat-group-title::after {
  content: '›';
  margin-left: 6px;
  color: var(--shop-text-subtle);
  font-size: 15px;
}
.cat-group-title:hover,
.cat-group-title.active {
  color: var(--shop-primary);
}
.cat-leaf-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
}
.cat-leaf {
  display: inline-block;
  max-width: 100%;
  padding: 5px 12px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--shop-text-muted);
  text-decoration: none;
  border-radius: 16px;
  white-space: nowrap;
  background: var(--shop-surface-muted);
  transition: all 0.15s;
}
.cat-leaf:hover {
  background: var(--shop-primary-soft);
  color: var(--shop-primary);
}
.cat-leaf.active {
  background: var(--shop-primary);
  color: #fff;
  font-weight: 500;
}
/* ---- 中栏：推荐轮播 ---- */
.hero-center { flex: 1; min-width: 0; height: 100%; }
.recommend-carousel {
  height: 100%;
  border-radius: var(--shop-radius);
  overflow: hidden;
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  box-shadow: var(--shop-shadow-sm);
}
.recommend-carousel :deep(.el-carousel__container) {
  height: 340px !important;
}
.recommend-carousel :deep(.el-carousel__item) {
  height: 340px;
  overflow: hidden;
}
.recommend-carousel :deep(.el-carousel__indicators--outside) {
  height: 40px;
  line-height: 40px;
}
.recommend-carousel :deep(.el-carousel__button) {
  width: 22px;
  height: 4px;
  border-radius: 999px;
  background: var(--shop-text-muted);
}
.recommend-carousel :deep(.el-carousel__arrow) {
  background: rgba(42,63,73,.58);
}
.recommend-carousel :deep(.el-carousel__arrow:hover) {
  background: rgba(42,63,73,.76);
}
.recommend-placeholder {
  height: 100%; border-radius: var(--shop-radius); background: linear-gradient(135deg,var(--shop-surface-muted),#eef2f7);
  display: flex; align-items: center; justify-content: center;
  color: var(--shop-text-subtle); font-size: 15px; border: 1px dashed var(--shop-border-strong);
}
.spotlight-slide {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, .9fr) minmax(360px, 1.1fr);
  align-items: center;
  gap: 34px;
  padding: 40px 46px;
  color: inherit;
  background:
    radial-gradient(circle at 76% 50%, rgba(64,158,255,.13), transparent 34%),
    linear-gradient(100deg, var(--shop-surface-muted) 0%, var(--shop-surface) 44%, #eef4f8 100%);
}
.spotlight-copy {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.spotlight-eyebrow {
  margin: 0 0 14px;
  color: var(--shop-text-muted);
  font-size: 14px;
  letter-spacing: 1px;
}
.spotlight-copy h3 {
  margin: 0;
  max-width: 420px;
  color: var(--shop-text);
  font-size: 30px;
  line-height: 1.35;
  font-weight: 800;
}
.spotlight-price {
  margin: 16px 0 20px;
  color: var(--shop-price);
  font-size: 24px;
  line-height: 1;
  font-weight: 800;
}
.spotlight-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 4px;
  background: var(--shop-primary);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  transition: background var(--shop-transition), box-shadow var(--shop-transition);
}
.spotlight-action:hover {
  background: var(--shop-primary-hover);
}
.spotlight-media {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 260px;
  display:flex; align-items:center; justify-content:center;
  padding: 14px;
  border: 1px solid rgba(211,218,226,.82);
  cursor: pointer;
  background: linear-gradient(135deg, var(--shop-surface), var(--shop-surface-muted));
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 28px rgba(42,63,73,.13);
  transition: border-color var(--shop-transition), box-shadow var(--shop-transition), transform var(--shop-transition);
}
.spotlight-media:hover {
  border-color: rgba(37,99,235,.3);
  box-shadow: 0 14px 34px rgba(42,63,73,.16);
}
.spotlight-media img {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: contain;
  opacity: 1;
  pointer-events: none;
}
.spotlight-placeholder {
  width: 100%;
  height: 100%;
  display:flex;
  align-items:center;
  justify-content:center;
  border: 1px dashed var(--shop-border-strong);
  border-radius: 10px;
  color:var(--shop-text-subtle);
  font-size:14px;
  background: rgba(255,255,255,.68);
}

/* ---- 右栏：用户面板 ---- */
.hero-right { width: 250px; height: 100%; flex-shrink: 0; }
.user-panel {
  background: var(--shop-surface); border: 1px solid var(--shop-border); border-radius: var(--shop-radius); height: 100%;
  box-shadow: var(--shop-shadow-sm); padding: 18px 16px;
  display:flex; flex-direction:column; gap:4px; overflow-y:auto;
}
.user-greeting { text-align:center; padding-top:10px; }
.greet-text { font-size:15px; color:var(--shop-text-muted); margin:0 0 14px; }
.auth-btns { display:flex; gap:10px; justify-content:center; }
.btn-login {
  padding:7px 22px; background:var(--shop-primary); color:#fff; border-radius:20px;
  text-decoration:none; font-size:13px; font-weight:500; transition:all .2s;
}
.btn-login:hover { background:var(--shop-primary-hover); color:#fff; }
.btn-reg {
  padding:7px 22px; background:#fff; color:var(--shop-primary); border:1px solid var(--shop-primary); border-radius:20px;
  text-decoration:none; font-size:13px; font-weight:500; transition:all .2s;
}
.btn-reg:hover { background:var(--shop-primary-soft); }

.user-avatar-row { display:flex; align-items:center; gap:12px; }
.avatar-circle {
  width:42px; height:42px; border-radius:50%; background:linear-gradient(135deg,var(--shop-primary),var(--shop-success));
  color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:bold; flex-shrink:0;
  overflow:hidden; text-decoration:none; cursor:pointer; transition:transform .15s, box-shadow .15s;
}
.avatar-circle:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37,99,235,.22);
}
.avatar-circle img {
  width:100%; height:100%; object-fit:cover;
}
.avatar-circle span {
  display:flex; align-items:center; justify-content:center; width:100%; height:100%;
}
.user-info-text b { display:block; font-size:15px; color:var(--shop-text); }
.user-info-text small { color:var(--shop-text-subtle); font-size:11px; }

.panel-divider { height:1px; background:var(--shop-border); margin:10px 0; }

.addr-card { padding: 8px 4px; }
.addr-label { display:flex; align-items:center; gap:5px; font-size:12px; color:var(--shop-text-subtle); margin:0 0 6px; }
.addr-name { font-size:13px; color:var(--shop-text); margin:0 0 4px; font-weight:700; }
.addr-detail { font-size:12px; color:var(--shop-text-muted); margin:0; line-height:1.45; word-break:break-all; }
.addr-empty { text-align:center; padding:12px 0; }
.addr-empty p { display:flex; align-items:center; justify-content:center; gap:5px; font-size:13px; color:var(--shop-text-subtle); margin:0 0 8px; }
.link-add { font-size:13px; color:var(--shop-primary); text-decoration:none; font-weight:600; }

.quick-links { list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:2px; }
.quick-links li a {
  display:flex;
  align-items:center;
  gap:8px;
  padding:7px 8px;
  color:var(--shop-text-muted);
  font-size:13px;
  text-decoration:none;
  border-radius:var(--shop-radius-sm);
  transition:background var(--shop-transition), color var(--shop-transition);
}
.quick-links li a:hover { background:var(--shop-primary-soft); color:var(--shop-primary); }
.link-icon,
.inline-icon { width:14px; height:14px; flex-shrink:0; }

/* ---- 商品网格 ---- */
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.empty-state { text-align:center; padding:60px 0; color:var(--shop-text-muted); background:var(--shop-surface); border:1px dashed var(--shop-border-strong); border-radius:var(--shop-radius); }
.empty-state p { font-size:16px; margin-bottom:12px; }
.empty-state a { color:var(--shop-primary); text-decoration:none; font-weight:600; }
.pagination { display:flex; justify-content:center; margin-top:30px; }
</style>
