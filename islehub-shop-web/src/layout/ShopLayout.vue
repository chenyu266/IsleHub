<template>
  <div class="shop-layout">
    <ShopHeader />
    <main class="shop-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import ShopHeader from '../components/ShopHeader.vue'
</script>

<style scoped>
.shop-layout {
  min-height: 100vh;
  background: transparent;
}

.shop-main {
  width: min(1240px, calc(100% - 48px));
  margin: 0 auto;
  padding: 24px 0 40px;
}
</style>
