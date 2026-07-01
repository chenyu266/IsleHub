<template>
  <div class="cart-page">
    <h2>我的购物车</h2>
    <PageSkeleton v-if="loading" variant="cart-list" :rows="3" class="inline-skeleton" />
    <div v-else-if="items.length === 0" class="empty">购物车空空如也，<router-link to="/">去逛逛</router-link></div>
    <template v-else>
      <div class="cart-list">
        <div class="cart-item" v-for="item in items" :key="item.skuId" :class="{ pending: item.updating || item.removing }">
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
        <span>共 {{ items.length }} 件商品，合计: <b>¥{{ total.toFixed(2) }}</b></span>
        <button class="btn-checkout" :disabled="items.length === 0" @click="goCheckout">去结算</button>
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

const router = useRouter()
const items = ref([])
const loading = ref(true)

onMounted(fetchCart)
async function fetchCart() {
  loading.value = true
  try { items.value = ((await getCart()).data || []).map(i => ({ ...i, updating: false, removing: false, imageFailed: false })) } catch { ElMessage.error('加载购物车失败') }
  finally { loading.value = false }
}

const total = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))

function goCheckout() {
  if (items.value.length === 0) {
    ElMessage.warning('购物车暂无可结算商品')
    return
  }
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
.cart-page {
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  padding: 30px;
  box-shadow: var(--shop-shadow-sm);
}

.cart-page h2 {
  margin: 0 0 22px;
  color: var(--shop-text);
  font-size: 24px;
}

.inline-skeleton { padding: 0; }

.empty {
  text-align: center;
  color: var(--shop-text-muted);
  padding: 64px 0;
  border: 1px dashed var(--shop-border-strong);
  border-radius: var(--shop-radius);
  background: var(--shop-surface-muted);
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--shop-border);
  transition: opacity var(--shop-transition), background var(--shop-transition);
}

.cart-item:hover {
  background: linear-gradient(90deg, rgba(37,99,235,.035), transparent);
}

.cart-item.pending { opacity: .72; }

.item-image {
  width: 84px;
  height: 84px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  background: linear-gradient(135deg, #f8fafc, #eef2f6);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius-sm);
  flex-shrink: 0;
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.no-image {
  width: 100%;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--shop-border-strong);
  border-radius: 4px;
  color: var(--shop-text-subtle);
  font-size: 12px;
  background: rgba(255,255,255,.58);
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  color: var(--shop-text);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
}

.item-spec {
  margin-top: 4px;
  color: var(--shop-text-subtle);
  font-size: 12px;
}

.item-price,
.item-subtotal {
  width: 100px;
  color: var(--shop-price);
  font-weight: 800;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-quantity span {
  min-width: 24px;
  text-align: center;
  color: var(--shop-text);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.item-quantity button {
  width: 30px;
  height: 30px;
  border: 1px solid var(--shop-border);
  background: var(--shop-surface);
  color: var(--shop-text-muted);
  cursor: pointer;
  border-radius: var(--shop-radius-sm);
  transition: border-color var(--shop-transition), color var(--shop-transition), background var(--shop-transition);
}

.item-quantity button:hover:not(:disabled) {
  border-color: var(--shop-primary);
  color: var(--shop-primary);
  background: var(--shop-primary-soft);
}

.item-quantity button:disabled {
  color: #cbd5e1;
  background: var(--shop-surface-muted);
  cursor: not-allowed;
}

.item-remove {
  color: var(--shop-text-subtle);
  background: none;
  border: none;
  cursor: pointer;
  transition: color var(--shop-transition);
}

.item-remove:hover:not(:disabled) {
  color: var(--shop-danger);
}

.item-remove:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.cart-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--shop-border);
}

.cart-footer b {
  color: var(--shop-price);
  font-size: 24px;
  font-variant-numeric: tabular-nums;
}

.btn-checkout {
  height: 44px;
  padding: 0 34px;
  border: none;
  border-radius: var(--shop-radius-sm);
  background: var(--shop-price);
  color: #fff;
  cursor: pointer;
  font-size: 16px;
  font-weight: 700;
  transition: background var(--shop-transition), opacity var(--shop-transition);
}

.btn-checkout:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-checkout:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
  opacity: .72;
}
</style>
