<template>
  <div>
    <el-card>
      <el-form :inline="true">
        <el-form-item>
          <el-input v-model="params.keyword" placeholder="搜索商品名称" clearable @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="params.categoryId" placeholder="分类" clearable @change="fetchData">
            <el-option v-for="c in flatCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-radio-group v-model="params.status" @change="fetchData">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="1">上架</el-radio-button>
            <el-radio-button :value="0">下架</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button type="success" @click="$router.push('/product/add')">新增商品</el-button>
          <el-button type="warning" :disabled="selectedRows.length === 0" @click="handleBatchStatus(1)">批量上架</el-button>
          <el-button type="info" :disabled="selectedRows.length === 0" @click="handleBatchStatus(0)">批量下架</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:15px">
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="(rows) => selectedRows = rows">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$router.push(`/product/${row.id}`)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-popconfirm title="确定删除吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageProducts, deleteProduct, updateProductStatus, batchProductStatus, getCategoryTree } from '../../api/product'

const params = ref({ page: 1, pageSize: 20, keyword: '', categoryId: null, status: undefined })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selectedRows = ref([])
const flatCategories = ref([])

function flattenTree(nodes, prefix = '') {
  const result = []
  nodes.forEach(n => {
    result.push({ id: n.id, name: prefix + n.name })
    if (n.children) result.push(...flattenTree(n.children, prefix + '-- '))
  })
  return result
}

onMounted(async () => {
  try {
    const res = await getCategoryTree()
    flatCategories.value = flattenTree(res.data)
  } catch { ElMessage.error('加载分类失败') }
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageProducts(params.value)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function toggleStatus(row) {
  const status = row.status === 1 ? 0 : 1
  await updateProductStatus(row.id, status)
  ElMessage.success('操作成功')
  fetchData()
}

async function handleBatchStatus(status) {
  const ids = selectedRows.value.map(r => r.id)
  await batchProductStatus({ ids, status })
  ElMessage.success('批量操作成功')
  fetchData()
}

async function handleDelete(id) {
  await deleteProduct(id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>
