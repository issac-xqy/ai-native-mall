# AI 驱动测试 — API 接口地图

> 用途：给 AI 提供后端接口的完整信息，让 AI 知道每个接口的路径、请求参数、预期响应，自动生成接口测试。
> 后端地址：`http://localhost:8081`
> 鉴权方式：JWT Token，Header 中携带 `Authorization: Bearer <token>`
> 获取 Token：`POST /api/user/login` → `{"username":"test_user","password":"admin123"}`

---

## 通用规则

### 响应格式

所有接口统一返回：
```json
{
  "success": true|false,
  "code": 200,
  "message": "操作成功",
  "data": {...},
  "timestamp": 1234567890
}
```

分页接口额外包含：
```json
{
  "data": {
    "records": [...],
    "total": 105,
    "pageNum": 1,
    "pageSize": 5,
    "pages": 21,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### 鉴权说明

| 接口分类 | 是否需要 Token | 示例 |
|---------|:---:|------|
| 公开接口 | ❌ | 登录、注册、商品列表、商品详情 |
| 用户接口 | ✅ | 购物车、订单、钱包、评论 |
| 管理接口 | ✅ (admin) | 商品管理、分类管理、统计 |

---

## 1. 用户模块 `/api/user`

| 方法 | 路径 | 鉴权 | 请求体 | 预期响应 |
|------|------|:---:|--------|----------|
| POST | `/api/user/login` | ❌ | `{"username":"","password":""}` | `{success:true, data:{token:"", userInfo:{id,nickname,phone,username,email}}}` |
| POST | `/api/user/register` | ❌ | `{"username":"","phone":"","email":"","password":""}` | `{success:true, message:"注册成功"}` |

### 验证点

| 场景 | 请求 | 预期 |
|------|------|------|
| 正确登录 | test_user/admin123 | success=true, 返回 token |
| 密码错误 | test_user/wrong | success=false, message 含"密码" |
| 用户不存在 | no_such_user/xxx | success=false |
| 注册成功 | 新用户名+手机+密码 | success=true, "注册成功" |
| 注册-用户名重复 | 已存在用户名 | success=false, message 含"已存在" |
| 注册-手机号格式错 | 手机号=123 | success=false, 校验失败 |

---

## 2. 商品模块 `/api/product`

| 方法 | 路径 | 鉴权 | 参数 | 预期响应 |
|------|------|:---:|------|----------|
| GET | `/api/product/list` | ❌ | `?pageNum=1&pageSize=12&categoryId=&keyword=&sortField=&sortOrder=` | `{success:true, data:{records:[], total:105}}` |
| GET | `/api/product/{id}` | ❌ | 路径参数 id | `{success:true, data:{id,name,price,stock,description,...}}` |

### 验证点

| 场景 | 请求 | 预期 |
|------|------|------|
| 获取商品列表 | pageNum=1&pageSize=5 | records 长度 ≤5, total=105 |
| 按分类筛选 | categoryId=1 | 只返回该分类商品 |
| 关键词搜索 | keyword=iPhone | 返回名称含 iPhone 的商品 |
| 排序-价格升序 | sortField=price&sortOrder=asc | 价格递增 |
| 商品详情 | /api/product/69 | 返回完整商品信息 |
| 商品不存在 | /api/product/99999 | success=false 或 data=null |

---

## 3. AI 智能客服 `/api/ai`

| 方法 | 路径 | 鉴权 | 请求体 | 预期响应 |
|------|------|:---:|--------|----------|
| POST | `/api/ai/customer-service/ask` | ❌ | `{"question":"","userId":"","sessionId":""}` | `{success:true, data:{answer:"...", sources:[...]}}` |
| GET | `/api/ai/customer-service/stream` | ❌ | `?question=&userId=&sessionId=` | SSE 流式 (`text/event-stream`) |
| POST | `/api/ai/product/seo-title` | ❌ | `{"productId":69}` | `{success:true, data:"SEO标题字符串"}` |
| POST | `/api/ai/product/description` | ❌ | `{"productId":69}` | `{success:true, data:"200-500字描述"}` |
| POST | `/api/ai/comment/analyze` | ❌ | `{"commentId":1}` | `{success:true, data:{sentiment:"positive", tags:[], summary:""}}` |
| POST | `/api/ai/product/reputation-report` | ❌ | `{"productId":69}` | `{success:true, data:{好评率, 高频词, 改进建议}}` |
| POST | `/api/ai/knowledge/import-products` | ❌ | `{"productIds":[69,70]}` | `{success:true, message:"已导入N条"}` |

### 验证点

| 场景 | 预期 |
|------|------|
| 商品咨询 | answer 包含商品信息 |
| 闲聊问题 | answer 友好拒绝 |
| 敏感词 | answer 包含拒绝提示 |
| SEO 标题生成 | 返回 ≤30 字标题 |
| 评论情感分析 | sentiment 为 positive/negative/neutral |
| 流式响应 | SSE 逐 token 推送 |

---

## 4. AI 推荐 `/api/ai/recommend`

| 方法 | 路径 | 鉴权 | 预期响应 |
|------|------|:---:|----------|
| GET | `/api/ai/recommend/carousel` | ❌ | `{success:true, data:[{id,name,image,reason},...]}` |

---

## 5. 购物车 `/api/cart` (需登录)

| 方法 | 路径 | 请求体 | 预期响应 |
|------|------|--------|----------|
| GET | `/api/cart` | - | `{success:true, data:[{id,productName,price,quantity},...]}` |
| POST | `/api/cart` | `{"productId":69,"quantity":1}` | `{success:true, message:"已加入购物车"}` |
| PUT | `/api/cart/{id}` | `{"quantity":3}` | `{success:true}` |
| DELETE | `/api/cart/{id}` | - | `{success:true}` |
| DELETE | `/api/cart/clear` | - | `{success:true}` |

---

## 6. 订单 `/api/order` (需登录)

| 方法 | 路径 | 请求体 | 预期响应 |
|------|------|--------|----------|
| POST | `/api/order` | `{"items":[{productId,quantity}], "address":{"name","phone","address"}}` | `{success:true, data:{orderNo, totalAmount}}` |
| GET | `/api/order/list` | `?pageNum=1&pageSize=10&status=` | `{success:true, data:{records:[], total}}` |
| PUT | `/api/order/{orderNo}/pay` | - | `{success:true}` (钱包扣款) |
| PUT | `/api/order/{orderNo}/cancel` | - | `{success:true}` (仅待支付可取消) |
| PUT | `/api/order/{orderNo}/confirm` | - | `{success:true}` (确认收货) |
| PUT | `/api/order/{orderNo}/ship` | - | `{success:true}` (admin) |
| PUT | `/api/order/{orderNo}/refund` | - | `{success:true}` (退款) |

---

## 7. 钱包 `/api/wallet` (需登录)

| 方法 | 路径 | 请求体 | 预期响应 |
|------|------|--------|----------|
| GET | `/api/wallet/info` | - | `{success:true, data:{balance, totalRecharge, totalSpent}}` |
| POST | `/api/wallet/recharge` | `{"amount":100, "type":1}` | `{success:true, data:{balance:新增后余额}}` |

### 验证点

| 场景 | 预期 |
|------|------|
| 充值 100 | balance 增加 100, totalRecharge 增加 100 |
| 充值金额 ≤0 | success=false |
| 下单支付 | balance 扣减订单金额 |
| 退款 | balance 增加退款金额 |

---

## 8. 评论 `/api/comment`

| 方法 | 路径 | 鉴权 | 请求体 | 预期响应 |
|------|------|:---:|--------|----------|
| GET | `/api/comment/product/{productId}` | ❌ | `?pageNum=1&pageSize=10` | `{success:true, data:[], total}` |
| POST | `/api/comment` | ✅ | `{"productId":69,"content":"","rating":5}` | `{success:true}` |
| DELETE | `/api/comment/{id}` | ✅ | - | `{success:true}` (仅自己的) |
| POST | `/api/comment/{id}/analyze` | ❌ | - | AI 分析该评论情感 |

---

## 9. 分类 `/api/category`

| 方法 | 路径 | 鉴权 | 预期响应 |
|------|------|:---:|----------|
| GET | `/api/category/list` | ❌ | `{success:true, data:[{id,name,parentId},...]}` |
| GET | `/api/category/tree` | ❌ | 树形结构 |
| GET | `/api/category/children/{parentId}` | ❌ | 子分类列表 |

---

## 10. 管理后台 `/api/admin/*` (需 admin 权限)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/product/list` | 商品管理列表 |
| POST | `/api/admin/product` | 新增商品 |
| PUT | `/api/admin/product/{id}` | 编辑商品 |
| DELETE | `/api/admin/product/{id}` | 删除商品 |
| PUT | `/api/admin/product/{id}/publish` | 上架 |
| PUT | `/api/admin/product/{id}/unpublish` | 下架 |
| PUT | `/api/admin/product/batch/publish` | 批量上架 |
| PUT | `/api/admin/product/batch/unpublish` | 批量下架 |
| GET | `/api/admin/category/list` | 分类管理 |
| POST | `/api/admin/category` | 新增分类 |
| PUT | `/api/admin/category/{id}` | 编辑分类 |
| DELETE | `/api/admin/category/{id}` | 删除分类 |
| GET | `/api/admin/order/list` | 订单管理 |
| GET | `/api/admin/statistics/overview` | 总览统计 |
| GET | `/api/admin/statistics/ai-monitor` | AI 调用监控 |
| GET | `/api/admin/statistics/top-sales` | 销量排行 |
| POST | `/api/admin/knowledge/upload` | 上传知识库文档 |
| GET | `/api/admin/knowledge/list` | 知识库列表 |
| DELETE | `/api/admin/knowledge/{id}` | 删除知识库文档 |

---

## 11. 健康检查 & 基础设施

| 方法 | 路径 | 预期响应 |
|------|------|----------|
| GET | `/actuator/health` | `{"status":"UP", "components":{db,redis,...}}` |
| GET | `/actuator/info` | 应用信息 |
| GET | `/` | 首页 |
