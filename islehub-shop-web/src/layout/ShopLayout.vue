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
.shop-layout { min-height: 100vh; background: #f5f7fa; }
.shop-main { max-width: 1200px; margin: 0 auto; padding: 20px; }
</style>
