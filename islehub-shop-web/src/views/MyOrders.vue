<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    <div class="status-tabs">
      <span :class="{ active: !currentStatus }" @click="currentStatus = null">全部</span>
      <span :class="{ active: currentStatus === 'paid' }" @click="currentStatus = 'paid'">已支付</span>
      <span :class="{ active: currentStatus === 'shipped' }" @click="currentStatus = 'shipped'">已发货</span>
      <span :class="{ active: currentStatus === 'completed' }" @click="currentStatus = 'completed'">已完成</span>
    </div>
    <PageSkeleton v-if="loading" variant="order-list" :rows="4" class="inline-skeleton" />
    <div v-else-if="orders.length === 0" class="empty">暂无订单</div>
    <template v-else>
      <div class="order-item" v-for="order in orders" :key="order.id" @click="$router.push(`/order/${order.id}`)">
        <div class="order-header">
          <span>订单号: {{ order.orderNo }}</span>
          <span class="order-status">{{ statusText(order.status) }}</span>
        </div>
        <div class="order-body">
          <span>¥{{ order.totalAmount }}</span>
          <span class="order-time">{{ order.createdAt }}</span>
        </div>
      </div>
    </template>
    <div class="pagination" v-if="total > pageSize">
      <el-pagination background layout="prev, pager, next"
        :total="total" :page-size="pageSize" v-model:current-page="pageNum"
        @current-change="fetchOrders" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { pageOrders } from '../api/order'
import PageSkeleton from '../components/PageSkeleton.vue'

const orders = ref([])
const currentStatus = ref(null)
const pageNum = ref(1)
const total = ref(0)
const pageSize = 20
const loading = ref(true)

onMounted(fetchOrders)
watch(currentStatus, () => { pageNum.value = 1; fetchOrders() })

async function fetchOrders() {
  loading.value = true
  try {
    const params = { page: pageNum.value, pageSize }
    if (currentStatus.value) params.status = currentStatus.value
    const res = await pageOrders(params)
    orders.value = res.data.records
    total.value = res.data.total
  } catch { ElMessage.error('加载订单失败') }
  finally { loading.value = false }
}

function statusText(status) {
  return { paid: '已支付', shipped: '已发货', completed: '已完成', cancelled: '已取消' }[status] || status
}
</script>

<style scoped>
.orders-page { background: #fff; padding: 30px; border-radius: 8px; }
.orders-page h2 { margin-bottom: 20px; }
.inline-skeleton { padding: 0; }
.status-tabs { display: flex; gap: 20px; margin-bottom: 20px; }
.status-tabs span { cursor: pointer; color: #666; }
.status-tabs span.active { color: #409eff; font-weight: bold; }
.order-item { border: 1px solid #eee; border-radius: 8px; padding: 16px; margin-bottom: 12px; cursor: pointer; }
.order-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.order-status { color: #409eff; }
.order-body { display: flex; justify-content: space-between; font-size: 18px; color: #e4393c; font-weight: bold; }
.order-time { font-size: 12px; color: #999; font-weight: normal; }
.empty { text-align: center; color: #999; padding: 60px 0; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>
