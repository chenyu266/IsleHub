<template>
  <div class="product-detail" v-if="product">
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
        <SkuSelector :skus="product.skus" @select="onSkuSelect" />
      </div>
      <div class="section">
        <div class="section-title">数量</div>
        <div class="quantity-ctrl">
          <button @click="quantity = Math.max(1, quantity - 1)">-</button>
          <span>{{ quantity }}</span>
          <button @click="quantity++">+</button>
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProduct } from '../api/product'
import { addToCart as addToCartApi } from '../api/cart'
import SkuSelector from '../components/SkuSelector.vue'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const selectedSku = ref(null)
const quantity = ref(1)

onMounted(async () => {
  try { product.value = (await getProduct(route.params.id)).data } catch { ElMessage.error('加载商品失败') }
})

function onSkuSelect(sku) { selectedSku.value = sku }

async function doAddToCart() {
  if (!selectedSku.value) { ElMessage.warning('请选择规格'); return }
  try {
    await addToCartApi({ skuId: selectedSku.value.id, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch { ElMessage.error('加入购物车失败') }
}

async function addToCart() { await doAddToCart() }

async function buyNow() {
  await doAddToCart()
  router.push('/cart')
}
</script>

<style scoped>
.product-detail { display: flex; gap: 40px; background: #fff; padding: 30px; border-radius: 8px; }
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
.quantity-ctrl button { width: 32px; height: 32px; border: 1px solid #dcdfe6; background: #fff; cursor: pointer; font-size: 16px; border-radius: 4px; }
.quantity-ctrl span { font-size: 16px; min-width: 30px; text-align: center; }
.stock-tip { font-size: 12px; color: #999; }
.actions { display: flex; gap: 16px; margin-top: 30px; }
.btn-cart { padding: 12px 30px; font-size: 16px; background: #409eff; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-buy { padding: 12px 30px; font-size: 16px; background: #e4393c; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.desc { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; }
.desc p { color: #666; line-height: 1.8; }
</style>
