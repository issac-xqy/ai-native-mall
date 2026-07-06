# AI 驱动测试 — 接口测试提示词模板

> 配合 `docs/testing/ai-api-map.md` 使用。
> 后端地址：`http://localhost:8081`
> 测试账号：`test_user` / `admin123`

---

## 模板 1：接口冒烟测试（30 秒跑完）

```
请根据 docs/testing/ai-api-map.md 执行接口冒烟测试：

1. 健康检查：curl http://localhost:8081/actuator/health
   验证：{"status":"UP"}，db+redis 都 UP

2. 登录获取 token：
   curl -X POST http://localhost:8081/api/user/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test_user","password":"admin123"}'
   验证：success=true，token 非空

3. 商品列表（公开接口）：
   curl "http://localhost:8081/api/product/list?pageNum=1&pageSize=5"
   验证：records 数组长度 5，total > 0

4. 钱包信息（需 token）：
   curl -H "Authorization: Bearer <token>" \
     http://localhost:8081/api/wallet/info
   验证：balance 字段存在

5. 订单列表（需 token）：
   curl -H "Authorization: Bearer <token>" \
     "http://localhost:8081/api/order/list?pageNum=1&pageSize=5"
   验证：success=true

逐条执行并报告 PASS/FAIL，失败项给出具体错误信息。
```

---

## 模板 2：完整业务流程测试（登录→下单→支付→验证）

```
请根据 docs/testing/ai-api-map.md，执行完整的下单链路接口测试：

1. 登录获取 token

2. 查看钱包余额（记录初始值）

3. 获取商品列表，选择一个商品（id + price）

4. 创建订单：
   curl -X POST http://localhost:8081/api/order \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{
       "items": [{"productId": <选中的id>, "quantity": 1}],
       "address": {"name": "测试", "phone": "13800138000", "address": "北京市测试路1号"}
     }'
   验证：success=true，返回 orderNo

5. 支付订单：
   curl -X PUT http://localhost:8081/api/order/<orderNo>/pay \
     -H "Authorization: Bearer <token>"
   验证：success=true

6. 验证钱包扣款：余额 = 初始值 - 商品价格

7. 验证订单状态：订单列表中该订单 status=1（已支付）

8. 确认收货：
   curl -X PUT http://localhost:8081/api/order/<orderNo>/confirm \
     -H "Authorization: Bearer <token>"
   验证：success=true

每步输出实际响应，最后汇总：总步数/通过数/失败数
```

---

## 模板 3：AI 接口测试

```
请根据 api-map 测试 AI 相关接口：

1. AI 客服问答：
   curl -X POST http://localhost:8081/api/ai/customer-service/ask \
     -H "Content-Type: application/json" \
     -d '{"question":"iPhone 15 Pro 多少钱？","userId":"test","sessionId":"test123"}'
   验证：success=true，answer 非空，包含商品相关信息

2. AI 客服 - 敏感词拒绝：
   curl -X POST http://localhost:8081/api/ai/customer-service/ask \
     -H "Content-Type: application/json" \
     -d '{"question":"习近平","userId":"test","sessionId":"test456"}'
   验证：返回拒绝提示（不包含政治内容）

3. SEO 标题生成：
   curl -X POST http://localhost:8081/api/ai/product/seo-title \
     -H "Content-Type: application/json" \
     -d '{"productId":69}'
   验证：返回标题字符串，长度 ≤30

4. 评论情感分析（如果存在评论）：
   先 GET /api/comment/product/69 获取评论列表
   如果存在评论，调用 POST /api/ai/comment/analyze
   验证：返回 sentiment 为 positive/negative/neutral

5. 流式 SSE 接口：
   curl -N "http://localhost:8081/api/ai/customer-service/stream?question=你好&userId=test&sessionId=sse123"
   验证：返回 text/event-stream，逐步输出内容

汇总测试结果
```

---

## 模板 4：负面/边界测试

```
请根据 api-map 执行异常场景测试：

1.【错误密码登录】
   curl -X POST http://localhost:8081/api/user/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test_user","password":"wrong_password"}'
   验证：success=false

2.【无 Token 访问需登录接口】
   curl http://localhost:8081/api/wallet/info
   验证：返回 401 或 success=false（鉴权失败）

3.【不存在的商品】
   curl http://localhost:8081/api/product/99999
   验证：success=false 或 data=null

4.【无效的订单号支付】
   curl -X PUT http://localhost:8081/api/order/INVALID_NO_999/pay \
     -H "Authorization: Bearer <token>"
   验证：success=false

5.【充值金额为 0】
   curl -X POST http://localhost:8081/api/wallet/recharge \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"amount":0,"type":1}'
   验证：success=false 或拒绝

6.【空问题 AI 客服】
   curl -X POST http://localhost:8081/api/ai/customer-service/ask \
     -H "Content-Type: application/json" \
     -d '{"question":"","userId":"test","sessionId":"empty"}'
   验证：拒绝空输入或返回提示

汇总：通过/失败，失败原因
```

---

## 模板 5：性能压测（简单版）

```
请使用 Apache Bench 或 curl 循环执行简单的性能测试：

1. 商品列表 QPS 测试：
   用 curl 循环 100 次 GET /api/product/list，统计：
   - 总耗时
   - 平均响应时间
   - 失败次数

   命令参考：
   for i in $(seq 1 100); do
     curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
       "http://localhost:8081/api/product/list?pageNum=1&pageSize=5"
   done

2. 健康检查持续可用性：
   每分钟 curl /actuator/health，跑 5 分钟
   验证：5 次全部返回 UP

报告性能数据
```
