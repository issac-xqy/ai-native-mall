# AI 驱动测试 — 提示词模板

> 配合 `docs/testing/ai-ui-component-map.md` 使用。
> 把下面的提示词直接发给 Claude Code（已配好 Playwright MCP），AI 就会自动操控浏览器执行测试。

---

## 使用方式

```
1. 确保前端在跑: cd web-client && npm run dev  (localhost:3001)
2. 确保后端在跑: ./mvnw spring-boot:run       (localhost:8081)
3. 把组件地图 + 下面任意一条提示词发给 Claude Code
```

---

## 模板 1：登录功能测试

```
请根据 docs/testing/ai-ui-component-map.md 中的组件地图，执行以下 UI 测试：

1. 打开 http://localhost:3001/login
2. 先截图看看页面长什么样
3. 检查页面是否包含以下组件：
   - 用户名输入框 (placeholder="请输入用户名/手机号")
   - 密码输入框 (placeholder="请输入密码")
   - 登录按钮 (.submit-btn)
   - 注册账号链接
4. 用测试账号登录：用户名 test_user，密码 admin123
5. 点击登录按钮
6. 验证：页面是否跳转（不再停留在 /login）
7. 验证：是否出现成功提示 (.el-message--success)
8. 报告测试结果：通过/失败，如果失败说明原因
```

---

## 模板 2：注册新用户 + 验证数据

```
请根据 docs/testing/ai-ui-component-map.md 中的组件地图，执行以下测试：

1. 打开 http://localhost:3001/login
2. 点击 "注册账号" 切换到注册模式
3. 确认注册表单出现，包含：
   - 用户名输入框
   - 手机号输入框
   - 密码输入框
   - 确认密码输入框
4. 填入注册信息（用时间戳保证唯一）：
   - 用户名: aitester_YYYYMMDDHHmmss（用当前时间）
   - 手机号: 1380000XXXX（随机4位）
   - 密码: test123456
   - 确认密码: test123456
5. 点击注册按钮
6. 验证：出现 "注册成功，请登录" 的绿色提示
7. 验证：表单自动切换回登录模式
8. 用刚注册的账号登录，验证能否成功登录
9. 通过后端 API 验证数据库中确实新增了该用户：
   curl http://localhost:8081/api/user/login -H "Content-Type: application/json" -d '{"username":"aitester_xxx","password":"test123456"}'
10. 报告：每一步的执行结果
```

---

## 模板 3：商品浏览 + 搜索 + 详情

```
请根据组件地图，测试以下商品浏览流程：

1. 先登录：打开 /login，用 test_user/admin123 登录
2. 打开 http://localhost:3001/products
3. 确认页面加载：
   - 商品卡片 (.product-card) 是否可见
   - 分页器 (.el-pagination) 是否存在
   - 分类筛选按钮是否可见
4. 搜索测试：
   a. 在搜索框输入 "iPhone"
   b. 点击搜索按钮
   c. 等待列表刷新
   d. 验证第一个商品名称是否包含 "iPhone"（不区分大小写）
5. 点击第一个商品卡片，验证：
   a. URL 变为 /product/:id
   b. 商品标题可见
   c. 价格可见
   d. "加入购物车" 按钮可见
   e. 商品描述区域可见
6. 报告每一步的执行结果和截图
```

---

## 模板 4：完整购物链路（登录→浏览→加购→结算）

```
请根据组件地图，执行完整的购物链路测试：

1. 登录：打开 /login，用 test_user/admin123 登录
2. 浏览商品：打开 /products，等待商品列表加载
3. 点击第一个有库存的商品进入详情页
4. 在详情页点击 "加入购物车"
5. 验证出现成功提示
6. 打开购物车 /cart，验证：
   a. 购物车中有商品
   b. 商品名称、价格、数量显示正确
7. 选中商品，点击 "结算" 按钮
8. 验证跳转到 /checkout 页面
9. 在结算页填入收货信息：
   - 收货人: 测试用户
   - 手机号: 13800138000
   - 地址: 北京市朝阳区测试路100号
10. 查看余额是否足够：
    - 如果足够：点击提交订单
    - 如果不足：先点击 "去充值" 跳转到钱包页充值100元，再返回结算
11. 验证订单提交成功
12. 打开 /orders 验证新订单出现在列表中
13. 报告完整链路测试结果
```

---

## 模板 5：AI 智能客服测试

```
请根据组件地图，测试 AI 智能客服：

1. 打开 http://localhost:3001/ai-chat
2. 验证：
   a. 页面显示欢迎消息
   b. 输入框可见
   c. 发送按钮可见
3. 在输入框输入 "iPhone 15 Pro 有什么特点？"
4. 点击发送按钮
5. 验证：
   a. 用户消息出现在对话区
   b. 按钮变为 "AI 思考中..."（loading 状态）
   c. 等待 AI 回复出现
6. 检查 AI 回复内容是否相关（包含商品相关信息）
7. 再问一个问题："这个手机多少钱？"
8. 验证 AI 能记住上下文并回复
9. 点击 "清空对话" 按钮
10. 验证对话历史被清除
11. 报告测试结果
```

---

## 模板 6：负面场景测试

```
请根据组件地图，测试以下异常场景：

1.【空输入登录】
   - 打开 /login，什么都不填直接点登录
   - 验证：出现表单校验错误提示

2.【错误密码】
   - 输入 test_user，密码填 wrong_password
   - 点击登录
   - 验证：出现红色错误提示 (.el-message--error)

3.【未登录下单】
   - 清除所有 localStorage/cookies
   - 打开 /products，找到商品点击 "加入购物车"
   - 验证：弹出 "请先登录" 警告

4.【搜索无结果】
   - 打开 /products，搜索 "xyznonexistent999"
   - 验证：显示空状态 "未找到 xyznonexistent999 相关商品"

5.【AI 客服空输入】
   - 打开 /ai-chat，不输入内容直接点发送
   - 验证：弹出 "请输入问题" 警告

6. 汇总所有异常场景的测试结果
```

---

## 模板 7：管理后台测试

```
请根据组件地图，测试管理后台：

1. 打开 http://localhost:3001/admin/dashboard（或对应管理端地址）
2. 验证统计卡片是否显示：
   - 商品总数、已上架、未上架、总浏览量、总销量、总用户
3. 进入商品管理 /admin/products
4. 验证商品表格是否加载
5. 搜索一个存在的商品名称
6. 验证搜索结果正确
7. 测试批量操作：选中2个商品，点批量下架，再批量上架
8. 报告测试结果
```

---

## 快速验证测试（1分钟冒烟）

```
快速冒烟测试，只需回答 PASS/FAIL：

1. GET http://localhost:3001/login → 页面加载，有登录表单？ PASS/FAIL
2. 登录 test_user/admin123 → 跳转首页？ PASS/FAIL
3. GET http://localhost:3001/products → 有商品卡片？ PASS/FAIL
4. GET http://localhost:3001/ai-chat → 有欢迎消息？ PASS/FAIL
5. GET http://localhost:8081/actuator/health → {"status":"UP"}？ PASS/FAIL
```

---

## 自定义测试模板

```
请根据 docs/testing/ai-ui-component-map.md 中的组件地图，执行以下自定义测试：

[这里写你的测试场景，用自然语言描述即可]

要求：
- 每步操作后截图
- 遇到失败不继续，先报告
- 最后汇总：总步骤数 / 通过数 / 失败数 / 失败详情
```
