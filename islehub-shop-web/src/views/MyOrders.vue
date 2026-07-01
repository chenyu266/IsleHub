<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    <div class="status-tabs">
      <button type="button" :class="{ active: !currentStatus }" @click="currentStatus = null">全部</button>
      <button type="button" :class="{ active: currentStatus === 'paid' }" @click="currentStatus = 'paid'">已支付</button>
      <button type="button" :class="{ active: currentStatus === 'shipped' }" @click="currentStatus = 'shipped'">已发货</button>
      <button type="button" :class="{ active: currentStatus === 'completed' }" @click="currentStatus = 'completed'">已完成</button>
    </div>
    <PageSkeleton v-if="loading" variant="order-list" :rows="4" class="inline-skeleton" />
    <div v-else-if="orders.length === 0" class="empty">暂无订单</div>
    <template v-else>
      <router-link class="order-item" v-for="order in orders" :key="order.id" :to="`/order/${order.id}`">
        <div class="order-header">
          <span>订单号: {{ order.orderNo }}</span>
          <span class="order-status">{{ statusText(order.status) }}</span>
        </div>
        <div class="order-body">
          <span>¥{{ order.totalAmount }}</span>
          <span class="order-time">{{ order.createdAt }}</span>
        </div>
      </router-link>
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
.orders-page {
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  padding: 30px;
  box-shadow: var(--shop-shadow-sm);
}

.orders-page h2 {
  margin: 0 0 22px;
  color: var(--shop-text);
  font-size: 24px;
}

.inline-skeleton { padding: 0; }

.status-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 22px;
}

.status-tabs button {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border: 1px solid var(--shop-border);
  border-radius: 999px;
  background: var(--shop-surface);
  color: var(--shop-text-muted);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  transition: border-color var(--shop-transition), background var(--shop-transition), color var(--shop-transition);
}

.status-tabs button:hover,
.status-tabs button.active {
  border-color: var(--shop-primary);
  background: var(--shop-primary-soft);
  color: var(--shop-primary);
}

.order-item {
  display: block;
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius-sm);
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  color: inherit;
  text-decoration: none;
  transition: border-color var(--shop-transition), box-shadow var(--shop-transition), transform var(--shop-transition);
}

.order-item:hover {
  transform: translateY(-1px);
  border-color: rgba(37,99,235,.28);
  box-shadow: var(--shop-shadow-sm);
}

.order-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 10px;
  color: var(--shop-text-muted);
  font-size: 13px;
}

.order-status {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--shop-primary-soft);
  color: var(--shop-primary);
  font-weight: 800;
}

.order-body {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  color: var(--shop-price);
  font-size: 18px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.order-time {
  color: var(--shop-text-subtle);
  font-size: 12px;
  font-weight: 400;
}

.empty {
  text-align: center;
  color: var(--shop-text-muted);
  padding: 64px 0;
  border: 1px dashed var(--shop-border-strong);
  border-radius: var(--shop-radius);
  background: var(--shop-surface-muted);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}
</style>
