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
import { ElMessage, ElMessageBox } from 'element-plus'
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
.order-detail {
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  padding: 30px;
  box-shadow: var(--shop-shadow-sm);
}

.order-detail h2 {
  margin: 0 0 22px;
  color: var(--shop-text);
  font-size: 24px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 10px 0;
  border-bottom: 1px solid var(--shop-border);
  color: var(--shop-text);
}

.info-row > span:first-child {
  color: var(--shop-text-muted);
  font-weight: 700;
}

.status {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--shop-primary-soft);
  color: var(--shop-primary);
  font-weight: 800;
}

.section {
  margin-top: 22px;
  padding-top: 4px;
  color: var(--shop-text-muted);
  line-height: 1.7;
}

.section-title {
  margin-bottom: 10px;
  color: var(--shop-text);
  font-weight: 800;
}

.item-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 8px 0;
  border-bottom: 1px solid var(--shop-border);
}

.item-row span:last-child {
  color: var(--shop-price);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.total-row {
  margin-top: 10px;
  padding-top: 10px;
  text-align: right;
}

.total-row b {
  color: var(--shop-price);
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.actions {
  margin-top: 30px;
  display: flex;
  gap: 12px;
}
</style>
