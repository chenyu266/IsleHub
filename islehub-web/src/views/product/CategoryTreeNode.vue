<template>
  <div class="cat-node">
    <div class="cat-row">
      <!-- 缩进区 + 层级连接线 -->
      <div class="cat-gutter" aria-hidden="true">
        <span
          v-for="lvl in depth"
          :key="lvl"
          class="cat-gutter__cell"
          :class="{ 'is-branch': lvl == depth, 'is-last': lvl == depth && isLast }"
        >
          <span class="cat-gutter__vline"></span>
          <span v-if="lvl == depth" class="cat-gutter__elbow"></span>
        </span>
      </div>

      <!-- 展开/折叠 -->
      <span class="cat-toggle" @click="toggleExpand">
        <el-icon v-if="hasChildren" size="14">
          <ArrowDown v-if="expanded" />
          <ArrowRight v-else />
        </el-icon>
        <span v-else class="cat-toggle__leaf"></span>
      </span>

      <!-- 分类名称 -->
      <span class="cat-name" :title="node.name">{{ node.name }}</span>

      <!-- 固定列：ID -->
      <span class="cat-col cat-col--id">{{ node.id }}</span>
      <!-- 固定列：排序 -->
      <span class="cat-col cat-col--sort">{{ node.sortOrder ?? '' }}</span>
      <!-- 固定列：操作 -->
      <span class="cat-col cat-col--actions">
        <el-button type="primary" size="small" @click="actions.add(node)">添加子级</el-button>
        <el-button size="small" @click="actions.edit(node)">编辑</el-button>
        <el-popconfirm
          title="删除分类会同时删除子分类，确定吗？"
          @confirm="actions.del(node.id)"
        >
          <template #reference>
            <el-button type="danger" size="small">删除</el-button>
          </template>
        </el-popconfirm>
      </span>
    </div>

    <!-- 子级：gap 留白，区分不同父级的子列表 -->
    <div v-show="expanded && hasChildren" class="cat-children">
      <CategoryTreeNode
        v-for="(child, i) in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :is-last="i == node.children.length - 1"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, inject } from 'vue'
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  isLast: { type: Boolean, default: false }
})

// 由父级 Category.vue 通过 provide 注入操作方法
const actions = inject('categoryActions')

const expanded = ref(true)
const hasChildren = computed(
  () => Array.isArray(props.node.children) && props.node.children.length > 0
)

function toggleExpand() {
  expanded.value = !expanded.value
}
</script>

<style scoped>
/* ---------- 单行 ---------- */
.cat-row {
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 6px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background-color 0.15s ease;
}
.cat-row:hover {
  background-color: var(--el-fill-color-light);
}

/* ---------- 缩进区 + 层级连接线 ---------- */
.cat-gutter {
  display: flex;
  align-items: stretch;
  align-self: stretch;   /* 让 gutter 撑满整行高度，竖线才能跨行连贯 */
  flex-shrink: 0;
}
.cat-gutter__cell {
  position: relative;
  width: 20px;            /* 固定 20px 阶梯缩进 */
  flex-shrink: 0;
}
/*
 * 竖向连接线：默认（祖先级）贯通整行，并用负 top 向上穿透 gap，
 * 与上一行的同列竖线首尾衔接，形成连贯层级线。
 */
.cat-gutter__vline {
  position: absolute;
  left: 50%;
  top: calc(-1 * var(--cat-gap, 10px));
  bottom: 0;
  width: 1px;
  background-color: var(--el-border-color);
  transform: translateX(-0.5px);
}
/*
 * 直接父级格（branch）：
 *  - 非末子 ├：竖线贯通整行（沿用默认 bottom:0）
 *  - 末子   └：竖线只到中点
 */
.cat-gutter__cell.is-branch.is-last .cat-gutter__vline {
  bottom: 50%;
}
/* 拐角横线：从中点竖线连到当前节点 */
.cat-gutter__elbow {
  position: absolute;
  left: 50%;
  right: 0;
  top: 50%;
  height: 1px;
  background-color: var(--el-border-color);
}

/* ---------- 展开/折叠 ---------- */
.cat-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  border-radius: 4px;
}
.cat-toggle:hover {
  background-color: var(--el-fill-color);
  color: var(--el-color-primary);
}
.cat-toggle__leaf {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: var(--el-text-color-placeholder);
}

/* ---------- 分类名称 ---------- */
.cat-name {
  flex: 1;
  min-width: 0;
  margin-right: 12px;
  font-size: 14px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 固定列 ---------- */
.cat-col {
  flex-shrink: 0;
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.cat-col--id {
  width: var(--cat-col-id, 64px);
}
.cat-col--sort {
  width: var(--cat-col-sort, 70px);
}
.cat-col--actions {
  width: var(--cat-col-actions, 240px);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
}

/* ---------- 子级容器：行间留白，区分不同父级的子列表 ---------- */
.cat-children {
  display: flex;
  flex-direction: column;
  gap: var(--cat-gap, 10px);  /* 父子分组行间留白 */
  overflow: visible;          /* 不裁切向上穿透的竖线 */
}
</style>
