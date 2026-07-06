# AI 运维监控助手

> 对应三层 AI 测试的第三层：服务器运维监控
> 脚本位置：`testing/scripts/ai-ops-monitor.sh`

---

## 设计原则（来自同事经验）

- **只读权限**：只能读取系统指标和日志，不能 delete/write 文件
- **自动巡检**：定时任务扫描 CPU、内存、磁盘、日志
- **异常报警**：超过阈值自动标记告警
- **AI 分析**：报错或崩溃时读取日志，AI 分析原因
- **通知闭环**：分析结果发送到运维邮箱

---

## 内置 7 个 Tool

| Tool | 功能 | 只读 |
|:----:|------|:---:|
| `check_health` | 调用 `/actuator/health` 检查服务可用性 | ✅ |
| `check_cpu` | 读取 CPU 使用率，超 80% 告警 | ✅ |
| `check_memory` | 读取内存使用率，超 85% 告警 | ✅ |
| `check_disk` | 读取磁盘使用率，超 90% 告警 | ✅ |
| `scan_logs` | 扫描 `logs/` 目录，匹配异常日志（ERROR/Exception/FATAL/OutOfMemoryError） | ✅ |
| `check_api_availability` | 巡检核心接口（health/product/category）HTTP 状态码 | ✅ |
| `check_ai_monitor` | 读取 AI 调用监控数据（需 token） | ✅ |

---

## 使用方式

### 手动执行
```bash
cd /path/to/ai-native-mall
bash testing/scripts/ai-ops-monitor.sh
```

### 定时任务（每 5 分钟）
```bash
crontab -e
# 添加：
*/5 * * * * /bin/bash /path/to/ai-native-mall/testing/scripts/ai-ops-monitor.sh
```

### 输出
```
ops-reports/
└── ops-report-20260609-163000.md   # 每次执行生成一份 Markdown 报告
```

---

## 告警阈值配置

编辑脚本顶部变量：
```bash
CPU_THRESHOLD=80        # CPU 告警阈值 %
MEMORY_THRESHOLD=85     # 内存告警阈值 %
DISK_THRESHOLD=90       # 磁盘告警阈值 %
ALERT_EMAIL="ops@company.com"   # 接收告警的邮箱
```

---

## 扩展方向

1. **AI 日志分析**：将 `scan_logs` 捕获的异常日志抛给 AI（Claude API/LangChain4j），自动分析根因
2. **钉钉/企微通知**：替换 `mail` 命令，发送到 IM 群
3. **自动建工单**：检测到 P0 级别异常时，调用 Jira/禅道 API 自动建 bug
4. **指标持久化**：将每次扫描的 CPU/内存数据写入时序数据库（InfluxDB/Prometheus），用于趋势分析
