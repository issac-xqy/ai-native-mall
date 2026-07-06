# AI 驱动测试体系

> AI-Native 智能商城 — 三层 AI 测试落地实施

---

## 三层架构

```
┌─────────────────────────────────────────────────┐
│  第一层: UI + 功能测试                            │
│  AI 看页面 → 找组件 → 点击操作 → 验证数据           │
│  工具: Playwright + 组件地图 + AI 脚本生成          │
├─────────────────────────────────────────────────┤
│  第二层: 接口 + 性能测试                           │
│  AI 调 API → 断言响应 → 压测 → 报告              │
│  工具: curl + API 地图 + JMeter MCP              │
├─────────────────────────────────────────────────┤
│  第三层: 运维监控                                  │
│  AI 读指标 → 扫日志 → 分析原因 → 发邮件             │
│  工具: ai-ops-monitor.sh + cron                 │
└─────────────────────────────────────────────────┘
```

---

## 文件索引

| 文件 | 内容 | 对应层级 |
|------|------|:---:|
| `01-ui-component-map.md` | 10 个页面的组件选择器、操作、验证点 | 第一层 |
| `02-ui-test-prompts.md` | 7 套 UI 测试提示词模板，复制即用 | 第一层 |
| `03-api-map.md` | 11 个模块的全部接口路径、参数、预期响应 | 第二层 |
| `04-api-test-prompts.md` | 5 套接口测试提示词模板（冒烟/链路/负面/压测） | 第二层 |
| `05-ops-monitor.md` | 运维监控助手说明，7 个内置 tool | 第三层 |

测试脚本：
| 文件 | 内容 |
|------|------|
| `testing/e2e/ai-smoke-test.spec.ts` | AI 生成的登录冒烟测试（Playwright） |
| `testing/scripts/ai-ops-monitor.sh` | 运维监控脚本（7 个 tool） |

---

## 快速开始

### 第一层：UI 测试
```bash
# 启动前端
cd web-client && npm run dev

# 执行 AI 生成的冒烟测试
cd web-client && npx playwright test ../testing/e2e/ai-smoke-test.spec.ts --project=edge

# 查看 trace 回放
cd web-client && npx playwright show-trace test-results/*/trace.zip
```

### 第二层：接口测试
```bash
# 复制 04-api-test-prompts.md 中的模板 1，发送给 Claude Code
# AI 会自动执行 curl 命令并报告结果
```

### 第三层：运维监控
```bash
# 手动执行
bash testing/scripts/ai-ops-monitor.sh

# 查看报告
cat testing/ops-reports/ops-report-*.md
```

---

## 核心方法论

每个层的模式完全相同：

```
1. 写地图 → 告诉 AI 有什么（组件/接口/指标）
2. 写提示词 → 告诉 AI 要做什么
3. AI 执行 → 自动操作 + 验证 + 报告
```

你只需要描述意图和验收标准，AI 负责执行和判断。
