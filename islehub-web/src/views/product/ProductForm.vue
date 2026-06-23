<template>
  <div>
    <el-card>
      <h3>{{ isEdit ? '编辑商品' : '新增商品' }}</h3>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width:800px;margin-top:20px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-tree-select v-model="form.categoryId" :data="categories" :props="{ value: 'id', label: 'name', children: 'children' }"
            check-strictly placeholder="选择分类" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="主图">
          <el-upload
            class="main-image-upload"
            :action="'/api/upload/image'"
            :headers="{ 'islehub-token': token }"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            :before-upload="beforeUpload"
            :file-list="fileList"
            list-type="picture"
            :limit="1"
            accept="image/*"
          >
            <el-button type="primary" :disabled="form.mainImage !== ''">选择图片</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 JPG/PNG/GIF 格式，大小不超过 10MB</div>
            </template>
          </el-upload>
          <div v-if="form.mainImage" style="margin-top:10px">
            <img :src="form.mainImage" style="max-width:200px;max-height:200px" />
            <el-button type="danger" size="small" style="margin-left:10px" @click="removeImage">移除图片</el-button>
          </div>
        </el-form-item>
        <el-form-item label="上下架">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:15px">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <h3>SKU 管理</h3>
        <el-button type="success" @click="addSku">新增 SKU</el-button>
      </div>
      <el-table :data="skus" border stripe style="margin-top:15px">
        <el-table-column label="规格" width="200">
          <template #default="{ row }">
            <el-input v-model="row.spec" placeholder="如：红色/XL" />
          </template>
        </el-table-column>
        <el-table-column label="编码" width="200">
          <template #default="{ row }">
            <el-input v-model="row.skuCode" placeholder="SKU编码" />
          </template>
        </el-table-column>
        <el-table-column label="价格" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ $index }">
            <el-button type="danger" size="small" @click="removeSku($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div style="margin-top:20px">
      <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      <el-button @click="$router.back()">取消</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProduct, addProduct, updateProduct, getCategoryTree } from '../../api/product'

const route = useRoute()
const router = useRouter()
const isEdit = ref(!!route.params.id)
const submitting = ref(false)
const categories = ref([])
const token = ref(localStorage.getItem('token') || '')
const fileList = ref([])
const form = ref({
  name: '', categoryId: null, description: '', mainImage: '', status: 1
})
const skus = ref([])

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function addSku() {
  skus.value.push({ spec: '', skuCode: '', price: 0, stock: 0, status: 1 })
}

function removeSku(index) { skus.value.splice(index, 1) }

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

function onUploadSuccess(response) {
  if (response.code === 200) {
    form.value.mainImage = response.data
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function onUploadError() {
  ElMessage.error('上传失败，请检查网络')
}

function removeImage() {
  form.value.mainImage = ''
  fileList.value = []
}

onMounted(async () => {
  try {
    const res = await getCategoryTree()
    categories.value = res.data
  } catch { ElMessage.error('加载分类失败') }
  if (isEdit.value) {
    try {
      const res = await getProduct(route.params.id)
      Object.assign(form.value, res.data)
      if (res.data.skus) skus.value = res.data.skus
    } catch { ElMessage.error('加载商品失败') }
  }
})

async function handleSubmit() {
  submitting.value = true
  try {
    const data = { product: form.value, skus: skus.value }
    if (isEdit.value) {
      await updateProduct(route.params.id, data)
    } else {
      await addProduct(data)
    }
    ElMessage.success('保存成功')
    router.push('/product')
  } catch { ElMessage.error('保存失败') }
  finally { submitting.value = false }
}
</script>
