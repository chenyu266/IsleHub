<template>
  <PageSkeleton v-if="loading" variant="product-detail" />
  <div class="product-detail" v-else-if="product">
    <div class="detail-image">
      <img
        v-if="product.mainImage && !imageFailed"
        :src="product.mainImage"
        :alt="product.name || '商品图片'"
        loading="lazy"
        @error="imageFailed = true"
      />
      <span v-else class="no-image">暂无图片</span>
    </div>
    <div class="detail-info">
      <h2>{{ product.name }}</h2>
      <div class="price">
        <template v-if="selectedSku">¥{{ (selectedSku.price * quantity).toFixed(2) }}</template>
        <template v-else>--</template>
      </div>
      <div class="price-detail" v-if="selectedSku && quantity > 1">
        单价 ¥{{ selectedSku.price }} × {{ quantity }} 件
      </div>
      <div class="section" v-if="product.skus && product.skus.length > 0">
        <div class="section-title">选择规格</div>
        <SkuSelector :skus="product.skus" :selected-sku-id="selectedSku?.id" @select="onSkuSelect" />
      </div>
      <div class="section">
        <div class="section-title">数量</div>
        <div class="quantity-ctrl">
          <el-input-number
            v-model="quantity"
            :min="1"
            :precision="0"
            :step="1"
            controls-position="right"
            class="quantity-input"
            @change="handleQuantityChange"
            @blur="normalizeQuantity"
          />
          <span class="stock-tip" v-if="selectedSku">库存 {{ selectedSku.stock }} 件</span>
        </div>
      </div>
      <div class="actions">
        <button class="btn-cart" :disabled="actionPending" @click="addToCart">
          {{ addingToCart ? '加入中...' : '加入购物车' }}
        </button>
        <button class="btn-buy" :disabled="actionPending" @click="buyNow">
          {{ buyingNow ? '处理中...' : '加入并去购物车' }}
        </button>
      </div>
      <div class="desc" v-if="product.description">
        <div class="section-title">商品详情</div>
        <p>{{ product.description }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage,ElMessageBox } from 'element-plus'
import { getProduct } from '../api/product'
import { addToCart as addToCartApi } from '../api/cart'
import PageSkeleton from '../components/PageSkeleton.vue'
import SkuSelector from '../components/SkuSelector.vue'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const selectedSku = ref(null)
const quantity = ref(1)
const loading = ref(true)
const stockWarningVisible = ref(false)
const imageFailed = ref(false)
const addingToCart = ref(false)
const buyingNow = ref(false)

onMounted(async () => {
  try {
    product.value = (await getProduct(route.params.id)).data
    selectedSku.value = getLowestPriceSku(product.value?.skus)
  } catch { ElMessage.error('加载商品失败') }
  finally { loading.value = false }
})

const availableStock = computed(() => {
  const stock = Number(selectedSku.value?.stock)
  return Number.isFinite(stock) ? Math.max(0, Math.floor(stock)) : 0
})

const maxQuantity = computed(() => {
  return availableStock.value > 0 ? availableStock.value : 1
})
const actionPending = computed(() => addingToCart.value || buyingNow.value)

watch(selectedSku, () => normalizeQuantity())

function getLowestPriceSku(skus = []) {
  if (!Array.isArray(skus) || skus.length === 0) return null
  const inStockSkus = skus.filter(sku => Number(sku.stock) > 0)
  const candidates = inStockSkus.length > 0 ? inStockSkus : skus
  return [...candidates].sort((a, b) => Number(a.price) - Number(b.price))[0] || null
}

function showMaxStockWarning() {
  if (stockWarningVisible.value) return
  stockWarningVisible.value = true
  ElMessageBox.alert(`最大库存只有${availableStock.value}！`, '库存不足', {
    type: 'warning',
    confirmButtonText: '知道了'
  }).finally(() => {
    stockWarningVisible.value = false
  })
}

