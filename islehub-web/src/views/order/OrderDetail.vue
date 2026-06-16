<template>
  <div v-loading="loading">
    <el-button @click="$router.back()" style="margin-bottom:15px">返回</el-button>

    <el-card v-if="order">
      <h3>订单信息</h3>
      <el-descriptions :column="2" border style="margin-top:15px">
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总金额">{{ order.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ order.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ order.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ order.remark || '无' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:20px">
        <el-button v-if="order.status === 'pending'" type="success" @click="handleUpdateStatus('paid')">标记已付款</el-button>
        <el-button v-if="order.status === 'paid'" type="primary" @click="openShippingDialog">录入发货</el-button>
        <el-button v-if="order.status === 'shipped'" type="success" @click="handleUpdateStatus('completed')">标记已完成</el-button>
        <el-button v-if="order.status === 'pending'" type="danger" @click="handleCancel">取消订单</el-button>
      </div>
    </el-card>

    <el-card style="margin-top:15px" v-if="order?.items?.length">
      <h3>商品明细</h3>
      <el-table :data="order.items" border stripe style="margin-top:15px">
        <el-table-column prop="productName" label="商品" />
        <el-table-column prop="skuSpec" label="规格" width="150" />
        <el-table-column prop="price" label="单价" width="100" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="小计" width="120">
          <template #default="{ row }">{{ (row.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top:15px" v-if="order?.shipping">
      <h3>物流信息</h3>
      <el-descriptions :column="2" border style="margin-top:15px">
        <el-descriptions-item label="快递公司">{{ order.shipping.carrier }}</el-descriptions-item>
        <el-descriptions-item label="运单号">{{ order.shipping.trackingNo }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ order.shipping.shippedAt }}</el-descriptions-item>
        <el-descriptions-item label="签收时间">{{ order.shipping.deliveredAt || '未签收' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog title="录入发货信息" v-model="shippingVisible" width="400px">
      <el-form :model="shippingForm" label-width="80px">
        <el-form-item label="快递公司">
          <el-select v-model="shippingForm.carrier">
            <el-option label="顺丰" value="顺丰" />
            <el-option label="中通" value="中通" />
            <el-option label="圆通" value="圆通" />
            <el-option label="韵达" value="韵达" />
            <el-option label="邮政" value="邮政" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="shippingForm.trackingNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shippingVisible = false">取消</el-button>
        <el-button type="primary" @click="handleShipping">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, updateOrderStatus, cancelOrder, addShipping } from '../../api/order'

const route = useRoute()
const loading = ref(false)
const order = ref(null)
const shippingVisible = ref(false)
const shippingForm = ref({ carrier: '顺丰', trackingNo: '' })

function statusType(s) {
  const map = { pending: 'warning', paid: '', shipped: 'primary', completed: 'success', cancelled: 'danger' }
  return map[s] || 'info'
}
function statusLabel(s) {
  const map = { pending: '待付款', paid: '已付款', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
  return map[s] || s
}

async function fetchData() {
  loading.value = true
  try { order.value = (await getOrder(route.params.id)).data }
  finally { loading.value = false }
}

async function handleUpdateStatus(status) {
  await updateOrderStatus(route.params.id, { status })
  ElMessage.success('操作成功')
  fetchData()
}

async function handleCancel() {
  await ElMessageBox.confirm('确定取消该订单吗？')
  await cancelOrder(route.params.id)
  ElMessage.success('订单已取消')
  fetchData()
}

async function handleShipping() {
  await addShipping(route.params.id, shippingForm.value)
  ElMessage.success('发货成功')
  shippingVisible.value = false
  fetchData()
}

function openShippingDialog() {
  shippingForm.value = { carrier: '顺丰', trackingNo: '' }
  shippingVisible.value = true
}

onMounted(fetchData)
</script>
