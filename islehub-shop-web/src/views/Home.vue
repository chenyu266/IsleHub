<template>
  <div>
    <div class="category-tabs">
      <span :class="{ active: !activeCategory }" @click="activeCategory = null">全部</span>
      <span v-for="cat in categories" :key="cat.id"
            :class="{ active: activeCategory === cat.id }"
            @click="activeCategory = cat.id">{{ cat.name }}</span>
    </div>
    <div class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>
    <div class="empty-state" v-if="!products.length">
      <p>没有找到相关商品</p>
      <router-link to="/">返回首页</router-link>
    </div>
    <div class="pagination" v-if="total > pageSize">
      <el-pagination background layout="prev, pager, next"
        :total="total" :page-size="pageSize" v-model:current-page="pageNum"
        @current-change="fetchProducts" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageProducts, getCategoryTree } from '../api/product'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const products = ref([])
const categories = ref([])
const activeCategory = ref(null)
const pageNum = ref(1)
const total = ref(0)
const pageSize = 20

onMounted(async () => {
  try { categories.value = (await getCategoryTree()).data } catch { ElMessage.error('加载分类失败') }
  fetchProducts()
})

watch(activeCategory, () => { pageNum.value = 1; fetchProducts() })
watch(() => route.query.keyword, () => { pageNum.value = 1; fetchProducts() })

async function fetchProducts() {
  try {
    const params = { page: pageNum.value, pageSize }
    if (activeCategory.value) params.categoryId = activeCategory.value
    if (route.query.keyword) params.keyword = route.query.keyword
    const res = await pageProducts(params)
    products.value = res.data.records
    total.value = res.data.total
  } catch { ElMessage.error('加载商品失败') }
}
</script>

<style scoped>
.category-tabs { display: flex; gap: 16px; margin-bottom: 20px; padding: 0 4px; }
.category-tabs span { cursor: pointer; color: #666; font-size: 14px; padding: 4px 0; }
.category-tabs span.active { color: #409eff; font-weight: bold; border-bottom: 2px solid #409eff; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.empty-state {text-align: center;padding: 60px 0;color: #999;}
.empty-state p {font-size: 16px;margin-bottom: 12px;}
.empty-state a {color: #409eff;text-decoration: none;}
.pagination { display: flex; justify-content: center; margin-top: 30px; }
</style>
