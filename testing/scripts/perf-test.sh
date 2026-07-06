#!/bin/bash
# 性能测试 — 并发压测 + 响应时间统计 + 资源监控
# 使用: bash testing/scripts/perf-test.sh

BASE="http://localhost:8081/api/v1"
PASS=0
FAIL=0
TOTAL_TIME=0
MAX_TIME=0
RESULTS_FILE=$(mktemp)

# 获取 token
login() {
  curl -s -X POST "$BASE/user/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"'"${1:-admin}"'","password":"'"${2:-admin123}"'"}'
}
TOKEN=$(login | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "❌ 登录失败，无法获取 token"
  exit 1
fi

# ==================== 单接口压测 ====================
benchmark() {
  local name="$1"
  local method="$2"
  local url="$3"
  local data="$4"
  local concurrency="${5:-10}"
  local rounds="${6:-100}"

  echo ""
  echo "========================================"
  echo " $name"
  echo " 并发: $concurrency | 总请求: $rounds"
  echo "========================================"

  local start=$(date +%s%N)

  # 并发执行
  for i in $(seq 1 $rounds); do
    (
      req_start=$(date +%s%N)
      if [ "$method" = "POST" ]; then
        http_code=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
          -X POST "$BASE$url" \
          -H "Authorization: $TOKEN" \
          -H 'Content-Type: application/json' \
          -d "$data" 2>/dev/null)
      else
        http_code=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
          "$BASE$url" \
          -H "Authorization: $TOKEN" 2>/dev/null)
      fi
      echo "$http_code" >> "$RESULTS_FILE.$i"
    ) &
    # 控制并发数
    if [ $((i % concurrency)) -eq 0 ]; then
      wait
    fi
  done
  wait

  # 统计结果
  local codes=$(cat "$RESULTS_FILE".* 2>/dev/null | grep -E '^(200|400|403|500)' | sort | uniq -c | sort -rn)
  local times=$(cat "$RESULTS_FILE".* 2>/dev/null | grep -E '^[0-9]' | sort -n)

  local total_requests=$(echo "$times" | wc -l)
  local avg_time=$(echo "$times" | awk '{sum+=$1; count++} END {if(count>0) printf "%.0f", sum/count*1000; else print "0"}')
  local max_time=$(echo "$times" | tail -1)
  local min_time=$(echo "$times" | head -1)
  local p50=$(echo "$times" | awk 'NR==int(NR*0.5){print $1}')
  local p95=$(echo "$times" | awk 'NR==int(NR*0.95){print $1}')
  local p99=$(echo "$times" | awk 'NR==int(NR*0.99){print $1}')
  local end=$(date +%s%N)
  local elapsed=$(( (end - start) / 1000000 ))

  echo " HTTP状态码分布:"
  echo "$codes" | while read count code; do
    echo "   $code: $count 次"
  done
  echo " 总请求: $total_requests | 耗时: ${elapsed}ms"
  echo " 响应时间 (ms):"
  [ -n "$min_time" ] && echo "   min=${min_time}s p50=${p50}s avg=${avg_time}ms p95=${p95}s p99=${p99}s max=${max_time}s"
  echo " QPS: $(echo "scale=1; $total_requests * 1000 / $elapsed" | bc 2>/dev/null || echo "N/A")"

  rm -f "$RESULTS_FILE".*
}

# ==================== 1. 商品列表 ====================
benchmark "商品列表 GET /product/list" "GET" "/product/list?pageNum=1&pageSize=20" "" 10 200

# ==================== 2. 商品详情 ====================
benchmark "商品详情 GET /product/1" "GET" "/product/1" "" 20 500

# ==================== 3. 登录 ====================
benchmark "登录 POST /user/login" "POST" "/user/login" '{"username":"admin","password":"admin123"}' 5 50

# ==================== 4. 下单 ====================
benchmark "下单 POST /order" "POST" "/order" '{"items":[{"productId":1,"price":29.90,"quantity":1,"productName":"test"}]}' 5 50

# ==================== 5. 搜索 ====================
benchmark "搜索 GET /search?keyword=test" "GET" "/search?keyword=test" "" 10 100

echo ""
echo "========================================"
echo " 压测完成"
echo "========================================"

# ==================== Actuator 健康检查 ====================
echo ""
echo "=== 容器资源 ==="
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" ai-mall-app mysql-ai-mall redis-ai-mall 2>/dev/null

echo ""
echo "=== Sentinel 流控状态 ==="
curl -s "$BASE/actuator/health" | python3 -c "import sys,json; print(json.load(sys.stdin))" 2>/dev/null
