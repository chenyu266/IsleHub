<template>
  <el-skeleton animated class="page-skeleton">
    <template #template>
      <div v-if="variant === 'category-nav'" class="category-lines">
        <el-skeleton-item v-for="i in rows" :key="i" variant="text" class="category-line" />
      </div>

      <div v-else-if="variant === 'banner'" class="banner-skeleton">
        <div class="banner-copy">
          <el-skeleton-item variant="text" class="eyebrow-line" />
          <el-skeleton-item variant="h1" class="hero-title-line" />
          <el-skeleton-item variant="text" class="hint-line" />
        </div>
        <el-skeleton-item variant="image" class="banner-image" />
      </div>

      <div v-else-if="variant === 'user-panel'" class="user-panel-skeleton">
        <div class="user-row">
          <el-skeleton-item variant="circle" class="avatar" />
          <div class="user-lines">
            <el-skeleton-item variant="text" class="user-name" />
            <el-skeleton-item variant="text" class="user-subtitle" />
          </div>
        </div>
        <el-skeleton-item variant="text" class="address-title" />
        <el-skeleton-item variant="text" class="address-line" />
        <el-skeleton-item variant="text" class="address-line short" />
      </div>

      <div v-else-if="variant === 'product-grid'" class="product-grid">
        <div v-for="i in rows" :key="i" class="product-card-skeleton">
          <el-skeleton-item variant="image" class="product-image" />
          <el-skeleton-item variant="text" class="product-name" />
          <el-skeleton-item variant="text" class="product-price" />
        </div>
      </div>

      <div v-else-if="variant === 'product-detail'" class="detail-skeleton">
        <el-skeleton-item variant="image" class="detail-image" />
        <div class="detail-copy">
          <el-skeleton-item variant="h1" class="detail-title" />
          <el-skeleton-item variant="h3" class="detail-price" />
          <el-skeleton-item variant="text" class="detail-subtitle" />
          <div class="sku-grid">
            <div v-for="i in 3" :key="i" class="sku-card">
              <el-skeleton-item variant="text" class="sku-name" />
              <el-skeleton-item variant="text" class="sku-price" />
              <el-skeleton-item variant="text" class="sku-stock" />
            </div>
          </div>
          <el-skeleton-item variant="text" class="detail-desc" />
          <el-skeleton-item variant="text" class="detail-desc short" />
        </div>
      </div>

      <div v-else-if="variant === 'cart-list'" class="cart-lines">
        <div v-for="i in rows" :key="i" class="cart-row">
          <el-skeleton-item variant="rect" class="checkbox-box" />
          <el-skeleton-item variant="image" class="cart-image" />
          <div class="cart-info">
            <el-skeleton-item variant="text" class="cart-name" />
            <el-skeleton-item variant="text" class="cart-spec" />
          </div>
          <el-skeleton-item variant="text" class="cart-price" />
          <el-skeleton-item variant="button" class="cart-quantity" />
          <el-skeleton-item variant="text" class="cart-subtotal" />
          <el-skeleton-item variant="text" class="cart-remove" />
        </div>
      </div>

      <div v-else-if="variant === 'checkout-addresses'" class="checkout-addresses">
        <div v-for="i in 2" :key="i" class="checkout-address">
          <el-skeleton-item variant="text" class="address-name" />
          <el-skeleton-item variant="text" class="address-detail" />
        </div>
      </div>

      <div v-else-if="variant === 'checkout-items'" class="checkout-items">
        <div v-for="i in rows" :key="i" class="checkout-item">
          <el-skeleton-item variant="image" class="checkout-image" />
          <el-skeleton-item variant="text" class="checkout-name" />
          <el-skeleton-item variant="text" class="checkout-price" />
        </div>
      </div>

      <div v-else-if="variant === 'order-list'" class="order-list">
        <div v-for="i in rows" :key="i" class="order-card">
          <div class="order-head">
            <el-skeleton-item variant="text" class="order-no" />
            <el-skeleton-item variant="text" class="order-status" />
          </div>
          <div class="order-body">
            <el-skeleton-item variant="text" class="order-amount" />
            <el-skeleton-item variant="text" class="order-time" />
          </div>
        </div>
      </div>

      <div v-else-if="variant === 'order-detail'" class="order-detail-skeleton">
        <el-skeleton-item variant="text" class="detail-row-wide" />
        <el-skeleton-item variant="text" class="detail-row-wide" />
        <el-skeleton-item variant="text" class="detail-row-wide" />
        <el-skeleton-item variant="text" class="detail-section-title" />
        <el-skeleton-item variant="text" class="detail-row" />
        <el-skeleton-item variant="text" class="detail-row short" />
        <el-skeleton-item variant="text" class="detail-section-title" />
        <el-skeleton-item v-for="i in 2" :key="i" variant="text" class="detail-row-wide" />
      </div>

      <div v-else-if="variant === 'address-list'" class="address-list-skeleton">
        <div v-for="i in rows" :key="i" class="address-card-skeleton">
          <el-skeleton-item variant="text" class="address-name" />
          <el-skeleton-item variant="text" class="address-detail" />
          <el-skeleton-item variant="text" class="address-action" />
        </div>
      </div>
    </template>
  </el-skeleton>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    required: true
  },
  rows: {
    type: Number,
    default: 4
  }
})
</script>

