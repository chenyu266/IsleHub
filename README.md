# IsleHub

IsleHub 是一个全栈电商系统，包含管理后台与 C 端商城两个前端应用，后端基于 Spring Boot 多模块架构。

## 技术栈

| 层面       | 选型                                |
| ---------- | ----------------------------------- |
| 后端框架   | Spring Boot 3.2 + Maven 多模块      |
| ORM        | MyBatis-Plus 3.5                    |
| 权限认证   | Sa-Token 1.37（JWT + Session 双模式） |
| 数据库     | MySQL 8.0                           |
| 缓存       | Redis（库存缓存 + 验证码）           |
| 消息队列   | RabbitMQ（订单库存确认、可靠消息）   |
| 邮件       | Spring Mail（邮箱验证码）            |
| API 文档   | Knife4j / SpringDoc OpenAPI         |
| 管理前端   | Vue 3 + Element Plus + Vite + ECharts |
| 商城前端   | Vue 3 + Element Plus + Vite         |
| Java       | JDK 17                              |

## 项目结构

```
IsleHub/
├── islehub-common/        # 公共模块（统一响应、异常处理、枚举、MQ 事件体、Redis 工具）
├── islehub-user/          # 用户模块（注册/登录、邮箱验证、信息修改、换绑邮箱、管理员 CRUD）
├── islehub-product/       # 商品模块（商品/分类/SKU 管理、库存 Redis 缓存、MQ 库存消费者）
├── islehub-order/         # 订单模块（订单 CRUD、状态流转、发货、Excel 导出、MQ 消息重试）
├── islehub-shop/          # 商城后端（购物车、收货地址、用户端订单，聚合各业务模块）
├── islehub-server/        # 启动入口（聚合所有模块，含全局配置与定时任务调度）
├── islehub-web/           # 管理后台前端
├── islehub-shop-web/      # C 端商城前端
└── pom.xml                # 根 POM
```

### 模块依赖

```
common ──┬── user ─────────────────────────────┐
         ├── product ── order ── shop ─────────┤
         └── (MQ 事件体、统一返回、Redis 工具)  │
                                               ├── server（启动入口）
                                               │
```

## 功能概览

### 管理后台 (`islehub-web`)

- **仪表盘** — ECharts 数据可视化工作台概览
- **用户管理** — 用户列表、新增/编辑/删除、启用/禁用
- **商品管理** — 商品列表、商品表单、SKU 规格与库存管理、分类树管理、批量上下架、图片上传
- **订单管理** — 订单列表、订单详情、状态流转、发货录入、取消订单、Excel 导出
- **权限控制** — admin / operator 角色隔离

### C 端商城 (`islehub-shop-web`)

- **用户中心** — 注册（邮箱验证码）、登录（邮箱/用户名）、修改用户名/密码/头像
- **换绑邮箱** — 两步验证：先验证旧邮箱 → 再确认新邮箱
- **商品浏览** — 商品搜索与分类浏览、商品详情与 SKU 选择
- **购物车** — 添加商品、修改数量、选中结算
- **收货地址** — 省市区三级联动（中国行政区划数据）、默认地址
- **订单** — 创建订单、订单列表与详情、确认收货、取消订单

### 后端核心机制

- **库存管理** — Redis 缓存 + Lua 脚本原子预扣/回滚，惰性加载 + TTL 兜底
- **可靠消息** — RabbitMQ Topic 交换机 + 死信队列 + 本地消息表 + 定时重试，保证订单创建后库存确认的最终一致性
- **邮箱验证** — 注册/换绑场景的邮箱验证码，自动匹配邮件服务器错误信息为中文提示
- **API 文档** — Knife4j / Swagger UI 自动生成接口文档，开发环境可访问

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis（库存缓存、验证码存储）
- RabbitMQ（订单库存确认消息）
- Node.js 18+（前端）

### 后端启动

1. 创建数据库：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS islehub DEFAULT CHARACTER SET utf8mb4;"
```

2. 配置环境变量，参考 `.env.example` 创建 `.env` 文件：

```bash
cp .env.example .env
```

3. 编辑 `.env`，填入数据库、Redis、RabbitMQ 及邮箱配置：

```env
# 数据库
DB_HOST=localhost
DB_PORT=3306
DB_NAME=islehub
DB_USERNAME=root
DB_PASSWORD=your_password_here

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBIT_HOST=localhost
RABBIT_PORT=5672
RABBIT_USERNAME=guest
RABBIT_PASSWORD=guest

# 邮箱（用于发送验证码）
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your_email@qq.com
MAIL_PASSWORD=your_auth_code
MAIL_FROM=your_email@qq.com
```

> **注意**：应用启动时会自动执行 `schema.sql`、`data.sql`、`china_region_data.sql` 初始化表结构和基础数据（dev 环境）。

4. 编译并启动：

```bash
# 编译
mvn clean install -pl islehub-server -am -DskipTests

# 启动
mvn spring-boot:run -pl islehub-server
```

应用默认运行在 `http://localhost:8080`，API 文档（Knife4j）访问 `http://localhost:8080/doc.html`。

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
mvn clean package -pl islehub-server -am -DskipTests

# 前端构建
cd islehub-web && npm run build
cd islehub-shop-web && npm run build
```

生产环境启动时设置 `spring.profiles.active=prod`，并确保所有环境变量已配置。

## 统一响应格式

所有接口返回统一 JSON 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页响应额外包含 `records`、`total`、`page`、`pageSize` 字段。

## 数据库表

| 表名            | 说明                     |
| --------------- | ------------------------ |
| `user`          | 用户表（邮箱 + 用户名登录） |
| `category`      | 商品分类（三级树形结构）   |
| `product`       | 商品主表                 |
| `product_sku`   | 商品 SKU（规格/价格/库存） |
| `order_table`   | 订单主表                 |
| `order_item`    | 订单明细                 |
| `order_shipping`| 物流信息                 |
| `order_message` | 可靠消息本地表（MQ 重试）  |
| `address`       | 用户收货地址             |
| `china_region`  | 中国行政区划数据          |

## 配置项说明

| 环境变量             | 说明             | 默认值                        |
| -------------------- | ---------------- | ----------------------------- |
| `DB_HOST`            | 数据库地址       | `localhost`                   |
| `DB_PORT`            | 数据库端口       | `3306`                        |
| `DB_NAME`            | 数据库名         | `islehub`                     |
| `DB_USERNAME`        | 数据库用户名     | 必填                          |
| `DB_PASSWORD`        | 数据库密码       | 必填                          |
| `REDIS_HOST`         | Redis 地址       | `localhost`                   |
| `REDIS_PORT`         | Redis 端口       | `6379`                        |
| `RABBIT_HOST`        | RabbitMQ 地址    | `localhost`                   |
| `RABBIT_PORT`        | RabbitMQ 端口    | `5672`                        |
| `RABBIT_USERNAME`    | RabbitMQ 用户名  | `guest`                       |
| `RABBIT_PASSWORD`    | RabbitMQ 密码    | `guest`                       |
| `MAIL_HOST`          | SMTP 服务器      | 必填（发送验证码）             |
| `MAIL_PORT`          | SMTP 端口        | 必填                          |
| `MAIL_USERNAME`      | 发件邮箱账号     | 必填                          |
| `MAIL_PASSWORD`      | 邮箱授权码       | 必填                          |
| `MAIL_FROM`          | 发件人地址       | 必填                          |
| `SA_TOKEN_JWT_SECRET`| JWT 签名密钥     | `dev-placeholder-...`（生产必改） |

## License

本项目仅用于学习与开源参考。
