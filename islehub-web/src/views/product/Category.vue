<template>
  <div>
    <el-card>
      <el-button type="success" @click="openAdd()">新增分类</el-button>
    </el-card>

    <el-card style="margin-top:15px">
      <el-table :data="treeData" border stripe row-key="id" default-expand-all v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openAdd(row)">添加子级</el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="删除分类会同时删除子分类，确定吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px">
      <el-form ref="formRef" :model="form" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryTree, addCategory, updateCategory, deleteCategory } from '../../api/product'

const treeData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const form = ref({ id: null, name: '', parentId: 0, sortOrder: 0 })

async function fetchData() {
  loading.value = true
  try {
    const res = await getCategoryTree()
    treeData.value = res.data
  } finally { loading.value = false }
}

function openAdd(parent) {
  isEdit.value = false
  dialogTitle.value = parent ? '新增子分类' : '新增顶级分类'
  form.value = { id: null, name: '', parentId: parent ? parent.id : 0, sortOrder: 0 }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑分类'
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (isEdit.value) {
      await updateCategory(form.value.id, form.value)
    } else {
      await addCategory(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } catch { ElMessage.error('保存失败') }
}

async function handleDelete(id) {
  await deleteCategory(id)
  ElMessage.success('删除成功')
  fetchData()
}

fetchData()
</script>
