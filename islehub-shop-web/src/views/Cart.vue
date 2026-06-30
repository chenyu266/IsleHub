<template>
  <div class="cart-page">
    <h2>我的购物车</h2>
    <PageSkeleton v-if="loading" variant="cart-list" :rows="3" class="inline-skeleton" />
    <div v-else-if="items.length === 0" class="empty">购物车空空如也，<router-link to="/">去逛逛</router-link></div>
    <template v-else>
      <div class="cart-list">
        <div class="cart-item" v-for="item in items" :key="item.skuId" :class="{ pending: item.updating || item.removing }">
          <el-checkbox v-model="item.checked" :disabled="item.updating || item.removing" />
          <div class="item-image">
            <img
              v-if="item.productImage && !item.imageFailed"
              :src="item.productImage"
              :alt="item.productName || '商品图片'"
              loading="lazy"
              @error="item.imageFailed = true"
            />
            <span v-else class="no-image">暂无图片</span>
          </div>
          <div class="item-info">
            <div class="item-name">{{ item.productName || '商品' }}</div>
            <div class="item-spec">{{ item.skuSpec }}</div>
          </div>
          <div class="item-price">¥{{ item.price }}</div>
          <div class="item-quantity">
            <button :disabled="item.updating || item.removing || item.quantity <= 1" @click="changeQty(item, -1)">-</button>
            <span>{{ item.quantity }}</span>
            <button :disabled="item.updating || item.removing" @click="changeQty(item, 1)">+</button>
          </div>
          <div class="item-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
          <button class="item-remove" :disabled="item.updating || item.removing" @click="removeItem(item)">
            {{ item.removing ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
      <div class="cart-footer">
        <span>合计: <b>¥{{ total.toFixed(2) }}</b></span>
        <button class="btn-checkout" :disabled="selectedItems.length === 0" @click="goCheckout">去结算</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { getCart, updateQuantity, removeFromCart } from '../api/cart'
import PageSkeleton from '../components/PageSkeleton.vue'
import { saveCheckoutSelectedSkuIds } from '../utils/checkoutSelection'

const router = useRouter()
const items = ref([])
const loading = ref(true)

onMounted(fetchCart)
async function fetchCart() {
  loading.value = true
  try { items.value = ((await getCart()).data || []).map(i => ({ ...i, checked: true, updating: false, removing: false, imageFailed: false })) } catch { ElMessage.error('加载购物车失败') }
  finally { loading.value = false }
}

const selectedItems = computed(() => items.value.filter(i => i.checked))
const total = computed(() => selectedItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

function goCheckout() {
  const selectedSkuIds = selectedItems.value.map(i => i.skuId)
  if (selectedSkuIds.length === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }
  saveCheckoutSelectedSkuIds(selectedSkuIds)
  router.push('/checkout')
}

async function changeQty(item, delta) {
  if (item.updating || item.removing) return
  const newQty = item.quantity + delta
  if (newQty < 1) return
  const oldQty = item.quantity
  item.updating = true
  item.quantity = newQty
  try {
    await updateQuantity(item.skuId, newQty)
  } catch {
    item.quantity = oldQty
    ElMessage.error('更新数量失败')
  } finally {
    item.updating = false
  }
}

async function removeItem(item) {
  if (item.updating || item.removing) return
  try {
    await ElMessageBox.confirm(`确定删除「${item.productName || '该商品'}」吗？`, '删除商品', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  item.removing = true
  try {
    await removeFromCart(item.skuId)
    items.value = items.value.filter(i => i.skuId !== item.skuId)
  } catch {
    item.removing = false
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.cart-page { background: #fff; padding: 30px; border-radius: 8px; }
.cart-page h2 { margin-bottom: 20px; }
.inline-skeleton { padding: 0; }
.empty { text-align: center; color: #999; padding: 60px 0; }
.cart-item { display: flex; align-items: center; gap: 16px; padding: 16px 0; border-bottom: 1px solid #eee; }
.cart-item.pending { opacity: .72; }
.item-image { width: 80px; height: 80px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 4px; flex-shrink: 0; overflow: hidden; }
.item-image img { width: 100%; height: 100%; object-fit: cover; }
.no-image { color: #ccc; font-size: 12px; }
.item-info { flex: 1; }
.item-name { font-size: 14px; color: #333; }
.item-spec { font-size: 12px; color: #999; margin-top: 4px; }
.item-price { color: #e4393c; font-weight: bold; width: 100px; text-align: center; }
.item-quantity { display: flex; align-items: center; gap: 8px; }
.item-quantity button { width: 28px; height: 28px; border: 1px solid #dcdfe6; background: #fff; cursor: pointer; border-radius: 4px; }
.item-quantity button:disabled { color: #c0c4cc; background: #f5f7fa; cursor: not-allowed; }
.item-subtotal { color: #e4393c; font-weight: bold; width: 100px; text-align: center; }
.item-remove { color: #999; background: none; border: none; cursor: pointer; }
.item-remove:disabled { color: #c0c4cc; cursor: not-allowed; }
.cart-footer { display: flex; justify-content: flex-end; align-items: center; gap: 20px; margin-top: 20px; padding-top: 16px; border-top: 2px solid #eee; }
.cart-footer b { font-size: 22px; color: #e4393c; }
.btn-checkout { padding: 12px 32px; font-size: 16px; background: #e4393c; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-checkout:disabled { background: #c0c4cc; cursor: not-allowed; }
</style>
