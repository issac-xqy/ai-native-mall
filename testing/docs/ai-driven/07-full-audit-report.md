# AI-Mall 全项目深度审计报告

> 日期：2026-06-10 | 范围：前端 22 页面 + 后端 20 控制器 + 测试框架

---

## 一、本次测试发现的 Bug 回顾

| # | 问题 | 根因 | 修复 |
|---|------|------|------|
| 1 | 登录无效（token=undefined） | `Login.vue` 取 `data.token`，API 返回 `data.data.token` | `Login.vue` + `user.ts` |
| 2 | 重复分类 145 条 | DB 5 个分类循环重复 29 次 | DB 清理 + 前端去重 |
| 3 | AI 轮播 403 | SecurityConfig 未放行 `/api/ai/recommend/**` | `SecurityConfig.java` |
| 4 | 订单列表空白 | API 嵌套 `data.data.data`，前端取 `data.data` | 多个 Vue 文件 |
| 5 | Wallet 403 | localStorage token 存成 "undefined" 字符串 | `user.ts` + `request.ts` |
| 6 | Admin 无登录入口 | admin 没有登录页面 | 新增 `AdminLogin.vue` |
| 7 | admin el-select 崩溃 | v-model 初始 undefined + clearable | `ProductList.vue` |
| 8 | admin el-table 空 | API 返回嵌套 `data.data.data` | `ProductList.vue` |
| 9 | el-rate 崩溃 | v-model 绑定原始类型，只读模式用 :model-value | `ProductDetail.vue` |
| 10 | 3255 条重复商品 | 每商品重复 31 次 | DB 去重 |

---

## 二、前端架构问题

### 2.1 🔴 严重：Login.vue 逻辑与 Store 重复
`Login.vue` 的 `handleLogin` 直接调 `fetch('/api/user/login')` 并手动操作 localStorage，完全绕过了 `userStore.login()`。
**修复**：删除 Login.vue 中的 fetch 调用，统一走 `userStore.login()`。

### 2.2 🔴 严重：缺少路由导航守卫
5 个路由（Checkout/Payment/Orders/Profile/Wallet）声明了 `meta: { requiresAuth: true }`，但 `router/index.ts` 没有 `beforeEach` 守卫。
**修复**：添加 `router.beforeEach((to, from, next) => { ... })`。

### 2.3 🔴 严重：多个页面直接用 fetch 绕过 request.ts
`Home.vue`（127, 140, 147行）、`AIChat.vue`（91行）、`Login.vue`（219行）都用 `fetch()` 而非 `request.ts`。丢掉了重试、超时、401 自动跳转。
**修复**：同步请求全部改 `get()`/`post()`/`put()`，SSE 流式保留 fetch 但复用 request.ts 的 header 逻辑。

### 2.4 🟡 中等：工具函数 4 处重复
`getFullImageUrl`、`formatSales`、`getRating`、`formatDate`、`getStatusText` 分散在 5+ 个文件中。
**修复**：创建 `src/utils/format.ts` 统一导出。

### 2.5 🟡 中等：大量 `any` 类型
`ref<any>()`、`product: any`、`[key: string]: any` 遍布各处，失去 TypeScript 保护。
**修复**：每个 API 响应定义对应 interface，`request.ts` 泛型默认改为 `unknown`。

### 2.6 🟡 中等：`request.ts` 401 处理用硬跳转
`window.location.href = '/login'` 而非 `router.push('/login')`，导致页面完全刷新。
**修复**：改用 Vue Router 导航。

### 2.7 🟢 低：不统一的空图片占位符
3 种不同的"无图" fallback（base64 SVG、via.placeholder.com、纯色块）。

---

## 三、后端架构问题

### 3.1 🔴 严重：返回值类型不统一
- 10 个控制器用 `Result<T>` 封装
- 10 个控制器用 `ResponseEntity<Map<String, Object>>` 手写 map
- `AiController` 两种混用
**修复**：全部统一为 `Result<T>`，`GlobalExceptionHandler` 统一处理异常转 Result。

### 3.2 🔴 严重：所有异常返回 HTTP 200
`FileUploadController`、`StatisticsController`、`UserAddressController` 等捕获异常后返回 `ResponseEntity.ok(success: false)`，HTTP 层分不清成功和失败。
**修复**：异常统一抛给 `GlobalExceptionHandler`，返回正确 HTTP 状态码。

### 3.3 🔴 严重：N+1 查询
`OrderServiceImpl.listOrders`（186行）对每个订单单独查询 order_items。50 个订单 = 51 次查询。
**修复**：批量查询 `selectByOrderIds(List<Long>)`。

### 3.4 🔴 严重：SQL 注入风险
`CommentController`（38-46行）用 `+ inClause +` 拼接 SQL。虽然是内部 ID，但违反安全最佳实践。
**修复**：用 `NamedParameterJdbcTemplate`。

### 3.5 🟡 中等：订单状态魔法数字
0/1/2/3/4 散落在 `OrderServiceImpl` 各处，无枚举。
**修复**：创建 `OrderStatus` 枚举。

### 3.6 🟡 中等：事务边界脆弱
`payOrder` 中扣款成功但订单状态更新失败时，通过另起一个 `refund()` 调用补救，而不是依赖 TX 回滚。
**修复**：扣款和更新放在同一事务中，用异常触发回滚。

