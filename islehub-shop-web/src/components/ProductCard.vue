<template>
  <div
    class="product-card"
    role="link"
    tabindex="0"
    @click="goDetail"
    @keydown.enter.prevent="goDetail"
    @keydown.space.prevent="goDetail"
  >
    <div class="card-image">
      <img
        v-if="showImage"
        :src="product.mainImage"
        :alt="product.name || '商品图片'"
        loading="lazy"
        @error="imageFailed = true"
      />
      <span v-else class="placeholder-img">暂无图片</span>
    </div>
    <div class="card-info">
      <div class="card-name">{{ product.name }}</div>
      <div class="card-price">¥{{ minPrice }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({ product: Object })
const router = useRouter()
const imageFailed = ref(false)

const showImage = computed(() => Boolean(props.product?.mainImage) && !imageFailed.value)
const minPrice = computed(() => {
  const skus = props.product?.skus || []
  const prices = skus
    .map(s => Number(s.price))
    .filter(price => Number.isFinite(price) && price >= 0)
  if (!prices.length) return '--'
  return Math.min(...prices).toFixed(2)
})

watch(() => props.product?.mainImage, () => {
  imageFailed.value = false
})

function goDetail() {
  if (props.product?.id) router.push(`/product/${props.product.id}`)
}
</script>

<style scoped>
.product-card {
  background: var(--shop-surface);
  border: none;
  border-radius: var(--shop-radius);
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--shop-transition), box-shadow var(--shop-transition), border-color var(--shop-transition);
  outline: none;
  box-shadow: 0 8px 24px rgba(31, 41, 55, .08);
}
.product-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shop-shadow-hover);
}
.product-card:focus-visible {
  box-shadow: var(--shop-focus), var(--shop-shadow-hover);
}
.card-image {
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: #eef2f6;
  border-bottom: 1px solid var(--shop-border);
}
.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform .2s ease;
}
.product-card:hover .card-image img { transform: scale(1.025); }
.placeholder-img {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--shop-text-subtle);
  font-size: 16px;
  background: #f3f6fa;
}
.card-info { padding: 16px 14px 14px; min-height: 104px; }
.card-name {
  min-height: 56px;
  font-size: 20px;
  color: var(--shop-text);
  line-height: 28px;
  font-weight: 700;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.card-price {
  font-size: 18px;
  color: var(--shop-price);
  font-weight: 800;
  margin-top: 8px;
  font-variant-numeric: tabular-nums;
}
</style>
