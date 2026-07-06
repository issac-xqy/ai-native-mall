
/.///# 测试体系

> AI-Native 智能商城 — 三层 AI 驱动测试

## 目录结构

```
testing/
├── README.md                       # 本文件
│
├── docs/                           # 测试文档
│   ├── 01-产品需求规格说明书.md
│   ├── 02-技术架构文档.md
│   ├── 03-UI设计清单.md
│   ├── 04-数据库设计文档.md
│   ├── 05-AI能力需求说明.md
│   ├── 06-REST API接口文档.md
│   └── ai-driven/                  # AI 驱动测试体系
│       ├── README.md               #   AI 测试总览
│       ├── 01-ui-component-map.md  #   10 页面的组件地图
│       ├── 02-ui-test-prompts.md   #   7 套 UI 测试提示词
│       ├── 03-api-map.md           #   11 模块接口地图
│       ├── 04-api-test-prompts.md  #   5 套接口测试提示词
│       └── 05-ops-monitor.md       #   运维监控说明
│
├── e2e/                            # Playwright E2E 测试（已迁移到 web-client/e2e/）
│
└── scripts/                        # 测试脚本
    └── ai-ops-monitor.sh           #   运维监控（7 个 tool）
```

## 三层 AI 测试

| 层级 | 做什么 | 怎么跑 |
|:---:|--------|--------|
| **UI+功能** | AI 操控浏览器，检查组件、点击、验证数据 | `cd web-client && npx playwright test --project=edge` |
| **接口+性能** | AI 调 API，断言响应，压测 | 按 `docs/ai-driven/04-api-test-prompts.md` 执行 |
| **运维监控** | AI 读 CPU/内存/日志，异常告警 | `bash testing/scripts/ai-ops-monitor.sh` |

## 快速开始

```bash
# UI 测试
cd web-client && npx playwright test --project=edge

# 接口测试（提示词已执行验证，参考模板）
# 打开 docs/ai-driven/04-api-test-prompts.md，复制模板发送给 AI

# 运维监控
bash testing/scripts/ai-ops-monitor.sh
```

## 核心方法论

```
写地图（告诉 AI 有什么）→ 写提示词（告诉 AI 做什么）→ AI 执行 + 验证 + 报告
```
