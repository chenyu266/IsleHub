<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <!-- 统计卡片：横向四张，顺序固定（商品总数→今日订单→待发货→用户数），禁止调换 -->
      <el-col v-for="item in stats" :key="item.key" :span="6">
        <el-card class="stat-card" shadow="never" :body-style="{ padding: '24px 20px', position: 'relative' }">
          <!-- 左上角图标：EP 内置线性图标 + 淡彩圆背景，绝对定位不占流、不遮挡数字 -->
          <div class="stat-icon" :class="item.iconClass">
            <el-icon :size="20"><component :is="item.icon" /></el-icon>
          </div>
          <!-- 数字：蓝色大号居中；加载中显示浅灰骨架占位（不增减文字） -->
          <div class="stat-value">
            <span v-if="!loading" class="stat-value__num">{{ formatNum(item.display) }}</span>
            <span v-else class="stat-skeleton" aria-hidden="true"></span>
          </div>
          <!-- 标题：灰色中文，内容固定 -->
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近7天订单趋势：折线面积图，卡片下方新增，不挤占统计卡片位置 -->
    <el-card class="chart-card" shadow="never" :body-style="{ padding: '20px' }">
      <div class="chart-header">
        <span class="chart-title">近7天订单趋势</span>
      </div>
      <div v-if="hasChartData" ref="chartRef" class="chart-container"></div>
      <div v-else class="chart-empty">
        <el-icon :size="36"><DataLine /></el-icon>
        <span class="chart-empty__text">暂无订单数据</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, markRaw, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Goods, ShoppingCart, Box, User, DataLine } from '@element-plus/icons-vue'
import { pageProducts } from '../api/product'
import { pageOrders } from '../api/order'
import { pageUsers } from '../api/user'

/* 卡片配置：顺序固定，禁止调换；icon 为 EP 线性图标（markRaw 避免响应式代理），iconClass 为淡彩圆背景 */
const stats = reactive([
  { key: 'productCount',     label: '商品总数', icon: markRaw(Goods),        iconClass: 'stat-icon--blue',   display: 0, target: 0 },
  { key: 'todayOrderCount',  label: '今日订单', icon: markRaw(ShoppingCart), iconClass: 'stat-icon--orange', display: 0, target: 0 },
  { key: 'pendingShipCount', label: '待发货',   icon: markRaw(Box),          iconClass: 'stat-icon--green',  display: 0, target: 0 },
  { key: 'userCount',        label: '用户数',   icon: markRaw(User),         iconClass: 'stat-icon--red',    display: 0, target: 0 }
])

const loading = ref(true)
let rafId = null

/* 数字格式化：千分位（zh-CN 语境），大数更易读 */
function formatNum(n) {
  return Math.round(n).toLocaleString('zh-CN')
}

/* 缓动函数：easeOutCubic，先快后慢，计数动画更自然 */
function easeOut(t) {
  return 1 - Math.pow(1 - t, 3)
}

/* 并发拉取四项统计；沿用原 allSettled 容错策略，单项失败归 0 */
async function fetchData() {
  const today = new Date().toISOString().slice(0, 10)
  const [r1, r2, r3, r4] = await Promise.allSettled([
    pageProducts({ page: 1, pageSize: 1 }, { silent: true }),
    pageOrders({ page: 1, pageSize: 1, startDate: today, endDate: today }, { silent: true }),
    pageOrders({ page: 1, pageSize: 1, status: 'paid' }, { silent: true }),
    pageUsers({ page: 1, pageSize: 1 }, { silent: true })
  ])
  /* 写入目标值（== 判等，遵循编码规范：禁用 ===） */
  stats[0].target = r1.status == 'fulfilled' ? r1.value.data.total : 0
  stats[1].target = r2.status == 'fulfilled' ? r2.value.data.total : 0
  stats[2].target = r3.status == 'fulfilled' ? r3.value.data.total : 0
  stats[3].target = r4.status == 'fulfilled' ? r4.value.data.total : 0
}

/* 数字滚动动画：四张卡片同步从 0 渐进到目标值，纯 requestAnimationFrame 实现 */
function animateAll(duration = 900) {
  const starts = stats.map(s => s.display)
  const startTime = performance.now()
  function step(now) {
    const t = Math.min((now - startTime) / duration, 1)
    const e = easeOut(t)
    stats.forEach((s, i) => {
      s.display = starts[i] + (s.target - starts[i]) * e
    })
    if (t < 1) {
      rafId = requestAnimationFrame(step)
    } else {
      rafId = null
    }
  }
  rafId = requestAnimationFrame(step)
}

/* ---------- 近7天订单趋势图（ECharts 折线面积图）---------- */
const chartRef = ref(null)
const hasChartData = ref(false)
let chartInstance = null

