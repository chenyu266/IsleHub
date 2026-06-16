<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ productCount }}</div>
          <div class="stat-label">商品总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ todayOrderCount }}</div>
          <div class="stat-label">今日订单</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ pendingShipCount }}</div>
          <div class="stat-label">待发货</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ userCount }}</div>
          <div class="stat-label">用户数</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pageProducts } from '../api/product'
import { pageOrders } from '../api/order'
import { pageUsers } from '../api/user'

const productCount = ref(0)
const todayOrderCount = ref(0)
const pendingShipCount = ref(0)
const userCount = ref(0)

onMounted(async () => {
  const today = new Date().toISOString().slice(0, 10)

  const [r1, r2, r3, r4] = await Promise.allSettled([
    pageProducts({ page: 1, pageSize: 1 }, { silent: true }),
    pageOrders({ page: 1, pageSize: 1, startDate: today, endDate: today }, { silent: true }),
    pageOrders({ page: 1, pageSize: 1, status: 'paid' }, { silent: true }),
    pageUsers({ page: 1, pageSize: 1 }, { silent: true })
  ])
  productCount.value = r1.status === 'fulfilled' ? r1.value.data.total : 0
  todayOrderCount.value = r2.status === 'fulfilled' ? r2.value.data.total : 0
  pendingShipCount.value = r3.status === 'fulfilled' ? r3.value.data.total : 0
  userCount.value = r4.status === 'fulfilled' ? r4.value.data.total : 0
})
</script>

<style scoped>
.stat-card { text-align: center; padding: 20px; }
.stat-value { font-size: 36px; font-weight: bold; color: #409eff; }
.stat-label { font-size: 14px; color: #909399; margin-top: 10px; }
</style>
