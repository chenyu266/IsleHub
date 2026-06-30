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
              <span class="cat-icon">🏠</span><span>主页</span>
            </a>
            <PageSkeleton v-if="categoriesLoading" variant="category-nav" :rows="8" />
            <template v-else v-for="cat in categories" :key="cat.id">
              <!-- 无子分类 → 直接可点击 -->
              <a v-if="!cat.children || !cat.children.length"
                 href="#"
                 :class="{ active: Number(route.query.categoryId) === cat.id }"
                 @click.prevent="selectCat(cat.id)">
                <span class="cat-icon">📁</span><span>{{ cat.name }}</span>
              </a>
              <!-- 有子分类 → 悬停弹出子面板 -->
              <div v-else class="cat-item" :class="{ active: isParentActive(cat) }">
                <div class="cat-row" @click.self="selectCat(cat.id)">
                  <span class="cat-icon">📁</span>
                  <span>{{ cat.name }}</span>
                  <span class="arrow">▸</span>
                </div>
                <!-- 子分类悬浮面板 -->
                <div class="cat-sub-panel">
                  <a v-for="sub in cat.children" :key="sub.id"
                     href="#"
                     :class="{ active: Number(route.query.categoryId) === sub.id }"
                     @click.prevent="selectCat(sub.id)">
                    {{ sub.name }}
                  </a>
                </div>
              </div>
            </template>
          </nav>
        </div>
      </aside>

      <!-- 中栏：推荐轮播 -->
      <main class="hero-center">
        <PageSkeleton v-if="recommendLoading" variant="banner" />
        <div class="recommend-banner" v-else-if="recommendList.length"
             :style="{ '--bg-color': bannerBgColor }"
             @mouseenter="pauseBanner" @mouseleave="resumeBanner">
          <router-link :to="`/product/${currentRecommend.id}`" class="banner-inner">
            <div class="banner-bg">
              <img
                v-if="!currentRecommend.imageFailed"
                :src="currentRecommend.mainImage || ''"
                :alt="currentRecommend.name || '推荐商品'"
                loading="lazy"
                crossorigin="anonymous"
                @load="extractColor($event)"
                @error="handleBannerImageError"
              />
            </div>
            <div class="banner-content">
              <p class="banner-cat">{{ currentRecommend.categoryName || '精选好物' }}</p>
              <h3>{{ shortName(currentRecommend.name) }}</h3>
              <p class="banner-hint">点击查看详情 →</p>
            </div>
          </router-link>
          <div class="banner-dots">
            <span v-for="(item, i) in recommendList" :key="i"
                  :class="{ active: i === currentIndex }"
                  @click.stop="goToSlide(i)" />
          </div>
        </div>
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
              <p class="greet-text">Hi，欢迎来到 IsleHub 👋</p>
              <div class="auth-btns">
                <router-link to="/login" class="btn-login">登录</router-link>
                <router-link to="/register" class="btn-reg">注册</router-link>
              </div>
            </div>
            <div class="panel-divider"></div>
            <ul class="quick-links">
              <li><router-link to="/orders">📦 我的订单</router-link></li>
              <li><router-link to="/cart">🛒 购物车</router-link></li>
            </ul>
          </template>
          <!-- 已登录态 -->
          <template v-else>
            <div class="user-avatar-row">
              <div class="avatar-circle">{{ (user.username).charAt(0) }}</div>
              <div class="user-info-text">
                <b>{{ user.username }}</b>
                <small>欢迎回来 ✨</small>
              </div>
            </div>
            <div class="panel-divider"></div>
            <!-- 默认收货地址 -->
            <div v-if="defaultAddr" class="addr-card">
              <p class="addr-label">📍 默认收货地址</p>
              <p class="addr-name">{{ defaultAddr.receiverName }} {{ defaultAddr.receiverPhone }}</p>
              <p class="addr-detail">{{ defaultAddr.province }}{{ defaultAddr.city }}{{ defaultAddr.district }} {{ defaultAddr.detail }}</p>
            </div>
            <div v-else class="addr-empty">
              <p>📌 暂无收货地址</p>
              <router-link to="/address" class="link-add">去添加 →</router-link>
            </div>
            <div class="panel-divider"></div>
            <ul class="quick-links">
              <li><router-link to="/orders">📦 我的订单</router-link></li>
              <li><router-link to="/cart">🛒 购物车</router-link></li>
              <li><router-link to="/address">📍 地址管理</router-link></li>
              <li><a href="#" @click.prevent="logout">🚪 退出登录</a></li>
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
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
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

