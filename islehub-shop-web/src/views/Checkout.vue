<template>
  <div class="checkout-page">
    <h2>确认订单</h2>
    <div class="section">
      <div class="section-title">收货地址</div>
      <div v-if="addresses.length === 0">请先 <router-link to="/address">添加收货地址</router-link></div>
      <div class="address-list" v-else>
        <div class="address-card" v-for="addr in addresses" :key="addr.id"
             :class="{ selected: selectedAddressId === addr.id }"
             @click="selectedAddressId = addr.id">
          <div><b>{{ addr.receiverName }}</b> {{ addr.receiverPhone }}</div>
          <div class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</div>
        </div>
      </div>
    </div>
    <div class="section">
      <div class="section-title">商品明细</div>
      <div class="order-item" v-for="item in cartItems" :key="item.skuId">
        <span>{{ item.productName || '商品' }} / {{ item.skuSpec }} ×{{ item.quantity }}</span>
        <span>¥{{ (item.price * item.quantity).toFixed(2) }}</span>
      </div>
    </div>
    <div class="section">
      <div class="section-title">备注</div>
      <el-input v-model="remark" placeholder="选填，如有特殊要求请备注" />
    </div>
    <div class="checkout-footer">
      <span>应付: <b>¥{{ total.toFixed(2) }}</b></span>
      <button class="btn-submit" @click="submitOrder">提交订单</button>
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

const router = useRouter()
const cartItems = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const remark = ref('')

onMounted(async () => {
  try { cartItems.value = (await getCart()).data || [] } catch {}
  try { addresses.value = (await getAddresses()).data || [] } catch {}
  if (addresses.value.length > 0) {
    const def = addresses.value.find(a => a.isDefault === 1)
    selectedAddressId.value = def ? def.id : addresses.value[0].id
  }
})

const total = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

async function submitOrder() {
  if (!selectedAddressId.value) { ElMessage.warning('请选择收货地址'); return }
  try {
    const res = await checkout({ addressId: selectedAddressId.value, remark: remark.value })
    const warnings = res.data?.warnings
    if (warnings && warnings.length > 0) {
      warnings.forEach(w => ElMessage.warning(w))
    }
    ElMessage.success('下单成功')
    router.push('/orders')
  } catch {}
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
.addr-detail { color: #999; font-size: 13px; margin-top: 4px; }
.order-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
.checkout-footer { display: flex; justify-content: flex-end; align-items: center; gap: 20px; padding-top: 16px; border-top: 2px solid #eee; }
.checkout-footer b { font-size: 22px; color: #e4393c; }
.btn-submit { padding: 12px 32px; font-size: 16px; background: #e4393c; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
</style>
