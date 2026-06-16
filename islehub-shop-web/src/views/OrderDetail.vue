<template>
  <div class="order-detail" v-if="order">
    <h2>订单详情</h2>
    <div class="info-row"><span>订单号</span><span>{{ order.orderNo }}</span></div>
    <div class="info-row"><span>状态</span><span class="status">{{ order.status }}</span></div>
    <div class="info-row"><span>下单时间</span><span>{{ order.createdAt }}</span></div>
    <div class="info-row"><span>收货时间</span><span>{{ order.shipping?.deliveredAt || '未收货' }}</span></div>
    <div class="section">
      <div class="section-title">收货信息</div>
      <div>{{ order.receiverName }} | {{ order.receiverPhone }}</div>
      <div>{{ order.receiverAddress }}</div>
    </div>
    <div class="section">
      <div class="section-title">商品明细</div>
      <div class="item-row" v-for="item in order.items" :key="item.id">
        <span>{{ item.productName }} / {{ item.skuSpec }} ×{{ item.quantity }}</span>
        <span>¥{{ (item.price * item.quantity).toFixed(2) }}</span>
      </div>
      <div class="total-row">合计: <b>¥{{ order.totalAmount }}</b></div>
    </div>
    <div class="section" v-if="order.shipping">
      <div class="section-title">物流信息</div>
      <div>{{ order.shipping.carrier }} — {{ order.shipping.trackingNo }}</div>
      <div v-if="order.shipping.shippedAt">发货时间: {{ order.shipping.shippedAt }}</div>
      <div v-if="order.shipping.deliveredAt">签收时间: {{ order.shipping.deliveredAt }}</div>
    </div>
    <div class="actions">
      <el-button v-if="order.status === 'paid'" type="danger" @click="handleCancel">取消订单</el-button>
      <el-button v-if="order.status === 'shipped'" type="primary" @click="handleConfirm">确认收货</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, cancelOrder, confirmOrder } from '../api/order'

const route = useRoute()
const router = useRouter()
const order = ref(null)

onMounted(async () => {
  const res = await getOrder(route.params.id)
  order.value = res.data
})

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定取消订单？', '确认')
    await cancelOrder(order.value.id)
    ElMessage.success('已取消')
    order.value.status = 'cancelled'
  } catch {}
}

async function handleConfirm() {
  try {
    await confirmOrder(order.value.id)
    ElMessage.success('已确认收货')
    order.value = (await getOrder(order.value.id)).data
  } catch {}
}
</script>

<style scoped>
.order-detail { background: #fff; padding: 30px; border-radius: 8px; }
.order-detail h2 { margin-bottom: 20px; }
.info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
.status { color: #409eff; font-weight: bold; }
.section { margin-top: 20px; }
.section-title { font-weight: bold; color: #666; margin-bottom: 8px; }
.item-row { display: flex; justify-content: space-between; padding: 6px 0; }
.total-row { text-align: right; margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee; }
.total-row b { font-size: 20px; color: #e4393c; }
.actions { margin-top: 30px; display: flex; gap: 12px; }
</style>
