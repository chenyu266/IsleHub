<template>
  <div class="hero-center">
    <PageSkeleton v-if="loading" variant="banner" />
    <el-empty v-else-if="!list.length" :image-size="80" description="暂无推荐商品" />
    <el-carousel
      v-else
      class="recommend-carousel"
      height="320px"
      :interval="3000"
      arrow="hover"
      trigger="click"
      indicator-position="inside"
      pause-on-hover
    >
      <el-carousel-item v-for="item in list" :key="item.id">
        <div class="spotlight-slide" :style="slideStyle(item)">
          <div class="spotlight-copy">
            <p class="spotlight-eyebrow">{{ item.categoryName || '精选好物' }}</p>
            <h3>{{ item.name }}</h3>
            <p class="spotlight-price">¥{{ productMinPrice(item) }}</p>
            <button
              type="button"
              class="spotlight-action"
              @click.stop="$emit('go-product', item.id)"
            >
              查看详情 <span class="action-arrow">→</span>
            </button>
          </div>
          <div class="spotlight-media" @click.stop="$emit('go-product', item.id)">
            <el-image
              :src="item.bannerImage"
              :alt="item.name || '推荐商品'"
              fit="contain"
              class="spotlight-img"
            >
              <template #error>
                <span class="img-fallback">暂无图片</span>
              </template>
              <template #placeholder>
                <span class="img-fallback">加载中…</span>
              </template>
            </el-image>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import PageSkeleton from './PageSkeleton.vue'

const props = defineProps({
  list: { type: Array, default: () => [] },
  loading: { type: Boolean, default: true }
})

defineEmits(['go-product'])

/* ======================== 主色提取 ======================== */
const extractedColors = ref({})

watch(() => props.list, (list) => {
  extractedColors.value = {}
  if (list && list.length) {
    list.forEach(item => extractColor(item))
  }
}, { immediate: true })

function extractColor(item) {
  if (!item?.bannerImage) return
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    try {
      const canvas = document.createElement('canvas')
      const size = 40
      canvas.width = size; canvas.height = size
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, size, size)
      const data = ctx.getImageData(0, 0, size, size).data
      const map = {}
      let best = { r: 55, g: 138, b: 221, count: 0 }
      for (let i = 0; i < data.length; i += 12) {
        let r = data[i], g = data[i + 1], b = data[i + 2]
        const avg = (r + g + b) / 3
        if (avg > 240 || avg < 15) continue
        const diff = Math.max(r, g, b) - Math.min(r, g, b)
        if (diff < 15) continue
        r = Math.round(r / 16) * 16
        g = Math.round(g / 16) * 16
        b = Math.round(b / 16) * 16
        const key = `${r},${g},${b}`
        map[key] = (map[key] || 0) + 1
        if (map[key] > best.count) {
          best = { r, g, b, count: map[key] }
        }
      }
      extractedColors.value = { ...extractedColors.value, [item.id]: { r: best.r, g: best.g, b: best.b } }
    } catch { /* 跨域回退默认 */ }
  }
  img.onerror = () => {}
  img.src = item.bannerImage
}

function slideStyle(item) {
  const c = extractedColors.value[item?.id]
  if (!c) return {}
  const lr = c.r + Math.round((255 - c.r) * 0.7)
  const lg = c.g + Math.round((255 - c.g) * 0.7)
  const lb = c.b + Math.round((255 - c.b) * 0.7)
  return {
    background: `radial-gradient(circle at 76% 50%, rgba(${c.r},${c.g},${c.b},.12), transparent 38%), linear-gradient(105deg, rgba(${lr},${lg},${lb},.45) 0%, var(--shop-surface) 58%)`
  }
}

/* ======================== 价格 ======================== */
function productMinPrice(product) {
  const skus = product?.skus || []
  const prices = skus
    .map(sku => Number(sku.price))
    .filter(price => Number.isFinite(price) && price >= 0)
  if (!prices.length) return '--'
  return Math.min(...prices).toFixed(2)
}
</script>

<style scoped>
.hero-center { flex: 1; min-width: 0; }

/* ======================== 轮播容器 ======================== */
.recommend-carousel {
  border-radius: var(--shop-radius);
  overflow: hidden;
  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  box-shadow: var(--shop-shadow);
}

/* 强制每个轮播项高度，修复仅首张可见问题 */
.recommend-carousel :deep(.el-carousel__item) { height: 320px; }

/* indicator 蓝色系 */
.recommend-carousel :deep(.el-carousel__indicators--inside) { padding-bottom: 10px; }
.recommend-carousel :deep(.el-carousel__button) {
  width: 24px; height: 4px; border-radius: 999px;
  background: rgba(55, 138, 221, .25);
}
.recommend-carousel :deep(.el-carousel__button.is-active) {
  width: 28px; background: var(--shop-primary);
}

/* 箭头 hover 显示 */
.recommend-carousel :deep(.el-carousel__arrow) {
  opacity: 0;
  background: rgba(55,138,221,.45);
  transition: opacity .2s;
}
.recommend-carousel:hover :deep(.el-carousel__arrow) { opacity: 1; }
.recommend-carousel :deep(.el-carousel__arrow:hover) { background: var(--shop-primary); }

/* ======================== 幻灯片 ======================== */
.spotlight-slide {
  width: 100%; height: 100%;
  display: flex; align-items: center; gap: 0;
  padding: 36px 46px;
  transition: background .6s ease;
}

/* ---- 左侧文案 ---- */
.spotlight-copy {
  flex: 0 0 38%;
  display: flex; flex-direction: column; align-items: flex-start;
}
.spotlight-eyebrow {
  margin: 0; font-size: 12px; color: var(--shop-text-muted);
  letter-spacing: 1.5px; text-transform: uppercase;
}
.spotlight-copy h3 {
  margin: 10px 0 0; max-width: 280px;
  color: var(--shop-text);
  font-size: 24px; line-height: 1.35; font-weight: 600;
}
.spotlight-price {
  margin: 14px 0 18px;
  color: var(--shop-price);
  font-size: 22px; font-weight: 600;
}
.spotlight-action {
  display: inline-flex; align-items: center;
  height: 34px; padding: 0 16px;
  border: none; border-radius: 4px;
  background: var(--shop-primary);
  color: #fff; cursor: pointer;
  font-size: 13px; font-weight: 500;
  transition: background var(--shop-transition), transform var(--shop-transition);
}
.spotlight-action:hover {
  background: var(--shop-primary-hover);
  transform: translateX(3px);
}
.action-arrow {
  margin-left: 5px;
  transition: transform var(--shop-transition);
}
.spotlight-action:hover .action-arrow { transform: translateX(3px); }

/* ---- 右侧图片 ---- */
.spotlight-media {
  flex: 1; min-width: 0;
  display: flex; align-items: center; justify-content: center;
  height: 248px;                         /* 320 - 36*2 padding */
  padding: 12px;
  border: 1px solid var(--shop-border);
  border-radius: var(--shop-radius);
  background: var(--shop-surface)
    center / contain no-repeat;
  position: relative;
  box-shadow: var(--shop-shadow-sm);
  cursor: pointer;
  transition: border-color var(--shop-transition), box-shadow var(--shop-transition), transform var(--shop-transition);
}
.spotlight-media:hover {
  border-color: var(--shop-primary);
  box-shadow: var(--shop-shadow);
  transform: translateY(-2px);
}
.spotlight-img {
  width: 100%;
  height: 100%;
}
.spotlight-img :deep(.el-image__inner) {
  object-fit: contain;
}
.img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--shop-text-subtle);
  font-size: 13px;
}
</style>
