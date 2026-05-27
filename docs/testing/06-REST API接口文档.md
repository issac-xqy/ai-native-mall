# REST API 接口文档

> 项目: AI-Native 智能商城 v1.0  
> 角色: 后端开发  
> 日期: 2026-05-26  
> 版本: v1.0  

---

## 1. 通用约定

### 1.1 基础信息

| 项目 | 值 |
|------|-----|
| Base URL | `http://localhost:8080` |
| 请求格式 | `application/json;charset=UTF-8` |
| 响应格式 | `application/json;charset=UTF-8` |
| 鉴权方式 | JWT Bearer Token (Header: `Authorization`) |
| 超时时间 | 120s (AI接口), 30s (其他) |

### 1.2 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1779800140766,
  "success": true
}
```

**分页响应:**
```json
{
  "code": 200,
  "success": true,
  "data": {
    "records": [],
    "total": 105,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 11,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### 1.3 状态码

| HTTP Code | ResultCode | 说明 |
|-----------|------------|------|
| 200 | SUCCESS(200) | 成功 |
| 400 | VALIDATION_ERROR(1001) | 参数校验失败 |
| 401 | UNAUTHORIZED(401) | 未登录或Token过期 |
| 403 | FORBIDDEN(403) | 无权限 |
| 404 | NOT_FOUND(404) | 资源不存在 |
| 500 | ERROR(500) | 系统异常 |
| 503 | AI_SERVICE_ERROR(2000) | AI服务异常 |

### 1.4 鉴权说明

```
需要鉴权的接口:
  用户端: /api/wallet/**, /api/order/**(部分), /api/address/**, /api/user/info(PUT)
  管理端: /api/admin/** (全部)

白名单 (无需Token):
  /api/user/login, /api/user/register
  /api/product/list, /api/product/{id}
  /api/category/**
  /api/search/**
  /api/ai/customer-service/**
```

---

## 2. 前台 API

### 2.1 根路径

#### `GET /`

服务健康检查。

**响应:**
```json
{
  "service": "AI-Native智能商城API",
  "version": "1.0",
  "time": "2026-05-26T20:18:05"
}
```

---

### 2.2 用户模块 `/api/user`

#### `POST /api/user/login` (无需Token)

**请求:**
```json
{ "username": "admin", "password": "admin123" }
```

**响应:**
```json
{
  "success": true,
  "message": "登录成功",
  "token": "Bearer MToxNzc5...",
  "userInfo": {
    "id": 1, "username": "admin", "phone": "13800138000",
    "nickname": "管理员", "email": "admin@example.com"
  }
}
```

**错误:**
```json
{ "success": false, "message": "用户名或密码错误" }
```

---

#### `POST /api/user/register` (无需Token)

**请求:**
```json
{
  "username": "newuser",
  "phone": "13800000000",
  "email": "new@test.com",
  "password": "password123"
}
```

**响应:**
```json
{ "success": true, "message": "注册成功" }
```

---

#### `GET /api/user/info` (需Token)

**请求头:** `Authorization: Bearer xxx`

**响应:**
```json
{
  "success": true,
  "data": {
    "id": 1, "username": "admin", "phone": "13800138000",
    "nickname": "管理员", "email": "admin@example.com"
  }
}
```

---

#### `PUT /api/user/info` (需Token)

**请求:**
```json
{ "nickname": "新昵称", "email": "new@email.com" }
```

---

### 2.3 商品模块 `/api/product`

#### `GET /api/product/list` (无需Token)

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:--:|--------|------|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |
| categoryId | long | 否 | - | 分类筛选 |
| keyword | string | 否 | - | 关键词搜索 |
| sortField | string | 否 | create_time | 排序字段: sales/price/create_time |
| sortOrder | string | 否 | desc | 排序方向: asc/desc |

**过滤条件 (后端自动):** publishStatus=1 AND stock>0 AND deleted=0

**响应:**
```json
{
  "code": 200, "success": true,
  "data": {
    "records": [
      {
        "id": 71, "name": "小米空气净化器4 Pro",
        "categoryId": 3, "price": 1299.00, "originalPrice": 1599.00,
        "stock": 100, "sales": 8900,
        "image": null, "images": null,
        "description": "小米空气净化器4 Pro，500m³/h CADR值，OLED触控屏",
        "specs": "{\"CADR\": \"500m³/h\", ...}",
        "seoTitle": null, "aiDescription": null,
        "sentimentScore": 5.00, "status": 1, "publishStatus": 1,
        "createTime": "2026-05-26T12:57:54"
      }
    ],
    "total": 105, "pageNum": 1, "pageSize": 10,
    "pages": 11, "hasNext": true, "hasPrevious": false
  }
}
```

---

#### `GET /api/product/{id}` (无需Token)

**响应:** 单个Product对象 (同上结构)

**404:** `{ "success": false, "message": "商品不存在" }`

---

#### `GET /api/product/top-sales` (无需Token)

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| limit | int | 10 | 返回数量 |

返回销量Top-N商品。

---

#### `GET /api/product/top-rated` (无需Token)

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| limit | int | 10 | 返回数量 |

返回好评Top-N商品（基于评论平均评分）。

---

#### `POST /api/product` (需管理Token)

创建商品。Body: Product对象JSON。

---

#### `PUT /api/product/{id}` (需管理Token)

更新商品。Body: 需要修改的字段。

---

#### `DELETE /api/product/{id}` (需管理Token)

逻辑删除商品 (deleted=1)。

---

#### `POST /api/product/{id}/seo-title` 

AI生成SEO标题。

---

#### `POST /api/product/{id}/ai-description`

AI生成商品描述文案。

---

#### `POST /api/product/{id}/analyze-comments`

AI批量分析商品评论情感。

---

#### `GET /api/product/analyze-by-name?productName=xxx`

按商品名称模糊匹配后分析评论情感。

---

### 2.4 分类模块 `/api/category`

#### `GET /api/category/tree` (无需Token)

返回分类树结构。

**响应:**
```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "手机数码", "parentId": 0, "children": [...] },
    { "id": 2, "name": "电脑办公", "parentId": 0, "children": [...] }
  ]
}
```

---

#### `GET /api/category/children/{parentId}` (无需Token)

获取指定父分类下的子分类。

---

#### `GET /api/category/list` (无需Token)

所有分类列表(平铺)。

---

### 2.5 订单模块 `/api/order`

#### `POST /api/order` (需Token)

创建订单。

**请求:**
```json
{
  "items": [{ "productId": 1, "quantity": 2 }],
  "addressId": 1,
  "remark": "请尽快发货"
}
```

---

#### `GET /api/order/list` (需Token)

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| status | int | - | 筛选: 0-4 |
| pageNum | int | 1 | |
| pageSize | int | 10 | |

**响应:** 分页订单列表, 含订单商品详情。

---

#### `PUT /api/order/{orderNo}/pay` (需Token)

钱包支付订单。扣减余额, 更新订单状态→已支付(1)。

**响应:**
```json
{ "success": true, "message": "支付成功" }
```

---

#### `PUT /api/order/{orderNo}/confirm` (需Token)

确认收货。状态: 已发货(2)→已完成(3)。

---

#### `PUT /api/order/{orderNo}/ship` (需管理Token)

管理员发货。状态: 已支付(1)→已发货(2)。

---

#### `PUT /api/order/{orderNo}/refund` (需Token)

申请退款。退款到钱包余额, 状态→已取消(4)。

---

#### `PUT /api/order/{orderNo}/cancel` (需Token)

取消订单。待支付→已取消, 恢复库存。

---

### 2.6 钱包模块 `/api/wallet`

#### `GET /api/wallet/info` (需Token)

**响应:**
```json
{
  "success": true,
  "data": {
    "id": 1, "userId": 1,
    "balance": 5000.00, "totalRecharge": 5000.00,
    "totalSpent": 0.00, "frozenAmount": 0.00
  }
}
```

---

#### `GET /api/wallet/balance` (需Token)

```json
{ "success": true, "data": { "balance": 5000.00 } }
```

---

#### `POST /api/wallet/recharge` (需Token)

**请求:**
```json
{ "amount": 1000.00, "rechargeType": 1 }
```

**响应:**
```json
{
  "success": true,
  "message": "充值成功",
  "data": { "balance": 6000.00, "tradeNo": "R20260526..." }
}
```

---

#### `GET /api/wallet/recharge-records` (需Token)

| 参数 | 类型 | 默认 |
|------|------|------|
| pageNum | int | 1 |
| pageSize | int | 10 |

返回充值记录分页列表。

---

#### `GET /api/wallet/spending-records` (需Token)

返回消费记录分页列表。

---

### 2.7 AI 模块 `/api/ai`

#### `POST /api/ai/customer-service/ask` (无需Token)

同步问答。

**请求:**
```json
{ "question": "iPhone 15 多少钱", "sessionId": "uuid-xxx" }
```

**响应:**
```json
{ "success": true, "data": { "answer": "iPhone 15的价格是5999元起..." } }
```

---

#### `GET /api/ai/customer-service/stream?question=xxx&sessionId=xxx` (无需Token)

SSE 流式问答。

**响应:** `text/event-stream`
```
data: {"content":"iPhone","done":false}
data: {"content":" 15","done":false}
...
data: {"content":"","done":true}
```

---

#### `POST /api/ai/product/seo-title`

**请求:**
```json
{ "productName": "小米14 Pro", "category": "手机数码", "features": "骁龙8Gen3,徕卡" }
```

---

#### `POST /api/ai/product/description`

**请求:**
```json
{
  "productName": "小米14 Pro",
  "category": "手机数码",
  "specs": "骁龙8Gen3/12GB/256GB/徕卡三摄",
  "features": "性能旗舰/徕卡影像/120W快充"
}
```

---

#### `POST /api/ai/comment/analyze`

**请求:**
```json
{ "comment": "手机很好用，拍照效果很棒，物流也很快" }
```

**响应:**
```json
{
  "success": true,
  "data": {
    "sentiment": "positive",
    "tags": "拍照,物流,体验",
    "summary": "用户对拍照和物流速度满意"
  }
}
```

---

#### `POST /api/ai/knowledge/import-products` (需管理Token)

批量将已上架商品导入向量知识库。

---

#### `POST /api/ai/product/reputation-report`

**请求:**
```json
{ "productName": "小米14 Pro" }
```

**响应:** JSON格式口碑报告 (好评率/关键词云/改进建议)。

---

### 2.8 推荐模块 `/api/ai/recommend`

#### `GET /api/ai/recommend/carousel` (无需Token)

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| limit | int | 5 | 轮播数量 |

AI智能推荐轮播商品。

**响应:**
```json
{
  "success": true,
  "data": [
    {
      "productId": 1, "productName": "iPhone 15 Pro Max",
      "reason": "本月热销TOP1, 用户好评率98%",
      "confidence": 0.95,
      "image": "http://...", "price": 9999.00
    }
  ]
}
```

---

### 2.9 搜索模块 `/api/search`

#### `GET /api/search/semantic?keyword=xxx&limit=10` (无需Token)

向量语义搜索。

---

#### `GET /api/search/hybrid?keyword=xxx&limit=10` (无需Token)

混合搜索 (向量+全文)。

---

#### `POST /api/search/rebuild-index` (需管理Token)

全量重建向量索引。

---

### 2.10 文件上传 `/api/upload`

#### `POST /api/upload/image`

**Content-Type:** `multipart/form-data`

| 参数 | 类型 | 说明 |
|------|------|------|
| file | File | 图片文件 (≤10MB) |

**响应:**
```json
{ "success": true, "data": { "url": "/uploads/2026/05/26/xxx.jpg" } }
```

---

#### `POST /api/upload/images`

批量上传。参数: `files` (多个File)。

---

### 2.11 地址模块 `/api/address`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/address/list` | 用户地址列表 |
| POST | `/api/address` | 添加地址 |
| PUT | `/api/address/{id}` | 更新地址 |
| PUT | `/api/address/{id}/default` | 设为默认 |
| DELETE | `/api/address/{id}` | 删除地址 |

---

### 2.12 评论模块 `/api/comment`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/comment/product/{productId}` | 分页查询商品评论 |
| POST | `/api/comment` | 添加评论 |
| POST | `/api/comment/{id}/analyze` | AI分析单条评论 |
| DELETE | `/api/comment/{id}` | 删除评论 |

---

### 2.13 统计模块 `/api/statistics`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/statistics/dashboard` | Dashboard汇总统计 |
| GET | `/api/statistics/sales-trend` | 近7天销售趋势 |
| GET | `/api/statistics/product-ranking` | 商品销量排行Top10 |
| GET | `/api/statistics/category-stats` | 分类商品统计 |

---

## 3. 后台管理 API `/api/admin/*`

> 全部需要管理员Token

### 3.1 商品管理 `/api/admin/product`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 后台商品列表(含草稿/下架) |
| GET | `/{id}` | 商品详情 |
| POST | `` | 创建商品 |
| PUT | `/{id}` | 更新商品 |
| DELETE | `/{id}` | 删除商品 |
| PUT | `/{id}/publish` | 上架 |
| PUT | `/{id}/unpublish` | 下架 |
| PUT | `/batch/publish` | 批量上架 |
| PUT | `/batch/unpublish` | 批量下架 |

### 3.2 分类管理 `/api/admin/category`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 所有分类(含禁用) |
| GET | `/tree` | 分类树 |
| POST | `` | 创建分类 |
| PUT | `/{id}` | 更新分类 |
| DELETE | `/{id}` | 删除分类 |
| DELETE | `/batch` | 批量删除 |
| PUT | `/{id}/status` | 更新状态 |
| PUT | `/{id}/sort` | 更新排序 |

### 3.3 订单管理 `/api/admin/order`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 全量订单列表(可按状态筛选) |

### 3.4 统计管理 `/api/admin/statistics`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/overview` | 总览: 商品数/订单数/用户数/销售额 |
| GET | `/top-views` | 浏览量Top10 |
| GET | `/top-clicks` | 点击量Top10 |
| GET | `/top-sales` | 销量Top10 |
| GET | `/publish-status` | 发布状态统计 |
| POST | `/product/{id}/view` | 记录浏览 |
| POST | `/product/{id}/click` | 记录点击 |
| GET | `/ai-monitor` | AI调用监控数据 |

### 3.5 知识库管理 `/api/admin/knowledge`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 知识库文档列表 |
| GET | `/{id}` | 文档详情(含内容) |
| POST | `/upload` | 上传文档 (multipart) |
| DELETE | `/{id}` | 删除文档+向量 |
| POST | `/{id}/revectorize` | 重新向量化 |

---

## 4. 接口统计

| 模块 | 路由前缀 | 接口数 |
|------|----------|:----:|
| 根路径 | `/` | 1 |
| 用户 | `/api/user` | 4 |
| 商品 | `/api/product` | 11 |
| 分类 | `/api/category` | 3 |
| 订单 | `/api/order` | 7 |
| 钱包 | `/api/wallet` | 5 |
| AI | `/api/ai` | 7 |
| 推荐 | `/api/ai/recommend` | 1 |
| 搜索 | `/api/search` | 3 |
| 文件 | `/api/upload` | 2 |
| 评论 | `/api/comment` | 4 |
| 地址 | `/api/address` | 5 |
| 统计 | `/api/statistics` | 4 |
| 后台商品 | `/api/admin/product` | 9 |
| 后台分类 | `/api/admin/category` | 8 |
| 后台订单 | `/api/admin/order` | 1 |
| 后台统计 | `/api/admin/statistics` | 8 |
| 知识库 | `/api/admin/knowledge` | 5 |
| **合计** | | **~88** |

---

> 文档编制: 后端开发 | 审核: 前端开发+测试 | 版本: v1.0 | 日期: 2026-05-26
