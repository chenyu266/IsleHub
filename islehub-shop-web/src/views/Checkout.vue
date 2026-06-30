<template>
  <div class="checkout-page">
    <h2>确认订单</h2>
    <div class="section">
      <div class="section-title">收货地址</div>
      <PageSkeleton v-if="loading" variant="checkout-addresses" />
      <div v-else-if="addresses.length === 0">请先 <router-link to="/address">添加收货地址</router-link></div>
      <div class="address-list" v-else>
        <div class="address-card" v-for="addr in addresses" :key="addr.id"
             :class="{ selected: selectedAddressId === addr.id, disabled: submitting }"
             @click="selectAddress(addr.id)">
          <div><b>{{ addr.receiverName }}</b> {{ addr.receiverPhone }}</div>
          <div class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</div>
        </div>
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
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { addToCart, getCart, removeFromCart } from '../api/cart'
import { getAddresses } from '../api/address'
import { checkout } from '../api/order'
import PageSkeleton from '../components/PageSkeleton.vue'
import { clearCheckoutSelectedSkuIds, readCheckoutSelectedSkuIds } from '../utils/checkoutSelection'

const router = useRouter()
const allCartItems = ref([])
const cartItems = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const remark = ref('')
const submitting = ref(false)
const loading = ref(true)

onMounted(async () => {
  try {
    await loadSelectedCartItems()
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

async function loadSelectedCartItems() {
  try {
    const selectedSkuIds = readCheckoutSelectedSkuIds().map(String)
    if (selectedSkuIds.length === 0) {
      ElMessage.warning('请先在购物车选择要结算的商品')
      router.replace('/cart')
      return
    }

    const selectedSkuSet = new Set(selectedSkuIds)
    allCartItems.value = ((await getCart()).data || []).map(item => ({ ...item, imageFailed: false }))
    cartItems.value = allCartItems.value.filter(item => selectedSkuSet.has(String(item.skuId)))

    if (cartItems.value.length === 0) {
      ElMessage.warning('选中的商品已不在购物车中')
      clearCheckoutSelectedSkuIds()
      router.replace('/cart')
    }
  } catch {
    ElMessage.error('加载购物车失败')
  }
}

async function restoreCartItems(items) {
  for (const item of items) {
    await addToCart({ skuId: item.skuId, quantity: item.quantity })
  }
}

async function submitOrder() {
  if (!selectedAddressId.value) { ElMessage.warning('请选择收货地址'); return }
  if (cartItems.value.length === 0) { ElMessage.warning('请选择要结算的商品'); return }
  if (submitting.value) return

  submitting.value = true
  const selectedSkuSet = new Set(cartItems.value.map(item => String(item.skuId)))
  const unselectedItems = allCartItems.value.filter(item => !selectedSkuSet.has(String(item.skuId)))
  const removedUnselectedItems = []

  try {
    for (const item of unselectedItems) {
      await removeFromCart(item.skuId)
      removedUnselectedItems.push(item)
    }

    const res = await checkout({ addressId: selectedAddressId.value, remark: remark.value })
    if (removedUnselectedItems.length > 0) {
      try {
        await restoreCartItems(removedUnselectedItems)
      } catch {
        ElMessage.warning('订单已提交，但未选商品恢复失败，请重新加入购物车')
      }
    }

    clearCheckoutSelectedSkuIds()
    const warnings = res.data?.warnings
    if (warnings && warnings.length > 0) {
      warnings.forEach(w => ElMessage.warning(w))
    }
    ElMessage.success('下单成功')
    router.push('/orders')
  } catch {
    if (removedUnselectedItems.length > 0) {
      try {
        await restoreCartItems(removedUnselectedItems)
      } catch {
        ElMessage.warning('未选商品恢复失败，请刷新购物车确认')
      }
    }
    ElMessage.error('下单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-page { background: #fff; padding: 30px; border-radius: 8px; }
.checkout-page h2 { margin-bottom: 20px; }
.section { margin-bottom: 24px; }
.section-title { font-size: 14px; color: #666; margin-bottom: 10px; font-weight: bold; }
.address-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.address-card { border: 2px solid #e4e7ed; border-radius: 8px; padding: 12px; cursor: pointer; }
.address-card.selected { border-color: #409eff; background: #ecf5ff; }
.address-card.disabled { cursor: not-allowed; opacity: .7; }
.addr-detail { color: #999; font-size: 13px; margin-top: 4px; }
.order-item { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 8px 0; border-bottom: 1px solid #eee; }
.order-item-main { display: flex; align-items: center; gap: 10px; min-width: 0; }
.order-item-img { width: 50px; height: 50px; object-fit: cover; border-radius: 4px; }
.order-item-placeholder { width: 50px; height: 50px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; background: #f5f7fa; border-radius: 4px; color: #bbb; font-size: 12px; }
.checkout-footer { display: flex; justify-content: flex-end; align-items: center; gap: 20px; padding-top: 16px; border-top: 2px solid #eee; }
.checkout-footer b { font-size: 22px; color: #e4393c; }
.btn-submit { padding: 12px 32px; font-size: 16px; background: #e4393c; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-submit:disabled { background: #c0c4cc; cursor: not-allowed; }
</style>
