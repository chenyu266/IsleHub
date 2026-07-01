<template>
  <div class="sku-selector">
    <button
      v-for="sku in skus"
      :key="sku.id"
      type="button"
      class="sku-item"
      :class="{ selected: selectedSkuId === sku.id }"
      :aria-pressed="selectedSkuId === sku.id"
      :disabled="Number(sku.stock) <= 0"
      @click="select(sku)"
    >
      <div class="sku-spec">{{ sku.spec }}</div>
      <div class="sku-price">¥{{ sku.price }}</div>
      <div class="sku-stock">库存: {{ sku.stock }}</div>
    </button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  skus: {
    type: Array,
    default: () => []
  },
  selectedSkuId: {
    type: [Number, String],
    default: null
  }
})
const emit = defineEmits(['select'])
const selectedSkuId = ref(null)

watch(() => props.selectedSkuId, value => {
  selectedSkuId.value = value
}, { immediate: true })

function select(sku) {
  if (Number(sku.stock) <= 0) return
  selectedSkuId.value = sku.id
  emit('select', sku)
}
</script>

<style scoped>
.sku-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.sku-item {
  min-width: 128px;
  padding: 12px 16px;
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius-sm);
  background: var(--shop-surface);
  cursor: pointer;
  text-align: center;
  font: inherit;
  transition: border-color var(--shop-transition), background var(--shop-transition), box-shadow var(--shop-transition), transform var(--shop-transition);
}

.sku-item:hover {
  border-color: rgba(37, 99, 235, .36);
  box-shadow: 0 6px 18px rgba(31, 41, 55, .07);
}

.sku-item.selected {
  border-color: var(--shop-primary);
  background: var(--shop-primary-soft);
  box-shadow: 0 0 0 1px rgba(37, 99, 235, .12);
}

.sku-item:disabled {
  cursor: not-allowed;
  opacity: .55;
  background: var(--shop-surface-muted);
}

.sku-spec {
  font-size: 14px;
  color: var(--shop-text);
  font-weight: 700;
}

.sku-price {
  margin: 5px 0;
  font-size: 16px;
  color: var(--shop-price);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.sku-stock {
  font-size: 12px;
  color: var(--shop-text-subtle);
}
</style>
