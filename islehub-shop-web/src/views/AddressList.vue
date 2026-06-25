<template>
  <div class="address-page">
    <div class="page-header">
      <h2>收货地址</h2>
      <el-button type="primary" @click="openDialog(null)">新增地址</el-button>
    </div>
    <p class="tip">提示：双击地址卡片可快速设为默认收货地址</p>
    <div v-if="addresses.length === 0" class="empty">暂无收货地址</div>
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
          <el-button text type="primary" @click="openDialog(addr)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(addr.id)">删除</el-button>
        </div>
      </div>
    </div>
    <el-dialog :title="editingAddr ? '编辑地址' : '新增地址'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="form.receiverName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.receiverPhone" /></el-form-item>
        <el-form-item label="省"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="区"><el-input v-model="form.district" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detail" /></el-form-item>
        <el-form-item label="默认地址"><el-switch v-model="form.isDefaultBool" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAddresses, addAddress, updateAddress, deleteAddress } from '../api/address'

const addresses = ref([])
const selectedId = ref(null)
const dialogVisible = ref(false)
const editingAddr = ref(null)
const form = reactive({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detail: '', isDefaultBool: false })

onMounted(fetchAddresses)
async function fetchAddresses() {
  try { addresses.value = (await getAddresses()).data || [] } catch { ElMessage.error('加载地址失败') }
}

function openDialog(addr) {
  editingAddr.value = addr
  if (addr) {
    Object.assign(form, addr)
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
      detail: '',
      isDefaultBool: false
    })}
  dialogVisible.value = true
}

async function handleSave() {
  try {
    const data = { ...form, isDefault: form.isDefaultBool ? 1 : 0 }
    if (editingAddr.value) {
      await updateAddress(editingAddr.value.id, data)
    } else {
      await addAddress(data)
    }
    dialogVisible.value = false
    await fetchAddresses()
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
}

async function handleDelete(id) {

  try {
    await deleteAddress(id)
    await fetchAddresses()
    ElMessage.success('已删除')
  } catch { ElMessage.error('删除失败') }
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.address-card {
  position: relative;
  border: 2px solid #eee;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}
.address-card:hover {
  border-color: #c6e2ff;
  background: #fafcff;
  user-select: none;
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
