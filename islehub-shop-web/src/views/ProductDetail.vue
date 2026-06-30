<template>
  <PageSkeleton v-if="loading" variant="product-detail" />
  <div class="product-detail" v-else-if="product">
    <div class="detail-image">
      <img v-if="product.mainImage" :src="product.mainImage" alt="" />
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
        <button class="btn-cart" @click="addToCart">加入购物车</button>
        <button class="btn-buy" @click="buyNow">立即购买</button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
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

async function addToCart() { await doAddToCart() }

async function buyNow() {
  const added = await doAddToCart()
  if (added) router.push('/cart')
}
</script>

<style scoped>
.product-detail { display: flex; gap: 40px; background: #fff; padding: 30px; border-radius: 8px; user-select: none;}
.detail-image { width: 420px; height: 420px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 8px; }
.detail-image img { max-width: 100%; max-height: 100%; object-fit: contain; }
.no-image { color: #ccc; font-size: 16px; }
.detail-info { flex: 1; }
.detail-info h2 { font-size: 22px; margin-bottom: 12px; }
.price { font-size: 28px; color: #e4393c; font-weight: bold; margin-bottom: 4px; }
.price-detail { font-size: 13px; color: #999; margin-bottom: 16px; }
.section { margin-bottom: 20px; }
.section-title { font-size: 14px; color: #999; margin-bottom: 8px; }
.quantity-ctrl { display: flex; align-items: center; gap: 8px; }
.quantity-input { width: 140px; }
.stock-tip { font-size: 12px; color: #999; }
.actions { display: flex; gap: 16px; margin-top: 30px; }
.btn-cart { padding: 12px 30px; font-size: 16px; background: #409eff; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-buy { padding: 12px 30px; font-size: 16px; background: #e4393c; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.desc { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; }
.desc p { color: #666; line-height: 1.8; }
</style>
