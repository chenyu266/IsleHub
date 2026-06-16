<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    <div class="status-tabs">
      <span :class="{ active: !currentStatus }" @click="currentStatus = null">全部</span>
      <span :class="{ active: currentStatus === 'paid' }" @click="currentStatus = 'paid'">已支付</span>
      <span :class="{ active: currentStatus === 'shipped' }" @click="currentStatus = 'shipped'">已发货</span>
      <span :class="{ active: currentStatus === 'completed' }" @click="currentStatus = 'completed'">已完成</span>
    </div>
    <div v-if="orders.length === 0" class="empty">暂无订单</div>
    <div class="order-item" v-for="order in orders" :key="order.id" @click="$router.push(`/order/${order.id}`)">
      <div class="order-header">
        <span>订单号: {{ order.orderNo }}</span>
        <span class="order-status">{{ statusText(order.status) }}</span>
      </div>
      <div class="order-body">
        <span>¥{{ order.totalAmount }}</span>
        <span class="order-time">{{ order.createdAt }}</span>
      </div>
      <div class="order-actions" v-if="order.status === 'shipped'" @click.stop>
        <button class="btn-confirm" @click="doConfirm(order.id)">确认收货</button>
      </div>
    </div>
    <div class="pagination" v-if="total > pageSize">
      <el-pagination background layout="prev, pager, next"
        :total="total" :page-size="pageSize" v-model:current-page="pageNum"
        @current-change="fetchOrders" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageOrders, confirmOrder } from '../api/order'

const orders = ref([])
const currentStatus = ref(null)
const pageNum = ref(1)
const total = ref(0)
const pageSize = 20

onMounted(fetchOrders)
watch(currentStatus, () => { pageNum.value = 1; fetchOrders() })

async function fetchOrders() {
  try {
    const params = { page: pageNum.value, pageSize }
    if (currentStatus.value) params.status = currentStatus.value
    const res = await pageOrders(params)
    orders.value = res.data.records
    total.value = res.data.total
  } catch {}
}

function statusText(status) {
  return { paid: '已支付', shipped: '已发货', completed: '已完成', cancelled: '已取消' }[status] || status
}

async function doConfirm(id) {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '确认收货', { type: 'success' })
    await confirmOrder(id)
    ElMessage.success('已确认收货')
    fetchOrders()
  } catch {}
}
</script>

<style scoped>
.orders-page { background: #fff; padding: 30px; border-radius: 8px; }
.orders-page h2 { margin-bottom: 20px; }
.status-tabs { display: flex; gap: 20px; margin-bottom: 20px; }
.status-tabs span { cursor: pointer; color: #666; }
.status-tabs span.active { color: #409eff; font-weight: bold; }
.order-item { border: 1px solid #eee; border-radius: 8px; padding: 16px; margin-bottom: 12px; cursor: pointer; }
.order-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.order-status { color: #409eff; }
.order-body { display: flex; justify-content: space-between; font-size: 18px; color: #e4393c; font-weight: bold; }
.order-time { font-size: 12px; color: #999; font-weight: normal; }
.order-actions { display: flex; justify-content: flex-end; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.btn-confirm { padding: 6px 16px; font-size: 13px; background: #67c23a; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-confirm:hover { background: #5daf34; }
.empty { text-align: center; color: #999; padding: 60px 0; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>
