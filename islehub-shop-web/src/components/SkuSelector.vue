<template>
  <div class="sku-selector">
    <div class="sku-item" v-for="sku in skus" :key="sku.id"
         :class="{ selected: selectedSkuId === sku.id }"
         @click="select(sku)">
      <div class="sku-spec">{{ sku.spec }}</div>
      <div class="sku-price">¥{{ sku.price }}</div>
      <div class="sku-stock">库存: {{ sku.stock }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({ skus: Array })
const emit = defineEmits(['select'])
const selectedSkuId = ref(null)

function select(sku) {
  selectedSkuId.value = sku.id
  emit('select', sku)
}
</script>

<style scoped>
.sku-selector { display: flex; flex-wrap: wrap; gap: 12px; }
.sku-item { padding: 10px 16px; border: 2px solid #e4e7ed; border-radius: 8px; cursor: pointer; min-width: 120px; text-align: center; }
.sku-item.selected { border-color: #409eff; background: #ecf5ff; }
.sku-spec { font-size: 14px; color: #333; }
.sku-price { font-size: 16px; color: #e4393c; font-weight: bold; margin: 4px 0; }
.sku-stock { font-size: 12px; color: #999; }
</style>