/* 拉取近7天每日订单数；7次并发，每次 pageSize:1 取 total，silent 静默容错 */
async function fetchChartData() {
  const dates = []
  const today = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    dates.push(d.toISOString().slice(0, 10))
  }
  const results = await Promise.allSettled(
    dates.map(date =>
      pageOrders({ page: 1, pageSize: 1, startDate: date, endDate: date }, { silent: true })
    )
  )
  /* 每日实际订单数（== 判等）；日期截取 MM-DD 便于横轴展示 */
  const actual = results.map(r => r.status == 'fulfilled' ? r.value.data.total : 0)
  const labels = dates.map(d => d.slice(5))
  return { labels, actual }
}

/* 渲染图表：蓝色实际订单（面积渐变），逐线绘制动画 */
function renderChart(labels, actual) {
  /* 空数据判断：7日全 0 则显示空状态占位 */
  const total = actual.reduce((a, b) => a + b, 0)
  hasChartData.value = total > 0
  if (!hasChartData.value) return

  /* 等 v-if 渲染出 DOM 后初始化图表 */
  nextTick(() => {
    if (!chartRef.value) return
    chartInstance = echarts.init(chartRef.value)
    chartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e4e7ed',
        textStyle: { color: '#303133' }
      },
      legend: {
        data: ['实际订单'],
        right: 10,
        top: 0,
        textStyle: { color: '#606266' }
      },
      grid: { left: '2%', right: '4%', top: '15%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: labels,
        axisLine: { lineStyle: { color: '#e4e7ed' } },
        axisLabel: { color: '#909399' }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        axisLabel: { color: '#909399' },
        splitLine: { lineStyle: { color: '#f0f2f5' } }
      },
      series: [
        {
          name: '实际订单',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 7,
          data: actual,
          itemStyle: { color: '#409EFF' },
          lineStyle: { color: '#409EFF', width: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.25)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
            ])
          },
          animationDuration: 1500,
          animationEasing: 'cubicOut'
        }
      ]
    })
  })
}

/* 窗口缩放时重绘图表 */
function handleResize() {
  if (chartInstance) chartInstance.resize()
}

onMounted(async () => {
  await fetchData()
  loading.value = false
  animateAll()
  /* 拉取并渲染7天订单趋势图 */
  const { labels, actual } = await fetchChartData()
  renderChart(labels, actual)
  window.addEventListener('resize', handleResize)
})

/* 卸载时取消动画帧 + 移除监听 + 释放图表实例，避免内存泄漏 */
onBeforeUnmount(() => {
  if (rafId != null) cancelAnimationFrame(rafId)
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
/* 仪表盘容器：轻微纵向留白，不改变横向四卡布局 */
.dashboard {
  padding: 4px 0;
}

/* 统计卡片：白底 + 浅灰阴影 + 圆角；悬停轻微上浮、阴影柔和加深，transition 顺滑过渡 */
.stat-card {
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

/* 左上角图标：40px 淡彩圆背景 + EP 线性图标，绝对定位于卡片体左上角，不遮挡数字 */
.stat-icon {
  position: absolute;
  top: 14px;
  left: 14px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}
/* 淡彩圆背景：EP 语义色低透明度，图标继承同色系 */
.stat-icon--blue   { background-color: rgba(64, 158, 255, 0.10);  color: #409EFF; }
.stat-icon--orange { background-color: rgba(230, 162, 60, 0.12);  color: #E6A23C; }
.stat-icon--green  { background-color: rgba(103, 194, 58, 0.12);  color: #67C23A; }
.stat-icon--red    { background-color: rgba(245, 108, 108, 0.12); color: #F56C6C; }

/* 数字：蓝色大号居中；等宽数字防止滚动动画时宽度抖动 */
.stat-value {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  font-size: 36px;
  font-weight: 700;
  color: #409EFF;
  line-height: 1.2;
}
.stat-value__num {
  font-variant-numeric: tabular-nums;
}

/* 加载骨架：浅灰脉冲占位（非文字，仅加载态） */
.stat-skeleton {
  display: inline-block;
  width: 64px;
  height: 30px;
  border-radius: 6px;
  background-color: #ebeef5;
  animation: stat-pulse 1.4s ease-in-out infinite;
}
@keyframes stat-pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.4; }
}

/* 标题：灰色中文，居中，间距与原版一致 */
.stat-label {
  margin-top: 10px;
  font-size: 14px;
  color: #909399;
}

/* ---------- 近7天订单趋势图：卡片下方新增，不挤占上方统计卡片位置 ---------- */
.chart-card {
  margin-top: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.chart-header {
  margin-bottom: 16px;
}
.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.chart-container {
  width: 100%;
  height: 320px;
}
/* 空状态占位：居中灰色图标+文字 */
.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 320px;
  color: #c0c4cc;
  gap: 12px;
}
.chart-empty__text {
  font-size: 14px;
}
</style>