// ---- 轮播 ----
const recommendList = ref([])
const currentIndex = ref(0)
let timer = null
const INTERVAL = 3000
const bannerBgColor = ref('#e2e8f0')
const currentRecommend = computed(() => recommendList.value[currentIndex.value] || {})

function extractColor(e) {
  try {
    const img = e.target, canvas = document.createElement('canvas'), ctx = canvas.getContext('2d')
    const size = 30; canvas.width = canvas.height = size
    ctx.drawImage(img, 0, 0, size, size)
    const d = ctx.getImageData(0, 0, size, size).data
    let r=0,g=0,b=0,c=0; for(let i=0;i<d.length;i+=4){r+=d[i];g+=d[i+1];b+=d[i+2];c++}
    r=Math.round(r/c); g=Math.round(g/c); b=Math.round(b/c)
    const m=Math.max(r,g,b), l=(255-m)*0.5
    r=Math.round((r+l)*0.82+255*0.18); g=Math.round((g+l)*0.82+255*0.18); b=Math.round((b+l)*0.82+255*0.18)
    bannerBgColor.value=`rgb(${r},${g},${b})`
  } catch {
    bannerBgColor.value = '#e2e8f0'
  }
}

function handleBannerImageError() {
  if (currentRecommend.value) currentRecommend.value.imageFailed = true
  bannerBgColor.value = '#e2e8f0'
}

function shortName(name){ return name? (name.length>18? name.slice(0,18)+'…':name):'' }

function startTimer(){
  stopTimer()
  if (recommendList.value.length <= 1) return
  timer=setInterval(()=>{ currentIndex.value=(currentIndex.value+1)%recommendList.value.length },INTERVAL)
}
function stopTimer(){ clearInterval(timer); timer=null }
function pauseBanner(){ stopTimer() }
function resumeBanner(){ if (recommendList.value.length > 1) startTimer() }
function goToSlide(i){ currentIndex.value=i; stopTimer(); resumeBanner() }

async function fetchRecommends(){
  recommendLoading.value = true
  try{
    const res = await pageProducts({page:1,pageSize:10})
    recommendList.value=(res.data.records||[]).filter(p=>p.mainImage).slice(0,6)
    if(recommendList.value.length>1) startTimer()
  }catch{}
  finally { recommendLoading.value = false }
}

// ---- 分类选择 ----
function selectCat(catId){
  if(!catId) router.push('/')
  else router.push({path:'/',query:{categoryId:catId}})
}



// 判断父分类是否处于选中状态（其某个子分类被选中）
function isParentActive(parent){
  if(Number(route.query.categoryId)===parent.id) return true
  return parent.children?.some(s=>Number(route.query.categoryId)===s.id)
}

// ---- 分页 ----
const pageNum = ref(1)
const total = ref(0)
const pageSize = 20

function handleLogout(){
  user.value = null
  defaultAddr.value = null
}

onMounted(async()=>{
  // 监听退出登录事件（ShopHeader 退出时触发）
  window.addEventListener('app-user-logout', handleLogout)

  Promise.all([
    fetchCategories(),
    fetchUserPanel(),
    fetchRecommends(),
    fetchProducts()
  ])
})
onUnmounted(()=>{
  stopTimer()
  window.removeEventListener('app-user-logout', handleLogout)
})

watch(()=>route.query.categoryId,()=>{pageNum.value=1;fetchProducts()})
watch(()=>route.query.keyword,()=>{pageNum.value=1;fetchProducts()})

