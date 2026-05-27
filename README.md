# AI-Native 智能商城

> Spring Boot 3.2 + JDK 21 + LangChain4j + Vue 3 — AI 原生电商系统

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.36.2-purple.svg)](https://docs.langchain4j.dev/)
[![Vue](https://img.shields.io/badge/Vue-3.4-42b883.svg)](https://vuejs.org/)
[![Tests](https://img.shields.io/badge/tests-172%20passed-success.svg)]()

---

## 项目简介

AI-Native Smart Mall 是一套融合 Java + AI 的企业级商城系统，涵盖用户端、管理后台、AI 智能客服。

**核心功能**

- 用户注册/登录、商品浏览/搜索/分类、购物车、下单支付
- 钱包充值/扣款/退款、订单全生命周期管理
- AI 智能客服（RAG 向量检索 + LLM 流式响应）
- AI 商品运营（SEO 标题生成、商品描述、评论情感分析）
- 数据统计看板、知识库文档管理

---

## 快速开始

### 前置要求

- JDK 21、Maven 3.8+
- Docker（可选，用于启动中间件）
- 通义千问 API Key（[申请地址](https://dashscope.aliyun.com/)）

### 1. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入真实的数据库密码和 AI API Key
```

### 2. 启动中间件（可选）

```bash
docker-compose up -d   # MySQL + Redis + ES + Nacos + RocketMQ
```

### 3. 启动后端

```bash
./mvnw spring-boot:run
# 后端运行在 http://localhost:8081
```

### 4. 启动前端

```bash
cd web-client
npm install
npm run dev
# 用户端运行在 http://localhost:3001
```

### 5. 验证

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 + JDK 21 |
| ORM | MyBatis Plus 3.5.7 |
| AI | LangChain4j 0.36.2 + 通义千问 / DeepSeek |
| 向量检索 | Redis Stack 7.x + all-MiniLM-L6-v2 |
| 搜索引擎 | Elasticsearch 8.x |
| 注册配置 | Nacos 2.x |
| 流量防护 | Sentinel |
| 消息队列 | RocketMQ 5.x |
| 前端 | Vue 3.4 + Element Plus + Pinia |

---

## 项目结构

```
ai-native-mall/
├── src/main/java/org/example/java_ai/
│   ├── controller/          API 控制器（用户端 + 管理端）
│   ├── service/             业务接口
│   │   ├── impl/            业务实现
│   │   └── ai/              AI 服务（客服、商品运营、语义搜索）
│   ├── mapper/              MyBatis Mapper
│   ├── entity/              实体类
│   ├── config/              配置类
│   ├── common/              通用模型（Result、PageResult）
│   ├── exception/           异常处理
│   └── util/                工具类
├── src/test/java/.../       152 个测试（单元 + 集成 + 接口）
├── src/main/resources/
│   ├── application.yml      应用配置
│   ├── schema.sql           建库脚本
│   └── sql/                 数据库迁移脚本
├── web-client/              Vue 3 用户端（含 E2E 测试）
├── web-admin/               Vue 3 管理后台
├── docs/                    项目文档
├── docker-compose.yml       中间件编排
├── .env.example             环境变量模板
└── pom.xml                  Maven 构建
```

---

## 测试

```bash
./mvnw test                    # 后端 152 个测试
cd web-client && npm test      # 前端 20 个测试
npm run test:e2e               # E2E 13 个测试
```

**测试分层**

| 层级 | 数量 | 技术 |
|------|:--:|------|
| Util | 39 | JUnit 5 |
| Mapper | 28 | H2 + MyBatis Plus Test |
| Service | 42 | Mockito |
| Controller | 43 | MockMvc |
| E2E | 13 | Playwright + Edge |
| 前端 | 20 | Vitest |
| **合计** | **172** | |

---

## API 示例

### 商品列表

```bash
curl http://localhost:8081/api/product/list
```

### 用户登录

```bash
curl -X POST http://localhost:8081/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### AI 智能客服

```bash
curl -X POST http://localhost:8081/api/ai/customer-service/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"iPhone 15 多少钱？","userId":"1","sessionId":"test"}'
```

---

## 安全

- API Key 通过 `.env` 环境变量管理，不硬编码在源码中
- `.env` 已加入 `.gitignore`，不会提交到版本控制
- 用户密码 BCrypt 加密存储
- 隐私数据（手机号、身份证等）自动脱敏
- Sentinel 熔断降级保护 AI 接口
- JWT Token 鉴权 + 接口限流

---

## 文档

| 文档 | 说明 |
|------|------|
| [架构设计](docs/ARCHITECTURE.md) | 完整技术架构、AI 能力详解 |
| [快速启动](docs/QUICK_START.md) | 详细部署指南 |
| [测试计划](docs/TESTING_PLAN.md) | 248 条用例、测试策略 |
| [验收报告](docs/ACCEPTANCE_REPORT.md) | 验收标准对照 |
| [项目总结](docs/PROJECT_SUMMARY.md) | 交付物清单 |

---

**版本**: v1.0.0 | **更新**: 2026-05-27
