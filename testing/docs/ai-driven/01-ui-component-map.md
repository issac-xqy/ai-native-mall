# AI 测试 — UI 组件地图

> 供 AI（Claude Code + Playwright）执行 UI 测试时参考
> 格式：页面 → 路径 → 组件选择器 → 操作 → 验证点

---

## web-client（用户端）13 页面

### 1. Login `/login`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 登录表单 | `.auth-form` | 可见 | |
| 用户名 | `input[placeholder="请输入用户名/手机号"]` | fill | |
| 密码 | `input[placeholder="请输入密码"]` | fill | |
| 登录按钮 | `.submit-btn` | click | url 不再含 /login |
| 注册链接 | `text=注册账号` | click | 表单切换为注册模式 |
| 注册用户名 | `input[placeholder="请输入用户名"]` | fill | |
| 注册手机号 | `input[placeholder="请输入手机号"]` | fill | |
| 注册邮箱 | `input[placeholder="请输入邮箱（选填）"]` | fill | |
| 注册密码 | `input[placeholder="请输入密码（6-20位）"]` | fill | |
| 确认密码 | `input[placeholder="请确认密码"]` | fill | |
| 注册按钮 | 同 `.submit-btn` | click | `.el-message--success` |
| 成功消息 | `.el-message--success` | | visible |
| 错误消息 | `.el-message--error` | | visible |
| 警告消息 | `.el-message--warning` | | visible |
| 表单校验错误 | `.el-form-item__error` | | visible |

### 2. Home `/`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 轮播推荐 | `.carousel-container` | | visible |
| 推荐商品卡片 | `.carousel-card` | click | 跳转详情 |
| 热门商品区 | `.section h2` text="热门商品" | | |
| 商品卡片 | `.product-card` / `.el-card` | click | visible |
| 加入购物车 | `button:has-text("加入购物车")` | click | `.el-message--success` |
| 销量排行 | `.ranking-card` text="销量排行榜" | | |
| 好评排行 | `.ranking-card` text="好评排行榜" | | |
| 换一批 | `button:has-text("换一批")` | click | 卡片刷新 |

### 3. Products `/products`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 分类筛选 | `.category-filter` el-radio-group | click | |
| 搜索输入 | `input[placeholder="搜索商品名称、描述"]` | fill | |
| 搜索按钮 | `button:has-text("搜索")` | click | 列表刷新 |
| 排序 | `.sort-group` radio | click | 列表重排 |
| 商品卡片 | `.product-card` | click | /product/:id |
| 分页器 | `.el-pagination` | | visible |
| 空状态 | `.el-empty` | | text "未找到" |

### 4. ProductDetail `/product/:id`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 商品图 | `.product-carousel` | | visible |
| 标题 | `h1.product-title` | | 非空 |
| 价格 | `.current-price` | | 非空 |
| 加入购物车 | `button:has-text("加入购物车")` | click | `.el-message--success` |
| 立即购买 | `button:has-text("立即购买")` | click | |
| 评价区 | `.reviews h3` text="用户评价" | | |
| 不存在的商品 | `.el-empty` text="商品不存在或已下架" | | |

### 5. Cart `/cart`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 全选 | el-checkbox | click | |
| 购物车项 | `.cart-item` | | visible |
| 数量 | el-input-number | fill | |
| 删除 | `button:has-text("删除")` | click | |
| 结算 | `button:has-text("结算")` | click | /checkout |
| 空购物车 | `.el-empty` text="购物车空空如也" | | |

### 6. Checkout `/checkout` *(需登录)*

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 收货人 | `input[placeholder="请输入收货人姓名"]` | fill | |
| 手机号 | `input[placeholder="请输入手机号"]` | fill | |
| 地址 | `textarea` | fill | |
| 提交订单 | `button:has-text("提交订单")` | click | success |

### 7. Orders `/orders` *(需登录)*

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 筛选标签 | `.order-tabs` el-radio-group | click | |
| 订单卡片 | `.order-card` | | visible |
| 去支付 | `button:has-text("去支付")` | click | |
| 取消 | `button:has-text("取消订单")` | click | |
| 确认收货 | `button:has-text("确认收货")` | click | |

### 8. Wallet `/wallet` *(需登录)*

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 余额 | `.balance-amount` | | 显示金额 |
| 充值金额 | el-input-number | fill | |
| 快捷金额 | `.quick-recharge button` | click | |
| 立即充值 | `button:has-text("立即充值")` | click | |
| 记录表 | `.el-table` | | rows |

### 9. AIChat `/ai-chat`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 欢迎消息 | `.message.ai .content` | | text "客服助手" |
| 输入框 | `textarea[placeholder*="请输入您的问题"]` | fill | |
| 发送 | `button:has-text("发送")` | click | |
| 思考中 | `button:has-text("AI 思考中")` | | 消失 |
| 清空 | `button:has-text("清空对话")` | click | 重置 |
| 空输入警告 | `.el-message--warning` | | |

