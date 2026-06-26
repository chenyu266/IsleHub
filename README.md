# IsleHub

IsleHub 是一个电商后端管理系统，包含管理后台与 C 端商城两个前端应用。

## 技术栈

| 层面     | 选型                              |
| -------- | --------------------------------- |
| 后端框架 | Spring Boot 3.2 + Maven 多模块    |
| ORM      | MyBatis-Plus 3.5                  |
| 权限认证 | Sa-Token 1.37                     |
| 数据库   | MySQL 8.0                         |
| 缓存     | Redis                             |
| 管理前端 | Vue 3 + Element Plus + Vite       |
| 商城前端 | Vue 3 + Element Plus + Vite       |
| Java     | JDK 17                            |

## 项目结构

```
IsleHub/
├── islehub-common/        # 公共模块（统一响应、异常处理、工具类）
├── islehub-user/          # 用户模块（登录认证、用户 CRUD）
├── islehub-product/       # 商品模块（商品/分类/SKU 管理）
├── islehub-order/         # 订单模块（订单/物流/Excel 导出）
├── islehub-shop/          # 商城后端（聚合上述模块，提供 C 端 API）
├── islehub-server/        # 启动入口（聚合所有业务模块）
├── islehub-web/           # 管理后台前端
├── islehub-shop-web/      # C 端商城前端
├── docs/                  # 设计文档
└── pom.xml                # 根 POM
```

### 模块依赖

```
common ──┬── user ─────────────────────────┐
         ├── product ── order ── shop ──┼── server
         └──────────────────────────────┘
```

## 功能概览

### 管理后台 (`islehub-web`)

- **仪表盘** — 工作台概览
- **用户管理** — 用户列表、新增/编辑/禁用
- **商品管理** — 商品列表、商品表单、分类树管理、批量上下架
- **订单管理** — 订单列表、订单详情、状态流转、发货录入、Excel 导出

### C 端商城 (`islehub-shop-web`)

- 商品浏览与搜索、商品详情与 SKU 选择
- 购物车、收货地址管理
- 订单创建、订单列表与详情

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis（仅 shop 模块需要）
- Node.js 18+

### 后端启动

1. 创建数据库并导入初始化脚本：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS islehub DEFAULT CHARACTER SET utf8mb4;"
```

2. 配置环境变量，参考 `.env.example` 创建 `.env` 文件：

```bash
cp .env.example .env
```

3. 编辑 `.env` 填入数据库和 Redis 连接信息。

4. 启动应用（根目录执行 Maven 会自动读取 `.env`）：

```bash
mvn clean install -pl islehub-server -am
```

5. 启动管理后台：

```bash
mvn spring-boot:run -pl islehub-server
```

应用默认运行在 `http://localhost:8080`。

### 前端启动

**管理后台：**

```bash
cd islehub-web
npm install
npm run dev        # 默认 http://localhost:5173
```

**C 端商城：**

```bash
cd islehub-shop-web
npm install
npm run dev        # 默认 http://localhost:5174
```

### 构建部署

```bash
# 后端打包
mvn clean package -pl islehub-server -am

# 前端构建
cd islehub-web && npm run build
cd islehub-shop-web && npm run build
```

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页响应额外包含 `records`、`total`、`page`、`pageSize` 字段。