# Bug Tracker — 企业测试流程

> 开始时间: 2026-07-06 | 测试人员: Admin-XQY
> 环境: Docker Compose (localhost:3001/3000/8081)

---

## 第 1 步：全量回归测试

**命令**: `mvn test -B` / `npx vitest run`
**初始结果**: 133 用例 / 45 失败 / 7 错误 / 3 跳过
**最终结果**: 155 用例 / 0 失败 / 0 错误 / 4 跳过 → BUILD SUCCESS ✅
**前端 Vitest**: 25/25 通过 ✅

---

### BUG-001: Controller 测试路径未适配 API 版本化 | P0 | ✅ 已修复

**现象**: 45 个 Controller 测试全部 404
**根因**: Round 5 改动 `context-path=/api/v1` + `@RequestMapping` 去 `/api/` 前缀，测试仍用 `/api/user/login`  
**解决**: 批量 `sed 's|"/api/|"/|g'` 所有 test 文件
**耗时**: 5 分钟

---

### BUG-002: Mapper 测试 Flyway 与 H2 冲突 | P0 | ✅ 已修复

**现象**: 7 个 Mapper 测试 ApplicationContext 启动失败
**根因**: 新增 Flyway 迁移脚本 `db/migration/V*` 在 H2 上语法不兼容
**解决**:
- application-test.yml: 改 H2 URL + `flyway.enabled: false` + `sentinel.enabled: false`  
- @MybatisPlusTest: 加 `excludeAutoConfiguration = FlywayAutoConfiguration.class`
- JavaAiApplicationTests: `@Disabled("需要 MySQL + Redis 容器")`
**耗时**: 10 分钟

---

### BUG-003: UserServiceTest 缺 RedisRateLimiter mock | P1 | ✅ 已修复

**现象**: 9 个 UserServiceTest error — UnnecessaryStubbingException
**根因**: Round 3 注入 RedisRateLimiter，`@BeforeEach` mock 了但非 login 测试报 unnecessary stub
**解决**: `lenient().when(rateLimiter.tryAcquire(...)).thenReturn(true)`
**耗时**: 3 分钟

---

