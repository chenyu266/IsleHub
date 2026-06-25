<template>
  <div class="product-card" @click="$router.push(`/product/${product.id}`)">
    <div class="card-image">
      <img v-if="product.mainImage" :src="product.mainImage" alt="" />
      <span v-else class="placeholder-img">暂无图片</span>
    </div>
    <div class="card-info">
      <div class="card-name">{{ product.name }}</div>
      <div class="card-price">¥{{ product.skus && product.skus.length > 0 ? (Math.min(...product.skus.map(s => Number(s.price || 0))).toFixed(2)) : '--' }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({ product: Object })
</script>

<style scoped>
.product-card { background: #fff; border-radius: 8px; overflow: hidden; cursor: pointer; transition: all 0.3s; user-select: none;}
.product-card:hover { transform: translateY(-4px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.card-image { height: 200px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; }
.card-image img { width: 100%; height: 100%; object-fit: cover; }
.placeholder-img { color: #ccc; font-size: 14px; }
.card-info { padding: 12px; }
.card-name { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-price { font-size: 18px; color: #e4393c; font-weight: bold; margin-top: 6px; }
</style>
