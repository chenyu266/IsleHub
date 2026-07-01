<template>
  <div class="checkout-page">
    <h2>确认订单</h2>
    <div class="section">
      <div class="section-title">收货地址</div>
      <PageSkeleton v-if="loading" variant="checkout-addresses" />
      <div v-else-if="addresses.length === 0">请先 <router-link to="/address">添加收货地址</router-link></div>
      <div class="address-list" v-else>
        <button
          v-for="addr in addresses"
          :key="addr.id"
          type="button"
          class="address-card"
          :class="{ selected: selectedAddressId === addr.id }"
          :disabled="submitting"
          @click="selectAddress(addr.id)"
        >
          <div><b>{{ addr.receiverName }}</b> {{ addr.receiverPhone }}</div>
          <div class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</div>
        </button>
      </div>
    </div>
    <div class="section">
      <div class="section-title">商品明细</div>
      <PageSkeleton v-if="loading" variant="checkout-items" :rows="2" />
      <template v-else>
        <div class="order-item" v-for="item in cartItems" :key="item.skuId">
          <div class="order-item-main">
            <img
              v-if="item.productImage && !item.imageFailed"
              :src="item.productImage"
              class="order-item-img"
              :alt="item.productName || '商品图片'"
              loading="lazy"
              @error="item.imageFailed = true"
            />
            <span v-else class="order-item-placeholder">暂无图片</span>
            <span>{{ item.productName || '商品' }} / {{ item.skuSpec }} ×{{ item.quantity }}</span>
          </div>
          <span>¥{{ (item.price * item.quantity).toFixed(2) }}</span>
        </div>
      </template>
    </div>
    <div class="section" v-if="!loading">
      <div class="section-title">备注</div>
      <el-input v-model="remark" :disabled="submitting" placeholder="选填，如有特殊要求请备注" />
    </div>
    <div class="checkout-footer" v-if="!loading">
      <span>应付: <b>¥{{ total.toFixed(2) }}</b></span>
      <button class="btn-submit" :disabled="submitting || cartItems.length === 0" @click="submitOrder">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCart } from '../api/cart'
import { getAddresses } from '../api/address'
import { checkout } from '../api/order'
import PageSkeleton from '../components/PageSkeleton.vue'

const router = useRouter()
const cartItems = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const remark = ref('')
const submitting = ref(false)
const loading = ref(true)

onMounted(async () => {
  try {
    await loadCartItems()
    try { addresses.value = (await getAddresses()).data || [] } catch { ElMessage.error('加载地址失败') }
    if (addresses.value.length > 0) {
      const def = addresses.value.find(a => a.isDefault === 1)
      selectedAddressId.value = def ? def.id : addresses.value[0].id
    }
  } finally {
    loading.value = false
  }
})

const total = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

function selectAddress(id) {
  if (submitting.value) return
  selectedAddressId.value = id
}

async function loadCartItems() {
  try {
    cartItems.value = ((await getCart()).data || []).map(item => ({ ...item, imageFailed: false }))

    if (cartItems.value.length === 0) {
      ElMessage.warning('购物车暂无可结算商品')
      router.replace('/cart')
    }
  } catch {
    ElMessage.error('加载购物车失败')
  }
}

async function submitOrder() {
  if (!selectedAddressId.value) { ElMessage.warning('请选择收货地址'); return }
  if (cartItems.value.length === 0) { ElMessage.warning('请选择要结算的商品'); return }
  if (submitting.value) return

  submitting.value = true

  try {
    const res = await checkout({ addressId: selectedAddressId.value, remark: remark.value })
    const warnings = res.data?.warnings
    if (warnings && warnings.length > 0) {
      warnings.forEach(w => ElMessage.warning(w))
    }
    ElMessage.success('下单成功')
    router.push('/orders')
  } catch {
    ElMessage.error('下单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-page {
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  padding: 30px;
  box-shadow: var(--shop-shadow-sm);
}

.checkout-page h2 {
  margin: 0 0 22px;
  color: var(--shop-text);
  font-size: 24px;
}

.section {
  margin-bottom: 26px;
}

.section-title {
  margin-bottom: 12px;
  color: var(--shop-text-muted);
  font-size: 14px;
  font-weight: 800;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.address-card {
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius-sm);
  padding: 14px;
  cursor: pointer;
  background: var(--shop-surface);
  color: inherit;
  font: inherit;
  text-align: left;
  transition: border-color var(--shop-transition), background var(--shop-transition), box-shadow var(--shop-transition);
}

.address-card:hover:not(:disabled) {
  border-color: rgba(37, 99, 235, .35);
  box-shadow: 0 6px 18px rgba(31, 41, 55, .06);
}

.address-card.selected {
  border-color: var(--shop-primary);
  background: var(--shop-primary-soft);
  box-shadow: 0 0 0 1px rgba(37,99,235,.12);
}

.address-card:disabled {
  cursor: not-allowed;
  opacity: .7;
}

.addr-detail {
  color: var(--shop-text-muted);
  font-size: 13px;
  margin-top: 5px;
  line-height: 1.5;
}

.order-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 10px 0;
  border-bottom: 1px solid var(--shop-border);
}

.order-item-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: var(--shop-text);
}

.order-item-img,
.order-item-placeholder {
  width: 54px;
  height: 54px;
  flex-shrink: 0;
  border-radius: var(--shop-radius-sm);
}

.order-item-img {
  padding: 5px;
  object-fit: contain;
  background: linear-gradient(135deg, #f8fafc, #eef2f6);
  border: 1px solid var(--shop-border);
}

.order-item-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--shop-surface-muted);
  border: 1px dashed var(--shop-border-strong);
  color: var(--shop-text-subtle);
  font-size: 12px;
}

.order-item > span:last-child {
  color: var(--shop-price);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.checkout-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--shop-border);
}

.checkout-footer b {
  color: var(--shop-price);
  font-size: 24px;
  font-variant-numeric: tabular-nums;
}

.btn-submit {
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

.btn-submit:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-submit:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
  opacity: .72;
}
</style>
