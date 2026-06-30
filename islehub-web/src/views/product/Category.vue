<template>
  <div>
    <el-card>
      <el-button type="success" @click="openAdd()">新增分类</el-button>
    </el-card>

    <el-card style="margin-top:15px">
      <div class="cat-tree" v-loading="loading">
        <!-- 表头 -->
        <div class="cat-tree__header">
          <span class="cat-tree__header-name">分类名称</span>
          <span class="cat-col cat-col--id">ID</span>
          <span class="cat-col cat-col--sort">排序</span>
          <span class="cat-col cat-col--actions">操作</span>
        </div>
        <!-- 树体 -->
        <div class="cat-tree__body">
          <CategoryTreeNode
            v-for="(node, i) in treeData"
            :key="node.id"
            :node="node"
            :depth="0"
            :is-last="i == treeData.length - 1"
          />
        </div>
      </div>
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
import { ref, provide } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryTree, addCategory, updateCategory, deleteCategory } from '../../api/product'
import CategoryTreeNode from './CategoryTreeNode.vue'

const treeData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const form = ref({ id: null, name: '', parentId: 0, sortOrder: 0 })

// 通过 provide/inject 把操作方法下发给递归节点
provide('categoryActions', {
  add: (node) => openAdd(node),
  edit: (node) => openEdit(node),
  del: (id) => handleDelete(id)
})

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

<style scoped>
/* ---------- 树容器 ---------- */
.cat-tree {
  --cat-gap: 10px;                 /* 父子分组行间留白，被节点继承 */
  --cat-col-id: 64px;
  --cat-col-sort: 70px;
  --cat-col-actions: 240px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: visible;
}

/* ---------- 表头 ---------- */
.cat-tree__header {
  display: flex;
  align-items: center;
  min-height: 42px;
  padding: 8px 12px;
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
  font-weight: 600;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.cat-tree__header-name {
  flex: 1;
  margin-left: 20px;   /* 与节点 toggle 占位对齐 */
}

/* ---------- 树体：顶层节点之间同样留白，区分不同父级子列表 ---------- */
.cat-tree__body {
  display: flex;
  flex-direction: column;
  gap: var(--cat-gap);
  overflow: visible;
}

/* 让表头固定列宽与节点保持一致 */
.cat-col {
  flex-shrink: 0;
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.cat-col--id {
  width: var(--cat-col-id);
}
.cat-col--sort {
  width: var(--cat-col-sort);
}
.cat-col--actions {
  width: var(--cat-col-actions);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
}
</style>