### 3.7 🟡 中等：缺少输入校验
`AiController.askQuestion()`、`OrderController.createOrder()`、`CartController.addToCart()` 无 `@Valid` 和 null 保护。
**修复**：加 `@Valid`、`@NotNull`、DTO 替代 `Map<String, Object>`。

### 3.8 🟢 低：25+ 处重复 try/catch 模式
多个控制器里 `try { ... return ok(...) } catch { return ok(false) }` 重复。
**修复**：统一交给 `GlobalExceptionHandler` 处理。

---

## 四、测试框架优化

### 4.1 🔴 严重的验证深度不足
32 个 Playwright 用例的验证点停留在 "URL变了/弹了绿色消息"，从未验证数据是否正确写入 localStorage、页面渲染的数值是否匹配 API 返回。

### 4.2 改进方案

**每个测试用例应同时验证 4 层：**

```ts
// 1. Network: 响应结构
const resp = await loginResp.json();
expect(resp.data.token).toMatch(/^Bearer /);

// 2. Storage: 数据正确持久化
const token = await page.evaluate(() => localStorage.getItem('token'));
expect(token).not.toBe('undefined');

// 3. Console: 无运行时错误
expect(consoleErrors.length).toBe(0);

// 4. Render: UI 渲染正确数据
await expect(page.locator('.balance-amount')).not.toHaveText('¥0.00');
```

### 4.3 需要改写的测试

| 文件 | 用例数 | 深度不足的 | 需改写 |
|---|:---:|:---:|:---:|
| 01-auth.spec.ts | 5 | 4 | 登录验证 token、注册验证 API、未登录验证 redirect 逻辑 |
| 02-product.spec.ts | 4 | 3 | 列表验证渲染数量、搜索验证 API 参数 |
| 03-checkout.spec.ts | 4 | 4 | 下单验证订单号、支付验证余额变化 |
| 04-ai-chat.spec.ts | 4 | 2 | 流式验证内容非空 |
| 05-admin.spec.ts | 8 | 7 | 表格验证行数、搜索验证参数 |
| 06-negative.spec.ts | 6 | 2 | 错误场景验证状态码和消息 |

### 4.4 测试基础设施缺失
- 无 API Mock（测试依赖真实后端）
- 无测试数据隔离（测试互相污染 userId=2 的数据）
- 无 visual regression（截图对比不自动化）
- 无 accessibility 检查

---

## 五、UI/UX 优化建议

### 5.1 响应式布局
全部页面用固定像素，无 media query。手机端完全不可用。
**建议**：至少加 `768px` 和 `480px` 两个断点。

### 5.2 加载体验
- 首页轮播区空白时间过长（AI 推荐 API 响应慢）
- 无全局 loading bar
- 骨架屏只在 Products 页面有，其他页面缺
**建议**：`request.ts` 拦截器 + NProgress 全局 loading bar

### 5.3 导航体验
- 没有面包屑导航
- 搜索只在导航栏，搜索结果页筛选能力弱
- "加入购物车"成功后无动画反馈
**建议**：面包屑 + 搜索结果页增强 + 购物车图标微动效

### 5.4 商品展示
- 商品卡片信息密度低（价格+名称+销量，三者重复出现在列表和排行中）
- 排行榜与热门商品区完全相同的数据来源，视觉上像重复内容
**建议**：排行榜改为差异化维度（收藏数、好评率、新品上架）

### 5.5 Admin 后台
- 无 Dark mode
- 商品管理表格列太多（从图片到操作共 8-9 列），信息过载
- 统计页面只有文字数字，缺图表（echarts 已安装但只用了一个 trend chart）

---

## 六、代码解耦建议

### 6.1 抽离共用模块

| 新建文件 | 从哪些文件迁入 |
|---|------|
| `web-client/src/utils/format.ts` | `getFullImageUrl`、`formatSales`、`getRating`、`formatDate`、`maskPhone`、`maskEmail` |
| `web-client/src/composables/useConfirm.ts` | 10+ 处 `ElMessageBox.confirm` 重复模式 |
| `web-client/src/composables/usePagination.ts` | Products/Wallet/Orders 的分页逻辑 |
| `web-client/src/types/api.ts` | 所有 API 响应 interface |
| `backend: OrderStatus.java` enum | `OrderServiceImpl` 中的 0/1/2/3/4 |
| `backend: BaseController.java` | `try/catch` 模板 |

### 6.2 前端 Store 统一
所有页面通过 Store 操作数据，不直接调 API：
```
Login.vue → userStore.login() ✅ (修后)
Products.vue → productStore (新建)
Cart.vue → cartStore ✅
Wallet.vue → walletStore (新建)
```

---

## 七、优先级执行路线

### 第一批（今天修复）
1. 路由守卫 `beforeEach`
2. Login.vue 走 Store
3. `request.ts` 401 改用 router
4. 抽出 `format.ts` + `types/api.ts`

### 第二批（本周修复）
5. 后端统一 `Result<T>` 返回值
6. 订单状态枚举
7. N+1 查询优化
8. 输入校验 @Valid

### 第三批（优化提升）
9. 响应式 CSS
10. Admin Dark mode
11. NProgress 全局 loading
12. Playwright 深度验证改写
