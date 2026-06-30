<template>
  <div class="order-detail" v-if="loading">
    <h2>订单详情</h2>
    <PageSkeleton variant="order-detail" />
  </div>
  <div class="order-detail" v-else-if="order">
    <h2>订单详情</h2>
    <div class="info-row"><span>订单号</span><span>{{ order.orderNo }}</span></div>
    <div class="info-row"><span>状态</span><span class="status">{{ statusText(order.status) }}</span></div>
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
    <div class="actions" v-if="order.status === 'paid'">
      <el-button type="danger" :loading="cancelling" @click="handleCancel">取消订单</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { getOrder, cancelOrder } from '../api/order'
import PageSkeleton from '../components/PageSkeleton.vue'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const cancelling = ref(false)

onMounted(async () => {
  try {
    const res = await getOrder(route.params.id)
    order.value = res.data
  } catch {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
})

function statusText(status) {
  return { paid: '已支付', shipped: '已发货', completed: '已完成', cancelled: '已取消' }[status] || status
}

async function handleCancel() {
  if (cancelling.value) return
  try {
    await ElMessageBox.confirm('确定取消订单？', '确认')
  } catch {
    return // 用户取消
  }
  cancelling.value = true
  try {
    await cancelOrder(order.value.id)
    ElMessage.success('已取消')
    order.value.status = 'cancelled'
  } catch { ElMessage.error('取消失败') }
  finally { cancelling.value = false }
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
