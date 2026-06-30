<template>
  <div class="address-page">
    <div class="page-header">
      <h2>收货地址</h2>
      <el-button type="primary" @click="openDialog(null)">新增地址</el-button>
    </div>
    <p class="tip">提示：双击地址卡片可快速设为默认收货地址</p>
    <PageSkeleton v-if="loading" variant="address-list" :rows="3" class="inline-skeleton" />
    <div v-else-if="addresses.length === 0" class="empty">暂无收货地址</div>
    <div class="address-list" v-else>
      <div class="address-card" v-for="addr in addresses" :key="addr.id"
           :class="{ 'is-selected': selectedId === addr.id, 'is-default': addr.isDefault === 1 }"
           @click="selectedId = addr.id"
           @dblclick="handleSetDefault(addr)">
        <span v-if="selectedId === addr.id" class="check-mark"></span>

        <div class="addr-header">
          <b>{{ addr.receiverName }}</b> {{ addr.receiverPhone }}
          <el-tag v-if="addr.isDefault === 1" size="small" type="danger">默认</el-tag>
        </div>
        <div class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</div>
        <div class="addr-actions">
          <el-button text type="primary" @click.stop="openDialog(addr)">编辑</el-button>
          <el-button text type="danger" :loading="deletingId === addr.id" @click.stop="handleDelete(addr)">删除</el-button>
        </div>
      </div>
    </div>
    <el-dialog :title="editingAddr ? '编辑地址' : '新增地址'" v-model="dialogVisible" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px" status-icon>
        <el-form-item label="收货人" prop="receiverName"><el-input v-model="form.receiverName" placeholder="请输入收货人姓名" /></el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入11位手机号" />
        </el-form-item>
        <el-form-item label="地区" prop="region">
          <el-cascader
            v-model="form.region"
            :options="chinaRegionOptions"
            :props="regionProps"
            placeholder="请选择省 / 市 / 区"
            filterable
            clearable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="默认地址"><el-switch v-model="form.isDefaultBool" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { getAddresses, addAddress, updateAddress, deleteAddress } from '../api/address'
import PageSkeleton from '../components/PageSkeleton.vue'

const addresses = ref([])
const selectedId = ref(null)
const dialogVisible = ref(false)
const editingAddr = ref(null)
const loading = ref(true)
const saving = ref(false)
const deletingId = ref(null)
const formRef = ref(null)
const chinaRegionOptions = ref([])
let validateRegionPath = null
const regionProps = { expandTrigger: 'hover', emitPath: true }
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  region: [],
  detail: '',
  isDefaultBool: false
})
const formRules = {
  receiverName: [
    { validator: validateReceiverName, trigger: ['blur', 'change'] }
  ],
  receiverPhone: [
    { validator: validatePhone, trigger: ['blur', 'change'] }
  ],
  region: [
    { validator: validateRegion, trigger: 'change' }
  ],
  detail: [
    { validator: validateDetail, trigger: ['blur', 'change'] }
  ]
}

onMounted(fetchAddresses)
async function fetchAddresses() {
  loading.value = true
  try { addresses.value = (await getAddresses()).data || [] } catch { ElMessage.error('加载地址失败') }
  finally { loading.value = false }
}

async function ensureRegionsLoaded() {
  if (chinaRegionOptions.value.length && validateRegionPath) return
  const regionModule = await import('../data/chinaRegions')
  chinaRegionOptions.value = regionModule.chinaRegionOptions
  validateRegionPath = regionModule.isValidRegionPath
}

async function openDialog(addr) {
  try {
    await ensureRegionsLoaded()
  } catch {
    ElMessage.error('加载地区数据失败')
    return
  }
  editingAddr.value = addr
  if (addr) {
    Object.assign(form, addr)
    form.region = [addr.province, addr.city, addr.district].filter(Boolean)
    form.isDefaultBool = addr.isDefault === 1
  } else {
    Object.assign(form, {
      id: undefined,
      userId: undefined,
      isDefault: undefined,
      createdAt: undefined,
      updatedAt: undefined,
      receiverName: '',
      receiverPhone: '',
      province: '',
      city: '',
      district: '',
      region: [],
      detail: '',
      isDefaultBool: false
    })}
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleSave() {
  if (saving.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const [province, city, district] = form.region
    const data = {
      receiverName: String(form.receiverName || '').trim(),
      receiverPhone: String(form.receiverPhone || '').trim(),
      province,
      city,
      district,
      detail: String(form.detail || '').trim(),
      isDefault: form.isDefaultBool ? 1 : 0
    }
    if (editingAddr.value) {
      await updateAddress(editingAddr.value.id, data)
    } else {
      await addAddress(data)
    }
    dialogVisible.value = false
    await fetchAddresses()
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

function validateReceiverName(_rule, value, callback) {
  if (!String(value || '').trim()) {
    callback(new Error('收货人不能为空'))
    return
  }
  callback()
}

function validatePhone(_rule, value, callback) {
  const phone = String(value || '').trim()
  if (!phone) {
    callback(new Error('请输入手机号'))
    return
  }
  if (!/^\d{11}$/.test(phone)) {
    callback(new Error('手机号必须为11位数字'))
    return
  }
  callback()
}

function validateRegion(_rule, value, callback) {
  if (!validateRegionPath || !validateRegionPath(value)) {
    callback(new Error('请选择省 / 市 / 区'))
    return
  }
  callback()
}

function validateDetail(_rule, value, callback) {
  if (!String(value || '').trim()) {
    callback(new Error('详细地址不能为空'))
    return
  }
  callback()
}

async function handleDelete(addr) {
  if (deletingId.value) return
  try {
    await ElMessageBox.confirm(`确定删除「${addr.receiverName} ${addr.receiverPhone}」这条地址吗？`, '删除地址', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  deletingId.value = addr.id
  try {
    await deleteAddress(addr.id)
    await fetchAddresses()
    ElMessage.success('已删除')
  } catch { ElMessage.error('删除失败') }
  finally { deletingId.value = null }
}

async function handleSetDefault(addr) {
  if (addr.isDefault === 1) {
    ElMessage.info('该地址已是默认地址')
    return
  }
  try {
    await updateAddress(addr.id, { isDefault: 1 })
    await fetchAddresses()
    ElMessage.success('已设为默认地址')
  } catch {
    ElMessage.error('设置失败')
  }
}
</script>

<style scoped>
.tip { color: #999; font-size: 12px; margin: 8px 0; }
.address-page { background: #fff; padding: 30px; border-radius: 8px; }
.inline-skeleton { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.address-card {
  position: relative;
  border: 2px solid #eee;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.address-card:hover {
  border-color: #c6e2ff;
  background: #fafcff;
}
.address-card.is-selected {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
}
.address-card.is-default:not(.is-selected) {
  border-color: #fdd;
}
.check-mark {
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-top: 28px solid #409eff;
  border-left: 28px solid transparent;
}
.check-mark::after {
  content: '';
  position: absolute;
  top: -26px;
  right: 4px;
  width: 6px;
  height: 11px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.addr-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.addr-detail { color: #666; font-size: 14px; }
.addr-actions { margin-top: 8px; display: flex; gap: 8px; }
.empty { text-align: center; color: #999; padding: 60px 0; }
</style>
