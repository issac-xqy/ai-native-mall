# 深度 E2E 测试计划

> 区别：普通 E2E 测"页面能加载"，深度 E2E 测"数据从后端到前端完整闭环"

## 核心原则

```
不只看 UI → 还要看 Network + localStorage + Console + 渲染数据
```

每个测试用例必须验证 4 层：

| 层 | 检查什么 | 工具 |
|:---:|------|------|
| Network | API 返回了什么？前端发了几次请求？有没有 403 静默失败？ | `page.waitForResponse()` |
| Storage | token 存对了吗？userInfo 是 undefined 吗？ | `page.evaluate(() => localStorage)` |
| Console | 有 error 吗？有 ElementPlus 警告吗？ | `page.on('console')` |
| Render | 页面上渲染的是真实数据还是"加载中"/空状态？ | `page.locator()` |

## Playwright 写法模板

```ts
test('登录 — 深度验证', async ({ page }) => {
  // 1. 拦截 network
  const loginReq = page.waitForResponse(r => r.url().includes('/api/user/login') && r.status() === 200);
  const loginResp = page.waitForResponse(r => r.url().includes('/api/user/login'));

  // 2. 操作
  await page.goto('/login');
  await page.fill('input[...]', 'test_user');
  await page.fill('input[...]', 'admin123');
  await page.click('.submit-btn');

  // 3. 验证 network
  const respData = await (await loginResp).json();
  expect(respData.success).toBe(true);
  expect(respData.data.token).toMatch(/^Bearer /);
  expect(respData.data.userInfo).toBeDefined();
  expect(respData.data.userInfo.username).toBe('test_user');

  // 4. 验证 localStorage
  const token = await page.evaluate(() => localStorage.getItem('token'));
  expect(token).toMatch(/^Bearer /);
  expect(token).not.toBe('undefined');

  const userInfo = await page.evaluate(() => {
    const raw = localStorage.getItem('userInfo');
    return raw && raw !== 'undefined' ? JSON.parse(raw) : null;
  });
  expect(userInfo).toBeDefined();
  expect(userInfo.username).toBe('test_user');

  // 5. 验证 UI 渲染
  await expect(page.locator('.user-dropdown, [class*="user"]').first()).toBeVisible();
  await expect(page.locator('button:has-text("登录")')).not.toBeVisible();

  // 6. 验证跳转
  await expect(page).not.toHaveURL(/\/login$/);

  // 7. 验证无 console error
  const errors = await collectConsoleErrors(page);
  expect(errors.filter(e => !e.includes('favicon') && !e.includes('google'))).toHaveLength(0);
});
```

## 覆盖清单

### 认证模块
- [ ] 登录：token 存对 + UI 切换 + 无 console error
- [ ] 登录失败：错误提示 + token 未存
- [ ] 注册：API 验证 + localStorage 不残留 token
- [ ] 退出：token/userInfo 清除 + UI 恢复登录按钮
- [ ] 未登录拦截：访问 /orders → 跳转 /login

### 商品模块
- [ ] 列表：API 返回 records → 页面渲染对应数量的 .product-card
- [ ] 列表：分类筛选 → API 请求带正确 categoryId → 列表更新
- [ ] 列表：搜索 → API 请求带 keyword → 结果匹配
- [ ] 详情：URL /product/:id → API 返回单条 → h1 显示商品名
- [ ] 详情：评论 API → .review-item 数量 = API 返回数量

### 购物车
- [ ] 加购：API POST → success → 再次 GET /api/cart 验证数据入库
- [ ] 数量修改：PUT → 再次验证
- [ ] 删除：DELETE → 验证
- [ ] 空购物车：显示 el-empty

### 订单
- [ ] 列表：API 返回 → 页面渲染 .order-card，数量匹配
- [ ] 筛选：状态筛选 → 请求带 status 参数
- [ ] 列表 loading → skeleton → 数据（防闪烁假阳性）

### 钱包
- [ ] 余额渲染：API 返回 balance → .balance-amount 显示对应数字（不显示 0.00 假阳性）
- [ ] 充值：POST → 余额变化 → 记录表新增行
- [ ] 记录分页：翻页 → API 请求带 pageNum

### AI 客服
- [ ] SSE 流式：发送消息 → loading 状态 → AI 回复出现 → 内容非空
- [ ] 清空：点击 → 只有欢迎消息
- [ ] 空输入：点击发送 → .el-message--warning

### 管理后台
- [ ] 登录：token 存 adminToken → 进入仪表盘
- [ ] 商品列表：API 返回 data.data.data → 表格渲染行数匹配
- [ ] 搜索：表单提交 → API 带参数
- [ ] 上架/下架：PUT → 状态更新
- [ ] 删除：DELETE → 行消失
- [ ] 统计卡片：数字匹配 API 返回

## 执行方式

```bash
# 启动前端
cd web-client && npm run dev

# AI 驱动执行（把这份文件给 Claude Code + Playwright MCP）
# Claude 会逐个验证上述清单
```