<style scoped>
.page-skeleton {
  width: 100%;
}
.category-lines {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 20px 0;
}
.category-line { width: 82%; height: 16px; }
.category-line:nth-child(2n) { width: 66%; }
.banner-skeleton {
  height: 100%;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  padding: 44px;
  border-radius: 12px;
  background: #fff;
}
.banner-copy { flex: 1; }
.eyebrow-line { width: 140px; height: 16px; }
.hero-title-line { width: min(360px, 75%); height: 42px; margin-top: 18px; }
.hint-line { width: 160px; height: 16px; margin-top: 18px; }
.banner-image { width: 38%; max-width: 300px; height: 220px; border-radius: 16px; }
.user-panel-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar { width: 42px; height: 42px; }
.user-lines { flex: 1; }
.user-name { width: 70%; }
.user-subtitle { width: 48%; margin-top: 8px; }
.address-title { width: 88px; margin-top: 10px; }
.address-line { width: 92%; }
.address-line.short { width: 66%; }
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.product-card-skeleton {
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
}
.product-image { width: 100%; height: 200px; }
.product-name { width: 84%; margin: 14px 12px 0; }
.product-price { width: 40%; margin: 10px 12px 14px; }
.detail-skeleton {
  display: flex;
  gap: 40px;
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}
.detail-image { width: 420px; height: 420px; border-radius: 8px; flex-shrink: 0; }
.detail-copy { flex: 1; }
.detail-title { width: 58%; height: 32px; }
.detail-price { width: 180px; height: 36px; margin-top: 18px; }
.detail-subtitle { width: 260px; margin-top: 14px; }
.sku-grid {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}
.sku-card {
  width: 120px;
  padding: 10px 16px;
  border-radius: 8px;
}
.sku-name { width: 78%; }
.sku-price { width: 60%; margin-top: 8px; }
.sku-stock { width: 70%; margin-top: 8px; }
.detail-desc { width: 86%; margin-top: 28px; }
.detail-desc.short { width: 56%; margin-top: 10px; }
.cart-lines {
  display: flex;
  flex-direction: column;
}
.cart-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #eee;
}
.checkbox-box { width: 14px; height: 14px; border-radius: 2px; }
.cart-image { width: 80px; height: 80px; border-radius: 4px; flex-shrink: 0; }
.cart-info { flex: 1; }
.cart-name { width: 68%; }
.cart-spec { width: 36%; margin-top: 8px; }
.cart-price,
.cart-subtotal { width: 86px; }
.cart-quantity { width: 88px; height: 28px; }
.cart-remove { width: 32px; }
.checkout-addresses,
.address-list-skeleton {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.checkout-address,
.address-card-skeleton {
  border-radius: 8px;
  padding: 12px;
  border: 2px solid #f0f2f5;
}
.address-name { width: 42%; }
.address-detail { width: 82%; margin-top: 10px; }
.address-action { width: 96px; margin-top: 12px; }
.checkout-items {
  display: flex;
  flex-direction: column;
}
.checkout-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}
.checkout-image { width: 50px; height: 50px; border-radius: 4px; }
.checkout-name { flex: 1; max-width: 460px; }
.checkout-price { width: 92px; margin-left: auto; }
.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.order-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
}
.order-head,
.order-body {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}
.order-body { margin-top: 12px; }
.order-no { width: 260px; }
.order-status { width: 68px; }
.order-amount { width: 90px; }
.order-time { width: 160px; }
.order-detail-skeleton {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.detail-row-wide { width: 100%; }
.detail-row { width: 72%; }
.detail-row.short { width: 46%; }
.detail-section-title {
  width: 96px;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .product-grid,
  .checkout-addresses,
  .address-list-skeleton {
    grid-template-columns: 1fr;
  }
  .detail-skeleton,
  .banner-skeleton {
    flex-direction: column;
    align-items: stretch;
  }
  .detail-image,
  .banner-image {
    width: 100%;
    max-width: none;
  }
}
</style>
