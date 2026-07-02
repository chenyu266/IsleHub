# IsleHub 管理后台

基于 Vue 3 + Vite + Element Plus 构建的电商管理后台系统。

## 技术栈

- **框架**: Vue 3 (Composition API + `<script setup>`)
- **构建工具**: Vite
- **UI 组件库**: Element Plus
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **图表**: ECharts

## 功能模块

- **仪表盘**: 数据统计卡片（商品总数、今日订单、待发货、用户数）与近 7 天订单趋势图
- **用户管理**: 用户 CRUD、启用/禁用、分页搜索
- **商品管理**: 商品 CRUD、分类树管理、SKU 规格管理、图片上传、批量上下架
- **订单管理**: 订单列表、订单详情、订单状态流转（待支付 → 已支付 → 已发货 → 已完成）、取消订单、发货信息录入、订单导出

## 本地开发

```bash
# 安装依赖
npm install

# 启动开发服务器 (端口 3000)
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

开发服务器默认运行在 `http://localhost:3000`，API 请求和上传请求会自动代理到 `http://localhost:8080`。

## 项目结构

```
src/
  main.js              # 应用入口
  App.vue              # 根组件
  style.css            # 全局样式
  router/
    index.js           # 路由配置与鉴权守卫
  layout/
    MainLayout.vue     # 主布局（顶栏导航 + 可收起侧边栏 + 内容区）
  api/
    request.js          # Axios 封装（Token 注入、统一错误处理、401 跳转）
    user.js             # 用户相关 API
    product.js          # 商品与分类 API
    order.js            # 订单与物流 API
  views/
    Login.vue           # 登录页
    Dashboard.vue       # 仪表盘
    user/
      UserList.vue      # 用户列表
    product/
      ProductList.vue   # 商品列表
      ProductForm.vue   # 商品新增/编辑
      Category.vue      # 分类树管理
      CategoryTreeNode.vue  # 分类树递归节点
    order/
      OrderList.vue     # 订单列表
      OrderDetail.vue   # 订单详情
```
