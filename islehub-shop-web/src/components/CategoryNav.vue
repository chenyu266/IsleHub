<template>
  <aside class="hero-left">
    <div class="category-nav">
      <h4 class="nav-title">分类</h4>
      <nav class="cat-list">
        <a href="#" :class="{ active: !selectedId }"
           @click.prevent="$emit('select-cat', null)">
          <HomeFilled class="cat-icon" /><span>主页</span>
        </a>
        <PageSkeleton v-if="loading" variant="category-nav" :rows="8" />
        <template v-else v-for="cat in categories" :key="cat.id">
          <a v-if="!cat.children || !cat.children.length"
             href="#"
             :class="{ active: selectedId === cat.id }"
             @click.prevent="$emit('select-cat', cat.id)">
            <Folder class="cat-icon" /><span>{{ cat.name }}</span>
          </a>
          <div v-else class="cat-item" :class="{ active: isParentActive(cat) }">
            <a href="#" class="cat-row" @click.prevent="$emit('select-cat', cat.id)">
              <Folder class="cat-icon" />
              <span>{{ cat.name }}</span>
              <span class="arrow">▸</span>
            </a>
            <div class="cat-sub-panel">
              <div class="cat-panel-header">
                <a href="#"
                   class="cat-panel-root"
                   :class="{ active: isCategoryActive(cat) }"
                   @click.prevent="$emit('select-cat', cat.id)">
                  全部 {{ cat.name }}
                </a>
              </div>
              <div class="cat-groups">
                <section class="cat-group" v-for="sub in secondLevelCats(cat)" :key="sub.id">
                  <a href="#"
                     class="cat-group-title"
                     :class="{ active: isCategoryActive(sub) }"
                     @click.prevent="$emit('select-cat', sub.id)">
                    {{ sub.name }}
                  </a>
                  <div v-if="thirdLevelCats(sub).length" class="cat-leaf-list">
                    <a v-for="leaf in thirdLevelCats(sub)"
                       :key="leaf.id"
                       href="#"
                       class="cat-leaf"
                       :class="{ active: isCategoryActive(leaf) }"
                       @click.prevent="$emit('select-cat', leaf.id)">
                      {{ leaf.name }}
                    </a>
                  </div>
                </section>
              </div>
            </div>
          </div>
        </template>
      </nav>
    </div>
  </aside>
</template>

<script setup>
import { Folder, HomeFilled } from '@element-plus/icons-vue'
import PageSkeleton from './PageSkeleton.vue'

const props = defineProps({
  categories: { type: Array, default: () => [] },
  selectedId: { type: Number, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['select-cat'])

function secondLevelCats(cat) {
  return Array.isArray(cat?.children) ? cat.children : []
}

function thirdLevelCats(cat) {
  return Array.isArray(cat?.children) ? cat.children : []
}

function isCategoryActive(cat) {
  return Number(props.selectedId) === cat.id
}

function isParentActive(parent) {
  return containsCategory(parent, props.selectedId)
}

// 递归判断分类树中是否包含指定 id
function containsCategory(cat, id) {
  if (!id || !cat) return false
  if (Number(cat.id) === id) return true
  return (cat.children || []).some(child => containsCategory(child, id))
}
</script>

<style scoped>
.hero-left {
  width: 210px; height: 90%; flex-shrink: 0;
  overflow: visible;
  border-radius: 12px;
}
.category-nav {
  background: var(--shop-surface); border: 1px solid var(--shop-border); border-radius: var(--shop-radius); height: 100%;
  box-shadow: var(--shop-shadow-sm);
  padding: 14px 0;
  overflow: visible;
}
.nav-title { margin: 0 18px 10px; font-size: 15px; color: var(--shop-text); letter-spacing: 0.5px; font-weight: 800; }

.cat-list > a,
.cat-item > .cat-row {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 18px; font-size: 13px; color: var(--shop-text-muted);
  text-decoration: none; transition: all 0.15s;
  cursor: pointer; position: relative;
}
.cat-list > a:hover,
.cat-item > .cat-row:hover { background: var(--shop-primary-soft); color: var(--shop-primary); }
.cat-list > a.active { background: var(--shop-primary-soft); color: var(--shop-primary); font-weight: 700; border-left: 3px solid var(--shop-primary); padding-left: 15px; }
.cat-item.active > .cat-row { color: var(--shop-primary); font-weight: 700; }

.cat-icon { width: 16px; height: 16px; text-align: center; flex-shrink: 0; }
.arrow { margin-left: auto; font-size: 10px; color: var(--shop-text-subtle); transition: transform 0.2s; }
.cat-item:hover .arrow { transform: translateX(2px); color: var(--shop-primary); }

.cat-item { position: relative; }

.cat-sub-panel {
  position: absolute;
  top: 0;
  left: 100%;
  width: 420px;
  height: 100%;

  background: var(--shop-surface);
  border: 1px solid var(--shop-border);
  border-radius: 0 var(--shop-radius) var(--shop-radius) 0;
  box-shadow: var(--shop-shadow);
  padding: 20px 24px;

  display: flex;
  flex-direction: column;
  gap: 16px;

  z-index: 9999;

  opacity: 0;
  visibility: hidden;
  transform: translateX(-4px);
  transition: opacity 0.2s ease, visibility 0.2s ease, transform 0.2s ease;
}

.cat-item:hover .cat-sub-panel,
.cat-sub-panel:hover {
  opacity: 1;
  visibility: visible;
  transform: translateX(0);
}

.cat-panel-header { padding-bottom: 12px; border-bottom: 1px solid var(--shop-border); }
.cat-panel-root {
  display: inline-block;
  padding: 7px 16px;
  font-size: 13px;
  color: var(--shop-primary);
  text-decoration: none;
  border-radius: 20px;
  background: var(--shop-primary-soft);
  font-weight: 600;
  transition: all 0.15s;
}
.cat-panel-root:hover,
.cat-panel-root.active { background: var(--shop-primary); color: #fff; }
.cat-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 28px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}
.cat-group { min-width: 0; }
.cat-group-title {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  margin-bottom: 8px;
  color: var(--shop-text);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  text-decoration: none;
}
.cat-group-title::after {
  content: '›';
  margin-left: 6px;
  color: var(--shop-text-subtle);
  font-size: 15px;
}
.cat-group-title:hover,
.cat-group-title.active { color: var(--shop-primary); }
.cat-leaf-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
}
.cat-leaf {
  display: inline-block;
  max-width: 100%;
  padding: 5px 12px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--shop-text-muted);
  text-decoration: none;
  border-radius: 16px;
  white-space: nowrap;
  background: var(--shop-surface-muted);
  transition: all 0.15s;
}
.cat-leaf:hover { background: var(--shop-primary-soft); color: var(--shop-primary); }
.cat-leaf.active { background: var(--shop-primary); color: #fff; font-weight: 500; }
</style>
