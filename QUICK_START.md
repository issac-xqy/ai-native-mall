# AI-Native智能商城 - 快速启动指南

> **目标**: 5分钟内完成环境搭建并启动项目

---

## 🚀 第一步：安装JDK 21

### Windows系统

```powershell
# 使用SDKMAN (推荐)
scoop install java21

# 或手动下载安装
# 下载地址: https://adoptium.net/temurin/releases/?version=21
```

### 验证安装

```bash
java -version
# 应输出: openjdk version "21.0.x"
```

---

## 🐳 第二步：启动依赖服务（Docker）

创建 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  # Nacos注册配置中心
  nacos:
    image: nacos/nacos-server:v2.3.0
    container_name: nacos
    environment:
      MODE: standalone
    ports:
      - "8848:8848"
      - "9848:9848"

  # Redis (含Vector Search模块)
  redis:
    image: redis/redis-stack:latest
    container_name: redis
    ports:
      - "6379:6379"
      - "8001:8001"

  # MySQL 8.0
  mysql:
    image: mysql:8.0
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: your_password
      MYSQL_DATABASE: ai_mall
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  # Elasticsearch
  elasticsearch:
    image: elasticsearch:8.11.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  # RocketMQ
  rocketmq:
    image: apache/rocketmq:5.1.4
    container_name: rocketmq
    ports:
      - "9876:9876"

volumes:
  mysql_data:
```

### 启动所有服务

```bash
docker-compose up -d
```

### 验证服务状态

```bash
docker-compose ps
# 所有服务状态应为 "Up"
```

---

## ⚙️ 第三步：配置API Key

### 获取通义千问API Key

1. 访问 [阿里云DashScope](https://dashscope.aliyun.com/)
2. 注册/登录账号
3. 创建API Key
4. 复制Key到配置文件

### 编辑配置文件

打开 `src/main/resources/application.yml`，修改：

```yaml
spring:
  ai:
    openai:
      api-key: sk-your-actual-api-key  # 替换为你的真实Key
```

---

## 🏗️ 第四步：编译项目

```bash
# 进入项目目录
cd D:\python\java_ai

# Maven编译 (跳过测试)
mvn clean package -DskipTests
```

**首次编译可能需要3-5分钟下载依赖**

---

## ▶️ 第五步：启动应用

```bash
# JDK 21启动 (启用虚拟线程)
java --enable-preview -jar target/ai-native-mall-1.0.0.jar
```

### 成功标志

看到以下输出表示启动成功：

```
========================================
  AI-Native Smart Mall 启动成功！
  
  技术栈:
  - Spring Boot 3.2 + JDK 21
  - Spring AI + LangChain4j
  - Nacos + Sentinel + Gateway
  - Redis Vector + Elasticsearch
  
  API文档:
  - 智能客服: POST /api/ai/customer-service/ask
  - 流式对话: GET /api/ai/customer-service/stream
  - SEO标题: POST /api/ai/product/seo-title
  - 商品描述: POST /api/ai/product/description
  - 评论分析: POST /api/ai/comment/analyze
  ========================================
```

---

## 🧪 第六步：测试API

### 测试1: 健康检查

```bash
curl http://localhost:8080/actuator/health
```

预期响应:
```json
{
  "status": "UP"
}
```

---

### 测试2: 智能客服问答

```bash
curl -X POST http://localhost:8080/api/ai/customer-service/ask \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user",
    "question": "iPhone 15 Pro有什么特点？",
    "sessionId": "test_session",
    "apiKey": "sk-test-key"
  }'
```

---

### 测试3: 生成SEO标题

```bash
curl -X POST http://localhost:8080/api/ai/product/seo-title \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "iPhone 15 Pro",
    "category": "手机通讯",
    "features": "A17 Pro芯片,钛金属,4800万像素"
  }'
```

---

## 🔍 第七步：查看监控

### Nacos控制台

访问: http://localhost:8848/nacos  
账号/密码: nacos/nacos

查看服务注册情况。

---

### Actuator监控端点

```bash
# 查看应用信息
curl http://localhost:8080/actuator/info

# 查看指标
curl http://localhost:8080/actuator/metrics
```

---

## ❌ 常见问题排查

### 问题1: 端口被占用

**错误**: `Port 8080 was already in use`

**解决**:
```bash
# Windows查看占用端口的进程
netstat -ano | findstr :8080

# 杀死进程
taskkill /F /PID <PID>
```

---

### 问题2: Docker容器启动失败

**错误**: `Cannot start service`

**解决**:
```bash
# 查看日志
docker-compose logs <service_name>

# 重启服务
docker-compose restart <service_name>
```

---

### 问题3: API Key无效

**错误**: `Invalid API Key`

**解决**:
1. 确认API Key格式: `sk-xxxxxxxx`
2. 检查是否已充值余额
3. 验证API Key权限

---

### 问题4: Maven依赖下载慢

**解决**: 配置阿里云镜像

编辑 `~/.m2/settings.xml`:

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

## 📊 性能验证

### 压测智能客服接口

使用Apache Bench:

```bash
# 100并发，总共1000次请求
ab -n 1000 -c 100 -p request.json -T application/json \
   http://localhost:8080/api/ai/customer-service/ask
```

**预期结果**:
- QPS > 100
- 平均响应时间 < 200ms
- 成功率 > 99%

---

## ✅ 验收清单

完成以下检查项即表示部署成功：

- [ ] JDK 21安装成功 (`java -version` 输出21.x)
- [ ] 所有Docker容器运行正常 (`docker-compose ps`)
- [ ] Nacos控制台可访问 (http://localhost:8848/nacos)
- [ ] 应用启动无报错 (看到"启动成功"提示)
- [ ] 健康检查返回 `{"status":"UP"}`
- [ ] 智能客服接口返回正确响应
- [ ] SEO标题生成功能正常
- [ ] 数据脱敏工具类工作正常
- [ ] 虚拟线程已启用 (日志中可见)

---

## 🎯 下一步

1. **阅读完整架构文档**: 查看 `ARCHITECTURE.md`
2. **前端开发**: 基于Vue 3 + Uniapp开发多端界面
3. **业务扩展**: 添加订单、支付等核心业务模块
4. **K8s部署**: 生产环境使用Kubernetes编排
5. **监控告警**: 配置Prometheus + Grafana监控

---

**技术支持**: AI-Native Mall Team  
**文档版本**: v1.0.0  
**最后更新**: 2026-04-09
