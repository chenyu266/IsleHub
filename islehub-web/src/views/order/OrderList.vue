<template>
  <div>
    <el-card>
      <el-form :inline="true">
        <el-form-item>
          <el-input v-model="params.orderNo" placeholder="订单号" clearable @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item>
          <el-radio-group v-model="params.status" @change="fetchData">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="pending">待付款</el-radio-button>
            <el-radio-button value="paid">已付款</el-radio-button>
            <el-radio-button value="shipped">已发货</el-radio-button>
            <el-radio-button value="completed">已完成</el-radio-button>
            <el-radio-button value="cancelled">已取消</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            @change="handleDateChange" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:15px">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="totalAmount" label="金额" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiverName" label="收货人" width="100" />
        <el-table-column prop="receiverPhone" label="手机" width="120" />
        <el-table-column prop="receiverAddress" label="地址" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$router.push(`/order/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="params.page"
        :page-size="params.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
        style="margin-top:15px;justify-content:flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageOrders, exportOrders } from '../../api/order'

const dateRange = ref(null)
const params = ref({ page: 1, pageSize: 20, orderNo: '', status: '', startDate: '', endDate: '' })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

function statusType(s) {
  const map = { pending: 'warning', paid: '', shipped: 'primary', completed: 'success', cancelled: 'danger' }
  return map[s] || 'info'
}
function statusLabel(s) {
  const map = { pending: '待付款', paid: '已付款', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
  return map[s] || s
}

function handleDateChange(val) {
  params.value.startDate = val ? val[0] : ''
  params.value.endDate = val ? val[1] : ''
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await pageOrders(params.value)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function handleExport() {
  try {
    const res = await exportOrders(params.value)
    const url = window.URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url; a.download = 'orders.xlsx'; a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

fetchData()
</script>