async function fetchProducts(){
  productsLoading.value = true
  try{
    const params={page:pageNum.value,pageSize}
    if(route.query.categoryId) params.categoryId=route.query.categoryId
    if(route.query.keyword) params.keyword=route.query.keyword
    const res=await pageProducts(params)
    products.value=res.data.records
    total.value=res.data.total
  }catch{ ElMessage.error('加载商品失败') }
  finally { productsLoading.value = false }
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
  localStorage.removeItem('shop-token'); user.value=null; defaultAddr.value=null; router.push('/')
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
.hero-left { width: 210px; flex-shrink: 0; }
.category-nav {
  background: #fff; border-radius: 12px; height: 90%;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  padding: 14px 0;
  overflow: visible;           /* 关键：允许子面板溢出到外部 */
}
.hero-left {
  width: 210px; flex-shrink: 0;
  overflow: visible;           /* 取消滑动条，允许子面板溢出，修复鼠标移入面板时消失的问题 */
  border-radius: 12px;
}
.nav-title { margin: 0 16px 8px; font-size: 15px; color: #333; letter-spacing: 0.5px; }

.cat-list a,
.cat-item .cat-row {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 20px; font-size: 13px; color: #555;
  text-decoration: none; transition: all 0.15s;
  cursor: pointer; user-select: none; position: relative;
}
.cat-list a:hover,
.cat-item .cat-row:hover { background: #f0f7ff; color: #409eff; }
.cat-list a.active { background: #e6f1fc; color: #409eff; font-weight: 600; border-left: 3px solid #409eff; padding-left: 17px; }
.cat-item.active > .cat-row { color: #409eff; font-weight: 600; }

.cat-icon { font-size: 14px; width: 22px; text-align: center; flex-shrink: 0; }
.arrow { margin-left: auto; font-size: 10px; color: #bbb; transition: transform 0.2s; }
.cat-item:hover .arrow { transform: translateX(2px); color: #409eff; }

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

  background: #fff;
  border: 5px solid #ffffff;
  border-radius: 0 12px 12px 0;
  box-shadow: 8px 0 28px rgba(0,0,0,0.12);
  padding: 20px 24px;

  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 6px 16px;

  z-index: 9999;

  /* 隐藏 → 显示过渡（只用 visibility+opacity，不用 pointer-events） */
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease, visibility 0.2s ease;
}

/* 鼠标在分类项上，或在面板内，都保持显示 */
.cat-item:hover .cat-sub-panel,
.cat-sub-panel:hover {
  opacity: 1;
  visibility: visible;
}

.cat-sub-panel a {
  display: inline-block;
  padding: 7px 18px;
  font-size: 13px;
  color: #444;
  text-decoration: none;
  border-radius: 20px;
  transition: all 0.15s;
  white-space: nowrap;
  background: transparent;
}
.cat-sub-panel a:hover {
  background: #eaf3ff;
  color: #409eff;
}
.cat-sub-panel a.active {
  background: #409eff;
  color: #fff;
  font-weight: 500;
}
/* ---- 中栏：推荐轮播 ---- */
.hero-center { flex: 1; min-width: 0; }
.recommend-banner {
  position: relative; width: 100%; height: 100%; border-radius: 12px;
  overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  transition: background-color 0.6s ease;
}
.recommend-placeholder {
  height: 100%; border-radius: 12px; background: linear-gradient(135deg,#f0f4f8,#e8ecf1);
  display: flex; align-items: center; justify-content: center;
  color: #aaa; font-size: 15px; box-shadow: inset 0 2px 8px rgba(0,0,0,0.04);
}
.banner-inner {
  display: flex; align-items: center; width: 100%; height: 100%;
  text-decoration: none; color: inherit; position: relative;
}
.banner-bg { position: absolute; inset: 0; z-index: 0; background: var(--bg-color, #e2e8f0); }
.banner-bg::after {
  content:''; position:absolute; inset:0;
  background: linear-gradient(
      105deg, var(--bg-color,#e2e8f0) 0%,
      color-mix(in srgb,var(--bg-color,#e2e8f0) 70%,white) 45%,
      color-mix(in srgb,var(--bg-color,#e2e8f0) 35%,white) 65%, transparent 100%
  );
}
.banner-bg img {
  width: 38%; height: 72%; object-fit: cover; border-radius: 16px;
  position: absolute; right: 32px; top: 50%; transform: translateY(-50%);
  box-shadow: 0 6px 24px rgba(0,0,0,0.14);
}
.banner-content { position:relative; z-index:1; padding-left:44px; display:flex; flex-direction:column; gap:10px; }
.banner-cat { font-size:13px; color:#5a7d87; margin:0; letter-spacing:1px; opacity:.85; }
.banner-content h3 { font-size:26px; color:#2a3f49; margin:0; max-width:360px; line-height:1.4; font-weight:700; }
.banner-hint { font-size:14px; color:#5a7d87; margin:0; opacity:.8; letter-spacing:.5px; }
.banner-dots {
  position:absolute; bottom:16px; left:50%; transform:translateX(-50%);
  display:flex; gap:9px; z-index:2;
}
.banner-dots span {
  width:9px; height:9px; border-radius:50%;
  background:rgba(80,80,80,.2); cursor:pointer; transition:all .3s;
}
.banner-dots span.active { background:rgba(42,63,73,.7); transform:scale(1.35); box-shadow:0 0 8px rgba(42,63,73,.12); }

/* ---- 右栏：用户面板 ---- */
.hero-right { width: 250px; flex-shrink: 0; }
.user-panel {
  background: #fff; border-radius: 12px; height: 90%;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); padding: 18px 16px;
  display:flex; flex-direction:column; gap:4px; overflow-y:auto;
}
.user-greeting { text-align:center; padding-top:10px; }
.greet-text { font-size:15px; color:#555; margin:0 0 14px; }
.auth-btns { display:flex; gap:10px; justify-content:center; }
.btn-login {
  padding:7px 22px; background:#409eff; color:#fff; border-radius:20px;
  text-decoration:none; font-size:13px; font-weight:500; transition:all .2s;
}
.btn-login:hover { background:#337ecc; }
.btn-reg {
  padding:7px 22px; background:#fff; color:#409eff; border:1px solid #409eff; border-radius:20px;
  text-decoration:none; font-size:13px; font-weight:500; transition:all .2s;
}
.btn-reg:hover { background:#f0f7ff; }

.user-avatar-row { display:flex; align-items:center; gap:12px; }
.avatar-circle {
  width:42px; height:42px; border-radius:50%; background:linear-gradient(135deg,#409eff,#67c23a);
  color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:bold; flex-shrink:0;
}
.user-info-text b { display:block; font-size:15px; color:#333; }
.user-info-text small { color:#999; font-size:11px; }

.panel-divider { height:1px; background:#f0f0f0; margin:10px 0; }

.addr-card { padding: 8px 4px; }
.addr-label { font-size:12px; color:#999; margin:0 0 6px; }
.addr-name { font-size:13px; color:#333; margin:0 0 4px; font-weight:600; }
.addr-detail { font-size:12px; color:#888; margin:0; line-height:1.45; word-break:break-all; }
.addr-empty { text-align:center; padding:12px 0; }
.addr-empty p { font-size:13px; color:#aaa; margin:0 0 8px; }
.link-add { font-size:13px; color:#409eff; text-decoration:none; font-weight:500; }

.quick-links { list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:2px; }
.quick-links li a {
  display:block; padding:7px 8px; font-size:13px; color:#555; text-decoration:none;
  border-radius:6px; transition:all .15s;
}
.quick-links li a:hover { background:#f5f7fa; color:#409eff; }

/* ---- 商品网格 ---- */
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.empty-state { text-align:center; padding:60px 0; color:#999; }
.empty-state p { font-size:16px; margin-bottom:12px; }
.empty-state a { color:#409eff; text-decoration:none; }
.pagination { display:flex; justify-content:center; margin-top:30px; }
</style>