function normalizeQuantity(options = {}) {
  const { warnOnOverflow = false } = options
  const value = Number(quantity.value)
  if (!Number.isFinite(value) || value < 1) {
    quantity.value = 1
    return quantity.value
  }
  const normalizedValue = Math.floor(value)
  if (normalizedValue > maxQuantity.value) {
    if (warnOnOverflow) showMaxStockWarning()
    quantity.value = maxQuantity.value
    return quantity.value
  }
  quantity.value = normalizedValue
  return quantity.value
}

function handleQuantityChange() {
  normalizeQuantity({ warnOnOverflow: true })
}

function onSkuSelect(sku) {
  selectedSku.value = sku
}

function validateQuantityForSubmit() {
  const value = Number(quantity.value)
  if (!Number.isFinite(value) || value < 1) {
    quantity.value = 1
    ElMessage.warning('请输入正确的购买数量')
    return false
  }
  const normalizedValue = Math.floor(value)
  if (normalizedValue > availableStock.value) {
    showMaxStockWarning()
    quantity.value = maxQuantity.value
    return false
  }
  quantity.value = normalizedValue
  return true
}

async function doAddToCart() {
  if (!localStorage.getItem('shop-token')) {
    ElMessage.warning('请先登录后再操作')
    router.push('/login')
    return false
  }
  if (!selectedSku.value) { ElMessage.warning('请选择规格'); return false }
  if (availableStock.value <= 0) { ElMessage.warning('该规格库存不足'); return false }
  if (!validateQuantityForSubmit()) return false
  const safeQuantity = quantity.value
  try {
    await addToCartApi({ skuId: selectedSku.value.id, quantity: safeQuantity })
    ElMessage.success('已加入购物车')
    return true
  } catch {
    ElMessage.error('加入购物车失败')
    return false
  }
}

async function addToCart() {
  if (actionPending.value) return
  addingToCart.value = true
  try {
    await doAddToCart()
  } finally {
    addingToCart.value = false
  }
}

async function buyNow() {
  if (actionPending.value) return
  buyingNow.value = true
  try {
    const added = await doAddToCart()
    if (added) router.push('/cart')
  } finally {
    buyingNow.value = false
  }
}
</script>

<style scoped>
.product-detail {
  display: flex;
  gap: 40px;
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  padding: 30px;
  box-shadow: var(--shop-shadow-sm);
}

.detail-image {
  width: 420px;
  height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  background: linear-gradient(135deg, #f8fafc, #eef2f6);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  flex-shrink: 0;
}

.detail-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.no-image {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  border: 1px dashed var(--shop-border-strong);
  border-radius: var(--shop-radius-sm);
  color: var(--shop-text-subtle);
  font-size: 16px;
  background: rgba(255,255,255,.58);
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.detail-info h2 {
  margin: 0 0 14px;
  color: var(--shop-text);
  font-size: 24px;
  line-height: 1.45;
}

.price {
  margin-bottom: 4px;
  color: var(--shop-price);
  font-size: 30px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.price-detail {
  margin-bottom: 18px;
  color: var(--shop-text-subtle);
  font-size: 13px;
}

.section {
  margin-bottom: 22px;
}

.section-title {
  margin-bottom: 10px;
  color: var(--shop-text-muted);
  font-size: 14px;
  font-weight: 700;
}

.quantity-ctrl {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quantity-input {
  width: 140px;
}

.stock-tip {
  color: var(--shop-text-subtle);
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 14px;
  margin-top: 30px;
}

.btn-cart,
.btn-buy {
  min-width: 132px;
  height: 44px;
  padding: 0 28px;
  border: none;
  border-radius: var(--shop-radius-sm);
  color: #fff;
  cursor: pointer;
  font-size: 16px;
  font-weight: 700;
  transition: background var(--shop-transition), box-shadow var(--shop-transition), opacity var(--shop-transition);
}

.btn-cart {
  background: var(--shop-primary);
}

.btn-cart:hover {
  background: var(--shop-primary-hover);
}

.btn-buy {
  background: var(--shop-price);
}

.btn-buy:hover {
  background: #b91c1c;
}

.btn-cart:disabled,
.btn-buy:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
  opacity: .72;
}

.desc {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--shop-border);
}

.desc p {
  color: var(--shop-text-muted);
  line-height: 1.8;
}
</style>