### 10. Profile `/profile` *(需登录)*

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 头像区 | `.user-header` el-avatar | | |
| 菜单 | el-menu items | click | 切换 tab |
| 保存 | `button:has-text("保存修改")` | click | |
| 新增地址 | `button:has-text("+ 新增地址")` | click | dialog |

### 11. Payment `/payment` *(需登录)*

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 金额 | `.amount` | | |
| 确认支付 | `button:has-text("确认支付")` | click | |
| 成功提示 | `.el-alert` text="支付成功" | | |

### 12. UploadDemo `/upload-demo`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| ImageUpload | el-card 内 | | visible |

### 13. NotFound `/*`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 404 | `.el-result` title="404" | | |
| 返回首页 | `button:has-text("返回首页")` | click | url="/" |

---

## web-admin（管理后台）9 页面

baseURL: `http://localhost:3000`

### 1. Dashboard `/`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 统计卡片 | `.stat-card` | | ≥3 个 |
| 商品总数 | `.stat-label` text="商品总数" | | 数字 |
| 发布状态 | el-progress | | |
| 浏览/销量 TOP | el-card header="浏览量 TOP 10" / "销量 TOP 10" | | `.top-item` |

### 2. ProductList `/products`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 搜索 | `input[placeholder="商品名称"]` | fill | |
| 搜索按钮 | `button:has-text("搜索")` | click | |
| 表格 | `.el-table` | | rows |
| 批量上架/下架 | `button:has-text("批量上架")` / "批量下架" | click | |
| 分页 | `.el-pagination` | | |
| 编辑/删除 | `button:has-text("编辑")` / "删除" | click | |

### 3. CategoryList `/categories`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 新增 | `button:has-text("新增分类")` | click | dialog |
| 表格 | `.el-table` | | rows |
| 状态开关 | `.el-switch` | click | |
| 编辑 | `button:has-text("编辑")` | click | dialog |

### 4. OrderList `/orders`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 标签 | `.el-tabs` | click tab | |
| 表格 | `.el-table` | | rows |
| 发货 | `button:has-text("发货")` | click | dialog |

### 5. ProductAI `/product-ai`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| SEO 商品名 | `input[placeholder="例如：iPhone 15 Pro"]` | fill | |
| 生成 SEO | `button:has-text("生成 SEO 标题")` | click | el-alert |
| 生成描述 | `button:has-text("生成商品描述")` | click | el-alert |

### 6. CommentAnalysis `/comment`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 商品名 | `input[placeholder="输入商品名称，自动分析评论"]` | fill | |
| 分析 | `button:has-text("智能分析")` | click | 结果 |
| 好评率 | `.el-statistic` | | |

### 7. AiMonitor `/ai-monitor`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 指标卡片 | `.metric-card` | | 数字 |
| 趋势图 | canvas/.chart | | |
| 刷新 | `button:has-text("刷新数据")` | click | |

### 8. KnowledgeBase `/knowledge`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 上传 | `button:has-text("上传文档")` | click | dialog |
| 分类筛选 | el-select placeholder="全部分类" | select | |
| 表格 | `.el-table` | | rows |

### 9. ProductForm `/product/create` `/product/edit/:id`

| 组件 | 选择器 | 操作 | 验证 |
|------|--------|------|------|
| 商品名称 | `input[placeholder="请输入商品名称"]` | fill | |
| 价格/库存 | el-input-number | fill | |
| 描述 | `textarea[placeholder="请输入商品描述"]` | fill | |
| 保存 | `button:has-text("保存修改")` | click | |
| 返回 | `button:has-text("返回列表")` | click | /products |

---

## 侧边栏导航 (admin Layout)

| 组件 | 选择器 | 操作 |
|------|--------|------|
| 商品管理 | `.el-menu-item:has-text("商品管理")` | click |
| 数据统计 | `.el-menu-item:has-text("数据统计")` | click |
| 分类管理 | `.el-menu-item:has-text("分类管理")` | click |
| AI 运营助手 | `.el-menu-item:has-text("智能运营助手")` | click |
| 评论分析 | `.el-menu-item:has-text("评论情感分析")` | click |
| AI 监控 | `.el-menu-item:has-text("运营监控看板")` | click |
| 知识库 | `.el-menu-item:has-text("知识库管理")` | click |
| 订单管理 | `.el-menu-item:has-text("订单管理")` | click |

---

## 通用 Element Plus 组件

| 组件 | 选择器 |
|------|--------|
| 成功消息 | `.el-message--success` |
| 错误消息 | `.el-message--error` |
| 警告消息 | `.el-message--warning` |
| 表格 | `.el-table` |
| 分页 | `.el-pagination` |
| 对话框 | `.el-dialog` |
| 空状态 | `.el-empty` |
| 表单校验错误 | `.el-form-item__error` |
