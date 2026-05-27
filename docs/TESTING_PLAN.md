# AI-Native 智能商城 — 企业级测试计划

> 项目: AI-Native Smart Mall v1.0  
> 测试模型: V-Model  
> 日期: 2026-05-26  
> 所属: 测试部  

---

## 目录

1. [需求评审](#1-需求评审)
2. [测试计划](#2-测试计划)
   - 2.1 测试策略
   - 2.2 测试团队与角色职责
   - 2.3 测试范围
   - 2.4 测试排期
   - 2.5 测试环境
   - 2.6 工具清单
   - 2.7 风险识别
3. [用例设计](#3-用例设计)
4. [单元测试](#4-单元测试)
5. [集成测试](#5-集成测试)
6. [系统测试](#6-系统测试)
7. [验收测试](#7-验收测试)
8. [附录](#8-附录)

---

## 1. 需求评审

### 1.1 评审目标

确保测试团队充分理解需求，识别需求中的模糊点、矛盾点和不可测点，评估可测性。

### 1.2 评审输入

| 文档 | 来源 | 说明 |
|------|------|------|
| 产品需求规格说明书 | 产品经理 | 功能需求、非功能需求 |
| 技术架构文档 | 架构师 | 技术栈、中间件依赖、部署架构 |
| UI/UX 设计稿 | 设计师 | 管理端 + 用户端页面设计 |
| 数据库设计文档 | 后端开发 | 12张表的ER图、字段约束 |
| AI能力需求说明 | AI工程师 | 智能客服、SEO生成、情感分析需求 |
| 接口文档 | 后端开发 | REST API 定义（约50+接口） |

### 1.3 评审维度

| 维度 | 检查项 | 本项目关注点 |
|------|--------|-------------|
| 完整性 | 需求是否覆盖所有功能模块 | 用户认证、商品、订单、钱包、AI客服、搜索、统计、知识库 8大模块 |
| 一致性 | 前后端接口字段是否一致 | `publishStatus` vs `status`、mybatis-plus 逻辑删除 |
| 可测性 | 是否有明确的输入输出和边界条件 | 金额精度 DECIMAL(10,2)、分页默认值、JWT 24h过期 |
| 安全性 | 鉴权、注入防护、敏感信息处理 | JWT 拦截器、AI API Key、文件上传类型 |
| 性能要求 | QPS/RT 指标 | 商品查询 QPS≥500、AI对话并发、SSE 流式 |
| 兼容性 | 浏览器/设备支持 | Chrome/Firefox/Edge 最新2版，移动端适配 |

### 1.4 评审输出

| 输出物 | 责任人 | 时间 |
|--------|--------|------|
| 需求评审记录表 | 测试经理 | 评审会后1天 |
| 需求问题清单 | 测试工程师 | 评审会现场 |
| 可测性评估报告 | 测试经理 | 评审会后2天 |
| 需求基线确认 | 全体 | 问题全部关闭后 |

### 1.5 本项目重点关注需求点

```
必须覆盖（P0）:
  ├─ 用户注册/登录/JWT鉴权
  ├─ 商品列表(分类筛选+分页+排序)/详情
  ├─ 购物车 → 下单 → 钱包支付 → 订单状态流转
  ├─ 钱包余额/充值/扣款/退款
  └─ 后台商品管理(CRUD+上下架)

重要覆盖（P1）:
  ├─ AI智能客服(RAG检索+流式响应)
  ├─ AI商品SEO标题/描述生成
  ├─ AI评论情感分析
  ├─ 语义搜索/混合搜索
  ├─ 文件上传(单图+多图)
  └─ 数据Dashboard统计

一般覆盖（P2）:
  ├─ 知识库文档管理(上传+向量化)
  ├─ AI监控看板
  ├─ 商品口碑报告
  └─ 收货地址管理
```

---

## 2. 测试计划

### 2.1 测试策略

```
测试金字塔:
       ┌─────┐
       │ E2E │  5%  - 核心业务链路 Selenium/Cypress
       ├─────┤
       │ API │  25% - 接口自动化 Postman/Newman
       ├─────┤
       │集成 │  20% - Testcontainers 中间件集成
       ├─────┤
       │单元 │  50% - JUnit5 + Mockito
       └─────┘
```

### 2.2 测试团队与角色职责

| 角色 | 人数 | 职责 | 技能要求 |
|------|:--:|------|----------|
| **测试经理** | 1 | 制定测试策略与排期、资源协调、风险评估、测试报告审核、对外沟通 | 5年+测试经验，熟悉电商业务 |
| **高级测试工程师** | 1 | 用例评审、自动化框架搭建、性能测试、安全测试、指导初级工程师 | 3年+自动化测试，JMeter/Selenium |
| **测试工程师** | 2 | 功能测试用例设计与执行、缺陷跟踪、API自动化脚本编写、回归测试 | 1-3年功能测试，熟悉Postman/SQL |
| **测试开发工程师(SDET)** | 1 | 单元测试框架搭建、CI流水线集成、Testcontainers维护、代码覆盖率门禁 | 3年+ Java开发，Spring Boot/JUnit5 |
| **AI测试工程师** | 1 | AI功能专项测试、问答对构建、模型效果评估、意图识别验收 | 2年+测试经验，了解LLM/Embedding |
| **业务验收人员(UAT)** | 2 | 业务场景验收、用户体验反馈、灰度发布签收 | 业务方代表（产品/运营） |

**合计：8人**

```
测试经理 (1)
  │
  ├── 高级测试工程师 (1)
  │     ├── 测试工程师 A (功能测试+API自动化)
  │     └── 测试工程师 B (用例执行+缺陷跟踪)
  │
  ├── 测试开发工程师 SDET (1)
  │     └── 单元测试+集成测试+CI流水线
  │
  ├── AI测试工程师 (1)
  │     └── AI专项测试+模型评估
  │
  └── 业务验收人员 (2) [虚线汇报]
        └── UAT验收
```

**各角色在各阶段的投入比例：**

| 阶段 | 测试经理 | 高级工程师 | 测试工程师×2 | SDET | AI测试 |
|------|:--:|:--:|:--:|:--:|:--:|
| 需求评审 | 100% | 80% | 50% | 30% | 80% |
| 测试计划 | 100% | 100% | 40% | 50% | 50% |
| 用例设计 | 30% | 100% | 100% | 30% | 80% |
| 单元测试 | 10% | 30% | 30% | 100% | 30% |
| 集成测试 | 20% | 50% | 50% | 100% | 50% |
| 系统测试 | 50% | 100% | 100% | 50% | 100% |
| 验收测试 | 80% | 50% | 30% | 30% | 30% |

**协作分工明细：**

```
测试经理:
  ├─ 制定测试计划、排期、资源分配
  ├─ 组织需求评审和用例评审
  ├─ 跟踪测试进度，每日站会同步
  ├─ 缺陷Triage(分诊)，确定优先级
  ├─ 发布测试报告
  └─ 与项目经理/开发经理协调

高级测试工程师:
  ├─ 搭建API自动化框架(Postman/Newman)
  ├─ 编写核心业务链路自动化脚本
  ├─ 性能测试方案设计与执行(JMeter/k6)
  ├─ 安全测试(Burp/SQL注入/XSS)
  ├─ 评审测试工程师用例
  └─ 定位复杂缺陷根因

测试工程师 A:
  ├─ 商品+订单+钱包模块功能测试
  ├─ 编写API自动化用例(参数化数据)
  ├─ 缺陷记录与回归验证
  └─ 测试数据准备

测试工程师 B:
  ├─ 用户+搜索+文件+统计模块功能测试
  ├─ 兼容性测试(多浏览器/移动端)
  ├─ UI测试(Selenium脚本)
  └─ 用例库维护(TestLink)

测试开发工程师(SDET):
  ├─ 编写单元测试(JUnit5+Mockito)
  ├─ Testcontainers集成测试环境搭建
  ├─ JaCoCo覆盖率报告+SonarQube门禁
  ├─ GitHub Actions/Jenkins CI流水线
  └─ 测试工具链维护

AI测试工程师:
  ├─ 构建问答评估数据集(100+标准问答对)
  ├─ AI客服准确率评估 + 意图识别验收
  ├─ 语义搜索相关性评估(MRR/NDCG)
  ├─ SEO标题生成质量人工评估
  ├─ AI API降级+超时+敏感词测试
  └─ 向量检索Top-K准确率测试

业务验收人员:
  ├─ 执行UAT验收用例
  ├─ 从用户视角反馈体验问题
  ├─ 签署验收报告
  └─ 灰度发布期间监控业务指标
```

### 2.3 测试范围

| 模块 | 单元测试 | 集成测试 | API测试 | UI测试 | 性能测试 | 安全测试 |
|------|:--:|:--:|:--:|:--:|:--:|:--:|
| 用户认证 | [x] | [x] | [x] | [x] | [ ] | [x] |
| 商品管理 | [x] | [x] | [x] | [x] | [x] | [ ] |
| 订单系统 | [x] | [x] | [x] | [x] | [x] | [x] |
| 钱包系统 | [x] | [x] | [x] | [x] | [ ] | [x] |
| AI客服 | [x] | [x] | [x] | [x] | [x] | [x] |
| AI商品运营 | [x] | [x] | [x] | [x] | [ ] | [ ] |
| 搜索 | [x] | [x] | [x] | [x] | [x] | [x] |
| 文件上传 | [x] | [x] | [x] | [x] | [ ] | [x] |
| 统计 | [x] | [ ] | [x] | [x] | [ ] | [ ] |
| 知识库 | [x] | [x] | [x] | [x] | [ ] | [ ] |

### 2.4 测试排期

```
项目周期: 8周

Week 1-2: 需求评审 + 测试计划 + 环境搭建
  ├─ D1-D3:  需求评审，输出问题清单
  ├─ D4-D5:  测试策略制定，排期确认
  ├─ D6-D8:  测试环境搭建(Docker Compose + 测试数据)
  └─ D9-D10: 用例评审

Week 3-4: 单元测试 + 集成测试
  ├─ W3:  Mapper/Util/Service 单元测试编写
  ├─ W3:  AI Service Mock测试
  ├─ W4:  Testcontainers 集成测试
  └─ W4:  Controller MockMvc 测试

Week 5-6: 系统测试
  ├─ W5:  功能测试(8大模块全量)
  ├─ W5:  API自动化脚本编写(Newman)
  ├─ W6:  性能测试(JMeter + k6)
  ├─ W6:  安全测试(Burp + sqlmap + ZAP)
  └─ W6:  兼容性测试

Week 7: 缺陷修复 + 回归测试
  ├─ D1-D3: 缺陷集中修复
  ├─ D4-D5: 回归测试(自动化+手工)
  └─ D5:    缺陷冻结(无P0/P1)

Week 8: 验收测试 + 上线
  ├─ D1-D3: UAT验收
  ├─ D4:    灰度发布(10%→50%→100%)
  └─ D5:    上线监控
```

### 2.5 测试环境

| 环境 | 主机 | 配置 | 用途 |
|------|------|------|------|
| DEV | 开发本机 | Docker Compose全部中间件 | 开发自测 |
| TEST | 测试服务器(4C8G) | MySQL/Redis/ES/RocketMQ/Nacos | 功能/集成测试 |
| STAGING | 预发布(8C16G) | 与生产同配置 | 性能测试 + UAT |
| PROD | 生产(16C32G) | 集群 | 灰度 + 线上 |

### 2.6 工具清单

| 类别 | 工具 | 用途 |
|------|------|------|
| 项目管理 | Jira / 禅道 | 需求跟踪 + 缺陷管理 |
| 用例管理 | TestLink / 飞书多维表格 | 用例库 + 执行追踪 |
| 单元测试 | JUnit5 + Mockito + JaCoCo | Java单元测试 + 覆盖率 |
| 集成测试 | Testcontainers | 中间件集成 |
| API测试 | Postman + Newman | 接口自动化 + CI集成 |
| UI测试 | Selenium / Playwright | 前端E2E |
| 性能测试 | JMeter + k6 | 压力测试 + 并发 |
| 安全测试 | Burp Suite + sqlmap + OWASP ZAP | 安全扫描 |
| 代码扫描 | SonarQube + OWASP Dependency Check | 代码质量 + 依赖漏洞 |
| 监控 | Prometheus + Grafana + SkyWalking | 性能基线 + 异常告警 |

### 2.7 风险识别

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|----------|
| AI模型API不稳定 | 中 | 高 | Mock降级方案，回退到规则匹配 |
| 中间件版本不兼容 | 中 | 中 | Docker Compose 固定版本，Testcontainers 验证 |
| 需求变更频繁 | 高 | 中 | 敏捷迭代，用例及时更新 |
| 测试数据准备不足 | 低 | 中 | 预置100+商品、2账号、完整订单链路数据 |
| 性能基线不达标 | 中 | 高 | 提前压测，留2周优化缓冲 |
| 安全漏洞 | 低 | 高 | SonarQube门禁 + 人工渗透测试 |

---

## 3. 用例设计

### 3.1 用例设计方法

| 方法 | 适用场景 | 示例 |
|------|----------|------|
| 等价类划分 | 输入有明确范围 | 金额: 正数/零/负数/超大值 |
| 边界值分析 | 数值型边界 | pageNum: 0/-1/1/MAX/MAX+1 |
| 判定表 | 多条件组合 | 订单状态流转: (当前状态,操作,角色) → 新状态 |
| 状态迁移 | 状态流转 | 订单: 待支付→已支付→已发货→已完成 |
| 场景法 | 业务流程 | 用户注册→登录→浏览→下单→支付 |
| 错误推测 | 异常场景 | 并发扣库存、支付超时回滚 |

### 3.2 用例模板

| 字段 | 说明 | 示例 |
|------|------|------|
| 用例ID | TC-MODULE-NNN | TC-ORDER-001 |
| 模块 | 功能模块 | 订单管理 |
| 测试点 | 被测功能点 | 创建订单-库存扣减 |
| 优先级 | P0/P1/P2/P3 | P0 |
| 前置条件 | 执行前状态 | 用户已登录，购物车有商品，钱包余额充足 |
| 测试步骤 | 操作序列 | 1.选择商品 2.填写地址 3.提交订单 |
| 预期结果 | 预期输出 | 订单创建成功，库存减少，购物车清空 |
| 实际结果 | 执行后填写 | - |
| 状态 | Pass/Fail/Blocked | - |

### 3.3 核心业务用例（部分示例）

#### 订单状态流转用例

| ID | 前置状态 | 操作 | 角色 | 预期状态 | 优先级 |
|----|---------|------|------|---------|--------|
| TC-ORDER-001 | 待支付 | 钱包支付(余额充足) | 用户 | 已支付 | P0 |
| TC-ORDER-002 | 待支付 | 钱包支付(余额不足) | 用户 | 待支付(提示余额不足) | P0 |
| TC-ORDER-003 | 待支付 | 取消订单 | 用户 | 已取消(退款) | P0 |
| TC-ORDER-004 | 已支付 | 发货 | 管理员 | 已发货 | P0 |
| TC-ORDER-005 | 已发货 | 确认收货 | 用户 | 已完成 | P0 |
| TC-ORDER-006 | 已支付 | 申请退款 | 用户 | 已取消(退款到钱包) | P1 |
| TC-ORDER-007 | 已发货 | 申请退款 | 用户 | 拒绝(需先退货) | P1 |
| TC-ORDER-008 | 已完成 | 取消订单 | 用户 | 拒绝(终态不可取消) | P1 |

#### 钱包系统用例

| ID | 测试点 | 输入 | 预期 | 优先级 |
|----|--------|------|------|--------|
| TC-WALLET-001 | 查询余额 | 登录用户 | 返回余额 | P0 |
| TC-WALLET-002 | 充值 | amount=100 | 余额+100, 生成充值记录 | P0 |
| TC-WALLET-003 | 充值-负数 | amount=-50 | 提示金额非法 | P1 |
| TC-WALLET-004 | 充值-超大金额 | amount=999999999999 | 提示金额超限 | P2 |
| TC-WALLET-005 | 支付扣款 | 余额充足 | 扣除成功, 生成消费记录 | P0 |
| TC-WALLET-006 | 支付扣款-不足 | 余额不足 | 扣款失败, 余额不变 | P0 |
| TC-WALLET-007 | 退款 | 订单取消 | 金额退回, 冻结金额释放 | P0 |
| TC-WALLET-008 | 并发充值 | 同时2笔充值 | 金额正确累加 | P1 |

#### AI 客服用例

| ID | 测试点 | 输入 | 预期 | 优先级 |
|----|--------|------|------|--------|
| TC-AI-001 | 商品咨询 | "iPhone 15 多少钱" | 返回价格信息 | P1 |
| TC-AI-002 | FAQ匹配 | "如何退货" | 返回退货政策 | P1 |
| TC-AI-003 | 意图识别失败 | "今天天气怎么样" | 返回引导提示 | P2 |
| TC-AI-004 | 流式响应 | 开启SSE连接 | 逐字返回, 无丢失 | P1 |
| TC-AI-005 | 敏感词拦截 | 含敏感词输入 | 返回拒绝提示 | P1 |
| TC-AI-006 | API超时降级 | 断开AI API | 返回降级提示, 不崩溃 | P1 |
| TC-AI-007 | 空输入 | 空字符串 | 返回"请输入问题" | P2 |

#### 安全测试用例

| ID | 测试点 | 攻击向量 | 预期 | 优先级 |
|----|--------|---------|------|--------|
| TC-SEC-001 | SQL注入 | 搜索框输入 `' OR '1'='1` | 不返回额外数据 | P0 |
| TC-SEC-002 | XSS | 评论输入 `<script>alert(1)</script>` | 转义输出 | P0 |
| TC-SEC-003 | 未授权访问 | 无Token访问 `/api/wallet/info` | 返回401 | P0 |
| TC-SEC-004 | 水平越权 | 用户A Token 访问用户B订单 | 返回空或403 | P0 |
| TC-SEC-005 | JWT伪造 | 篡改Token中userId | 验签失败, 返回401 | P0 |
| TC-SEC-006 | 文件上传绕过 | 上传 `.jsp` 文件伪装成图片 | 拒绝上传 | P1 |
| TC-SEC-007 | 路径遍历 | 文件名 `../../../etc/passwd` | 拒绝或规范化路径 | P1 |

### 3.4 用例统计

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|------|----|----|----|----|------|
| 用户认证 | 8 | 6 | 4 | 2 | 20 |
| 商品管理 | 12 | 10 | 8 | 4 | 34 |
| 订单系统 | 15 | 10 | 8 | 3 | 36 |
| 钱包系统 | 10 | 8 | 5 | 2 | 25 |
| AI 客服 | 6 | 10 | 8 | 4 | 28 |
| AI 商品运营 | 4 | 8 | 6 | 3 | 21 |
| 搜索 | 4 | 6 | 5 | 2 | 17 |
| 文件上传 | 4 | 4 | 3 | 2 | 13 |
| 数据统计 | 3 | 5 | 4 | 2 | 14 |
| 知识库 | 3 | 5 | 4 | 2 | 14 |
| 安全测试 | 10 | 8 | 5 | 3 | 26 |
| **合计** | **79** | **80** | **60** | **29** | **248** |

---

## 4. 单元测试

### 4.1 技术栈

```xml
<!-- pom.xml 测试依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <scope>test</scope>
</dependency>
```

### 4.2 覆盖率目标

```
整体覆盖率目标: ≥80%

分层目标:
  Mapper 层:  ≥90%  (SQL正确性、逻辑删除、分页查询)
  Service 层: ≥85%  (业务逻辑、事务边界、异常处理)
  AI Service: ≥75%  (Mock AI API、向量检索、流式响应)
  Controller: ≥80%  (参数校验、权限拦截、响应格式)
  Util 层:    ≥95%  (纯函数，容易覆盖)
  Entity 层:  ≥90%  (Lombok生成的getter/setter可豁免)
```

### 4.3 测试结构

```
src/test/java/org/example/java_ai/
├── mapper/
│   ├── ProductMapperTest.java
│   ├── UserMapperTest.java
│   ├── OrderMapperTest.java
│   └── WalletMapperTest.java
├── service/
│   ├── ProductServiceTest.java
│   ├── UserServiceTest.java
│   ├── OrderServiceTest.java
│   ├── WalletServiceTest.java
│   └── ai/
│       ├── SmartCustomerServiceTest.java
│       ├── ProductOperationServiceTest.java
│       ├── SemanticSearchServiceTest.java
│       └── IntentRecognitionServiceTest.java
├── controller/
│   ├── UserControllerTest.java
│   ├── ProductControllerTest.java
│   ├── OrderControllerTest.java
│   ├── WalletControllerTest.java
│   └── AiControllerTest.java
├── util/
│   ├── TokenUtilTest.java
│   ├── FileUploadUtilTest.java
│   └── DataMaskingUtilTest.java
└── config/
    └── SecurityConfigTest.java
```

### 4.4 关键测试示例

#### Mapper 测试（H2 内存数据库）

```java
@MybatisPlusTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Test
    @DisplayName("分页查询-已上架有库存商品-按时间降序")
    void selectPage_PublishedWithStock_OrderByCreateTime() {
        // Given
        Page<Product> page = new Page<>(1, 10);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getPublishStatus, 1)
               .gt(Product::getStock, 0)
               .eq(Product::getDeleted, 0);

        // When
        Page<Product> result = productMapper.selectPage(page, wrapper);

        // Then
        assertNotNull(result);
        assertTrue(result.getTotal() > 0);
        // 验证时间降序: 第一个的createTime >= 最后一个
        List<Product> records = result.getRecords();
        assertFalse(records.get(0).getCreateTime()
                .isBefore(records.get(records.size()-1).getCreateTime()));
    }

    @Test
    @DisplayName("逻辑删除-标记deleted=1-普通查询查不到")
    void logicDelete_SetDeleted_SkipInNormalQuery() {
        // Given
        Product product = new Product();
        product.setName("测试商品");
        product.setDeleted(0);
        productMapper.insert(product);

        // When: 逻辑删除
        product.setDeleted(1);
        productMapper.updateById(product);

        // Then: 普通查询查不到
        Product result = productMapper.selectById(product.getId());
        assertNull(result);
    }
}
```

#### Service 测试（Mockito）

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private ProductMapper productMapper;
    @Mock private UserWalletMapper walletMapper;
    @Mock private RechargeRecordMapper rechargeMapper;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("创建订单-库存充足钱包余额充足-成功")
    void createOrder_SufficientStockAndBalance_Success() {
        // Given
        Long userId = 1L;
        Long productId = 100L;
        Product product = buildProduct(productId, 9999.00, 50); // 库存50
        UserWallet wallet = buildWallet(userId, new BigDecimal("20000.00"));

        when(productMapper.selectById(productId)).thenReturn(product);
        when(walletMapper.selectOne(any())).thenReturn(wallet);

        // When
        OrderResult result = orderService.createOrder(userId, productId, 1);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("9999.00"), result.getTotalAmount());
        verify(productMapper).updateById(argThat(p -> p.getStock() == 49));
        verify(walletMapper).updateById(argThat(w ->
            w.getBalance().compareTo(new BigDecimal("10001.00")) == 0));
    }

    @Test
    @DisplayName("创建订单-库存不足-抛异常")
    void createOrder_InsufficientStock_ThrowException() {
        Product product = buildProduct(100L, 5000.00, 0); // 库存0
        when(productMapper.selectById(100L)).thenReturn(product);

        assertThrows(BusinessException.class, () ->
            orderService.createOrder(1L, 100L, 1));
    }

    @Test
    @DisplayName("创建订单-钱包余额不足-返回错误")
    void createOrder_InsufficientBalance_ReturnError() {
        Product product = buildProduct(100L, 99999.00, 50);
        UserWallet wallet = buildWallet(1L, new BigDecimal("100.00"));
        when(productMapper.selectById(100L)).thenReturn(product);
        when(walletMapper.selectOne(any())).thenReturn(wallet);

        OrderResult result = orderService.createOrder(1L, 100L, 1);

        assertFalse(result.isSuccess());
        assertEquals("余额不足", result.getMessage());
        // 未扣库存
        verify(productMapper, never()).updateById(any());
    }
}
```

#### AI Service 测试（WireMock）

```java
@WireMockTest(httpPort = 8089)
class SmartCustomerServiceTest {

    @Test
    @DisplayName("智能客服-正常问答-返回AI回复")
    void answerQuestion_NormalQuery_ReturnAiResponse() {
        // Given: Mock AI API
        stubFor(post(urlPathEqualTo("/compatible-mode/v1/chat/completions"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"choices":[{"message":{"content":"iPhone 15的价格是5999元起"}}]}
                    """)));

        // When
        String answer = smartCustomerService.answerQuestion("iPhone 15 多少钱");

        // Then
        assertNotNull(answer);
        assertTrue(answer.contains("5999"));
    }

    @Test
    @DisplayName("智能客服-API超时-降级提示")
    void answerQuestion_ApiTimeout_FallbackMessage() {
        stubFor(post(urlPathEqualTo("/compatible-mode/v1/chat/completions"))
            .willReturn(aResponse()
                .withFixedDelay(35000))); // 超过30s超时

        String answer = smartCustomerService.answerQuestion("什么商品好");

        assertNotNull(answer);
        assertTrue(answer.contains("暂时无法回答") || answer.contains("稍后重试"));
    }

    @Test
    @DisplayName("智能客服-敏感词-拒绝回答")
    void answerQuestion_SensitiveContent_Reject() {
        String answer = smartCustomerService.answerQuestion("违规内容xxx");

        assertTrue(answer.contains("无法回答") || answer.contains("涉及敏感"));
    }
}
```

#### Controller 测试（MockMvc）

```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProductService productService;

    @Test
    @DisplayName("GET /api/product/list-默认参数-返回分页数据")
    void listProducts_DefaultParams_ReturnPage() throws Exception {
        when(productService.listProducts(1, 10, null, null, "create_time", "desc"))
            .thenReturn(buildMockPage());

        mockMvc.perform(get("/api/product/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    @DisplayName("GET /api/product/list-pageNum=0-默认使用1")
    void listProducts_PageNumZero_DefaultToOne() throws Exception {
        mockMvc.perform(get("/api/product/list")
                .param("pageNum", "0"))
            .andExpect(status().isOk());

        verify(productService).listProducts(1, 10, null, null, "create_time", "desc");
    }

    @Test
    @DisplayName("GET /api/product/list-未登录-返回401")
    void listProducts_NoToken_Return401() throws Exception {
        // 假设该接口需要登录
        mockMvc.perform(get("/api/product/list"))
            .andExpect(status().isOk()); // 商品列表目前无需登录
    }

    @Test
    @DisplayName("POST /api/product-缺少必填字段-返回参数错误")
    void createProduct_MissingRequiredField_Return400() throws Exception {
        String json = """
            {"name":"","price":null,"categoryId":1}
            """;

        mockMvc.perform(post("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }
}
```

### 4.5 CI 集成

```yaml
# .github/workflows/test.yml (示例)
name: Unit & Integration Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Unit Tests
        run: ./mvnw test -Punit
      - name: JaCoCo Report
        run: ./mvnw jacoco:report
      - name: Coverage Gate
        run: |
          COVERAGE=$(grep -Po 'Total.*?([0-9]{2,3})%' target/site/jacoco/index.html)
          echo "Coverage: $COVERAGE"
          # 低于80%构建失败
      - name: SonarQube Scan
        run: ./mvnw sonar:sonar -Dsonar.host.url=$SONAR_URL
```

---

## 5. 集成测试

### 5.1 集成测试矩阵

| 组件 | 版本 | 测试工具 | 验证点 |
|------|------|----------|--------|
| MySQL | 8.0 | Testcontainers | CRUD、事务、索引、逻辑删除 |
| Redis Stack | 7.x | Testcontainers | 向量写入、相似度查询、维度校验 |
| Elasticsearch | 8.11 | Testcontainers | 全文搜索、中文分词、索引管理 |
| RocketMQ | 5.1 | Testcontainers | 消息发送、消费、重试 |
| Nacos | 2.2 | Testcontainers | 服务注册、配置管理 |
| AI API | 通义千问 | WireMock | 请求格式、超时降级、流式响应 |

### 5.2 Testcontainers 配置

```java
@SpringBootTest
@Testcontainers
class DatabaseIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("ai_mall")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("schema.sql");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis/redis-stack:latest"))
        .withExposedPorts(6379);

    @Container
    static ElasticsearchContainer<?> es =
        new ElasticsearchContainer<>("elasticsearch:8.11.0")
            .withEnv("xpack.security.enabled", "false");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.elasticsearch.uris", () ->
            "http://" + es.getHost() + ":" + es.getMappedPort(9200));
    }
}
```

### 5.3 关键集成测试

```java
@Test
@DisplayName("Redis向量-写入商品嵌入-检索Top5-校验余弦相似度")
void redisVector_InsertAndSearch_ReturnTop5() {
    // Given: 初始化商品向量
    Product product = buildProduct("iPhone 15", "手机数码");
    Embedding embedding = embeddingModel.embed(product.getDescription()).content();
    embeddingStore.add(embedding.embeddingId(), embedding.vector());

    // When: 语义搜索
    Embedding queryEmbedding = embeddingModel.embed("苹果手机").content();
    List<EmbeddingMatch<TextSegment>> results =
        embeddingStore.findRelevant(queryEmbedding.vector(), 5);

    // Then
    assertEquals(5, results.size());
    assertTrue(results.get(0).score() > 0.7); // 余弦相似度 ≥0.7
}

@Test
@DisplayName("MySQL事务-下单扣库存+扣钱包-任一失败则回滚")
@Transactional
void orderCreation_Transactional_AtomicOperation() {
    // Given
    Product product = insertProduct("测试商品", 100, 50); // 价格100，库存50

    // When: 模拟扣款失败
    assertThrows(Exception.class, () -> {
        productService.decrementStock(product.getId(), 1);
        walletService.deductBalance(1L, new BigDecimal("999999")); // 余额不足
    });

    // Then: 库存应该回滚
    Product after = productService.getProductById(product.getId());
    assertEquals(50, after.getStock()); // 库存未变
}

@Test
@DisplayName("ES全文搜索-中文分词-搜索'小米手机'匹配包含'小米'的商品")
void elasticsearch_FullText_ChineseTokenizer() {
    // Given: 索引商品
    indexProduct("小米14 Pro", "小米旗舰手机，骁龙8Gen3");
    indexProduct("iPhone 15", "Apple iPhone 15");

    // When
    List<Product> results = searchService.fullTextSearch("小米手机");

    // Then
    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(p -> p.getName().contains("小米")));
}
```

---

## 6. 系统测试

### 6.1 功能测试执行计划

#### 第1轮：冒烟测试（1天）

验证核心链路是否打通，快速发现阻塞性问题。

| 场景 | 步骤 | 预期 | 结果 |
|------|------|------|------|
| 用户注册登录 | 注册→登录→获取用户信息 | 200 + Token | - |
| 商品浏览 | 访问首页→商品列表→商品详情 | 有商品展示 | - |
| 下单支付 | 加购物车→下单→钱包支付 | 订单状态=已支付 | - |
| 后台管理 | 管理员登录→商品管理→上架 | 操作成功 | - |

#### 第2轮：全量功能测试（3天）

参照 3.4 节用例清单逐条执行，使用 TestLink 记录结果。

#### 第3轮：回归测试（1天）

缺陷修复后，重新运行 P0+P1 用例 + 自动化脚本。

### 6.2 API 自动化测试（Newman）

```javascript
// Postman Collection 核心结构
{
  "info": { "name": "AI-Mall API Tests" },
  "item": [
    {
      "name": "01-用户认证",
      "item": [
        {
          "name": "登录成功",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('状态码200', () => pm.response.to.have.status(200));",
                  "pm.test('success=true', () => {",
                  "  const json = pm.response.json();",
                  "  pm.expect(json.success).to.be.true;",
                  "  pm.expect(json.token).to.exist;",
                  "  pm.environment.set('token', json.token);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "登录-密码错误",
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('success=false', () => pm.expect(pm.response.json().success).to.be.false);"
            ]}
          }]
        }
      ]
    },
    {
      "name": "02-商品管理",
      "item": [
        { "name": "商品列表-默认参数" },
        { "name": "商品列表-分页" },
        { "name": "商品列表-按价格排序" },
        { "name": "商品列表-分类筛选" },
        { "name": "商品详情-正常ID" },
        { "name": "商品详情-不存在的ID→404" }
      ]
    },
    {
      "name": "03-订单流程",
      "item": [
        { "name": "下单-正常" },
        { "name": "下单-库存不足" },
        { "name": "支付-余额充足" },
        { "name": "支付-余额不足" },
        { "name": "确认收货" }
      ]
    },
    {
      "name": "04-钱包",
      "item": [
        { "name": "查询余额" },
        { "name": "充值-正常" },
        { "name": "充值-负数" },
        { "name": "充值记录" }
      ]
    },
    {
      "name": "05-AI客服",
      "item": [
        { "name": "商品咨询" },
        { "name": "FAQ匹配" },
        { "name": "空输入" }
      ]
    }
  ]
}
```

**CI 执行命令：**

```bash
# Jenkinsfile
stage('API Test') {
    steps {
        sh 'newman run ai-mall-tests.postman_collection.json \
            --env-var base_url=http://test-server:8080 \
            --reporters cli,junit,htmlextra \
            --reporter-junit-export reports/api-results.xml \
            --reporter-htmlextra-export reports/api-report.html'
    }
    post {
        always {
            junit 'reports/api-results.xml'
            publishHTML([reportDir: 'reports', reportFiles: 'api-report.html'])
        }
    }
}
```

### 6.3 性能测试脚本

#### JMeter 线程组配置

```
场景1: 商品列表查询（读为主）
  线程数: 100 → 200 → 500 (梯度)
  Ramp-Up: 60s
  持续时间: 300s
  目标QPS: ≥500
  P99响应时间: <200ms

场景2: 下单支付（写为主）
  线程数: 50 → 100
  Ramp-Up: 30s
  持续时间: 180s
  目标TPS: ≥100
  错误率: <0.1%

场景3: AI客服问答（长连接）
  并发SSE连接: 20 → 50
  持续时间: 120s
  验证点: SSE流式不中断，无字符丢失

场景4: 混合压测（模拟真实流量）
  读写比: 80:20
  线程组:
    - 商品浏览: 80%
    - 搜索: 10%
    - 下单: 8%
    - AI客服: 2%
```

#### k6 流式压测脚本

```javascript
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '60s', target: 50 },
    { duration: '30s', target: 0 },
  ],
};

export default function () {
  const response = http.get('http://test-server:8080/api/ai/customer-service/stream', {
    params: { headers: { 'Accept': 'text/event-stream' } }
  });

  check(response, {
    'SSE status 200': (r) => r.status === 200,
    'Has data stream': (r) => r.body.includes('data:'),
  });

  sleep(1);
}
```

### 6.4 安全测试执行

```bash
# 1. SQL注入扫描
sqlmap -u "http://test-server:8080/api/product/list?keyword=test" \
  --batch --level=3 --risk=2 --tamper=space2comment

# 2. XSS 测试（手工 + ZAP自动化）
# 在评论输入框、搜索框、昵称字段输入:
# <script>alert('XSS')</script>
# <img src=x onerror=alert('XSS')>
# javascript:alert('XSS')

# 3. JWT 伪造测试
# 抓取合法Token，修改payload中userId，验证服务器拒绝

# 4. 文件上传绕过测试
curl -X POST http://test-server:8080/api/upload/image \
  -F "file=@shell.jsp;type=image/jpeg"

# 5. 未授权访问测试
curl http://test-server:8080/api/wallet/info           # 无Token → 401
curl http://test-server:8080/api/admin/product/list     # 无Token → 401
```

### 6.5 兼容性测试矩阵

| 浏览器 | 版本 | 管理端(3000) | 用户端(3001) | 备注 |
|--------|------|:--:|:--:|------|
| Chrome | 130+ | [x] | [x] | 主力浏览器 |
| Firefox | 130+ | [x] | [x] | |
| Edge | 130+ | [x] | [x] | |
| Chrome | 移动端模拟 | [ ] | [x] | 375×667 |
| Safari | iOS模拟 | [ ] | [x] | |

---

## 7. 验收测试

### 7.1 UAT 测试流程

```
步骤1: 准备UAT环境
  ├─ 部署到STAGING环境
  ├─ 导入脱敏生产数据（或充分测试数据）
  └─ 开通UAT账号

步骤2: UAT测试执行
  ├─ 业务方按UAT用例执行
  ├─ 关键操作: 注册→浏览→下单→支付→退款 完整链路
  ├─ 管理后台: 商品上架→AI生成SEO→统计查看
  └─ 异常操作: 取消订单、退款、修改地址

步骤3: 问题记录与修复
  ├─ 问题登记到Jira
  ├─ 开发修复
  └─ 测试验证

步骤4: UAT签收
  ├─ 所有P0/P1问题关闭
  ├─ 业务方签署验收报告
  └─ 准备上线
```

### 7.2 UAT 验收清单

| 序号 | 验收项 | 标准 | 通过 | 备注 |
|------|--------|------|:--:|------|
| 1 | 用户可以正常注册和登录 | 注册成功率≥99% | [ ] | |
| 2 | 商品浏览体验流畅 | 列表加载<1s | [ ] | |
| 3 | 下单-支付全链路可用 | 成功率≥99% | [ ] | |
| 4 | 订单状态正确流转 | 状态机无异常 | [ ] | |
| 5 | 钱包充值/扣款/退款准确 | 金额无误差 | [ ] | |
| 6 | AI客服回答基本准确 | 准确率≥85% | [ ] | |
| 7 | AI商品SEO标题可读 | 通过率≥80% | [ ] | |
| 8 | 后台管理功能完整 | CRUD全覆盖 | [ ] | |
| 9 | 数据统计正确 | 与数据库一致 | [ ] | |
| 10 | 无严重安全漏洞 | 无P0安全缺陷 | [ ] | |

### 7.3 灰度发布策略

```
阶段1: 金丝雀发布 (10% 流量, 持续2小时)
  ├─ 监控: 错误率、响应时间、CPU/内存
  └─ 回滚条件: 错误率>1% 或 P99延迟>2x基线

阶段2: 扩大灰度 (50% 流量, 持续4小时)
  ├─ 监控: 同上 + 支付成功率、AI问答延迟
  └─ 回滚条件: 支付成功率<99%

阶段3: 全量发布 (100% 流量)
  ├─ 持续监控24小时
  └─ 日常巡检接入

每个阶段: 确认无回归 → 进入下一阶段
```

---

## 8. 附录

### 8.1 测试数据准备

```sql
-- 测试用户
-- admin / admin123 (已存在)
-- test_user / admin123 (已存在)
-- 额外创建: tester01-tester10 (用于并发/边界测试)

-- 测试分类: 手机数码、电脑办公、家用电器、服装鞋帽、食品饮料 (已存在)

-- 测试商品: 105个 (已存在, publish_status=1)

-- 测试钱包:
-- admin:  5000.00
-- test_user: 1000.00

-- 测试订单: 需预置各状态订单用于状态流转测试
-- 待支付、已支付、已发货、已完成、已取消 各2个
```

### 8.2 缺陷严重等级定义

| 等级 | 定义 | 处理时限 | 示例 |
|------|------|----------|------|
| P0-阻塞 | 核心功能不可用，阻塞测试 | 立即修复 | 登录失败、支付金额错误、数据库连不上 |
| P1-严重 | 核心功能异常，影响较大 | 当天修复 | AI回答错误率高、订单状态流转异常 |
| P2-一般 | 非核心功能异常 | 本迭代修复 | 页面显示错位、提示文案不友好 |
| P3-建议 | 体验优化建议 | backlog | 增加动画效果、优化搜索建议 |

### 8.3 测试报告模板

```
AI-Native智能商城测试报告
═══════════════════════════

版本: v1.0
测试周期: 2026-MM-DD ~ 2026-MM-DD
测试环境: TEST / STAGING

一、测试概要
  计划用例: 248
  执行用例: ___
  通过: ___
  失败: ___
  阻塞: ___
  通过率: ___%

二、缺陷统计
  P0: ___ (已修复___, 未修复___)
  P1: ___ (已修复___, 未修复___)
  P2: ___ (已修复___, 未修复___)
  P3: ___

三、覆盖率
  单元测试覆盖率: ___%
  API自动化覆盖: ___%

四、性能基线
  商品列表QPS: ___
  订单TPS: ___
  P99延迟: ___ms

五、风险评估
  [列出未修复缺陷及风险]

六、测试结论
  [通过/不通过/有条件通过]
```

### 8.4 项目当前状态与缺口

```
❌ 缺失项:
  1. 无测试用例（0个）
  2. 无单元测试代码
  3. 无集成测试
  4. 无API自动化脚本
  5. 无性能测试基线
  6. 无CI/CD流水线
  7. SonarQube未集成
  8. schema.sql缺少 user_wallet、recharge_record 表(已修复)

✅ 已具备:
  1. Docker Compose 开发环境
  2. 完善的API接口
  3. schema.sql建表脚本
  4. 前端页面完整
  5. application.yml配置完整

📋 建议优先级:
  1. 补充核心Service单元测试 (2周)
  2. 搭建Testcontainers集成测试 (1周)
  3. 编写Newman接口自动化 (1周)
  4. 集成SonarQube静态扫描 (0.5周)
  5. JMeter性能压测建立基线 (1周)
  6. 搭建Jenkins/GitHub Actions CI (1周)
  7. 补充安全测试用例 (0.5周)
  8. 编写E2E测试脚本 (1周)
```

---

> 文档编制: 测试部  
> 审核: 测试经理  
> 批准: 项目经理  
> 版本: v1.0  
> 日期: 2026-05-26
