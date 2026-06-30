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
  if (!skus.length) return '--'
  return Math.min(...skus.map(s => Number(s.price || 0))).toFixed(2)
})

watch(() => props.product?.mainImage, () => {
  imageFailed.value = false
})

function goDetail() {
  if (props.product?.id) router.push(`/product/${props.product.id}`)
}
</script>

<style scoped>
.product-card { background: #fff; border-radius: 8px; overflow: hidden; cursor: pointer; transition: all 0.3s; outline: none; }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.product-card:focus-visible { box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.22), 0 4px 16px rgba(0,0,0,0.12); }
.card-image { height: 200px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; }
.card-image img { width: 100%; height: 100%; object-fit: cover; }
.placeholder-img { color: #b8bec8; font-size: 14px; }
.card-info { padding: 12px; min-height: 78px; }
.card-name {
  min-height: 40px;
  font-size: 14px;
  color: #333;
  line-height: 20px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.card-price { font-size: 18px; color: #e4393c; font-weight: bold; margin-top: 6px; }
</style>
