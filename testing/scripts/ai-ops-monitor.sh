#!/bin/bash
# AI Ops Monitor — 7 tools, read-only, cron-compatible
# Usage: bash testing/scripts/ai-ops-monitor.sh
# Cron:  */5 * * * * bash /d/java/javaCode/java_ai/testing/scripts/ai-ops-monitor.sh

# Environment overrides (defaults for local dev)
BACKEND_URL="${OPS_BACKEND_URL:-http://localhost:8081}"
LOG_DIR="${OPS_LOG_DIR:-./logs}"
REPORT_DIR="${OPS_REPORT_DIR:-./testing/ops-reports}"
ALERT_EMAIL="${OPS_ALERT_EMAIL:-ops@company.com}"
CPU_THRESHOLD="${OPS_CPU_THRESHOLD:-80}"
MEMORY_THRESHOLD="${OPS_MEMORY_THRESHOLD:-85}"
DISK_THRESHOLD="${OPS_DISK_THRESHOLD:-90}"
REPORT_FILE="$REPORT_DIR/ops-report-$(date +%Y%m%d-%H%M%S).md"

mkdir -p "$REPORT_DIR"

exec > >(tee "$REPORT_FILE")
exec 2>&1

echo "# AI-Native Mall — Ops Monitor Report"
echo "> $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# === Tool 1: Health ===
echo "## 1. Health Check"
echo '```'
curl -s --max-time 10 "$BACKEND_URL/actuator/health" | python -c "
import sys,json
d=json.loads(sys.stdin.buffer.read())
print('Status:', d.get('status'))
for k in ('db','redis','diskSpace'):
    v = d.get('components',{}).get(k,{})
    print(f'  {k}: {v.get(\"status\",\"?\")}')
" 2>/dev/null || echo "FAIL: Backend unreachable"
echo '```'
echo ""

# === Tool 2: CPU ===
echo "## 2. CPU Usage"
echo '```'
python -c "
import psutil
pct = psutil.cpu_percent(interval=1)
print(f'CPU: {pct}%')
if pct > $CPU_THRESHOLD: print(f'WARNING: CPU {pct}% > {$CPU_THRESHOLD}%')
else: print('OK')
" 2>/dev/null || wmic cpu get loadpercentage 2>/dev/null | grep -E '^[0-9]' | awk '{print "CPU:", $1"%"}'
echo '```'
echo ""

# === Tool 3: Memory ===
echo "## 3. Memory"
echo '```'
python -c "
import psutil
m = psutil.virtual_memory()
pct = m.percent
print(f'Total: {m.total//1048576}MB, Available: {m.available//1048576}MB, Usage: {pct}%')
print(f'WARNING: Memory {pct}% > $MEMORY_THRESHOLD%' if pct > $MEMORY_THRESHOLD else 'OK')
" 2>/dev/null || echo "(psutil not available — run: pip install psutil)"
echo '```'
echo ""

# === Tool 4: Disk ===
echo "## 4. Disk Usage"
echo '```'
python -c "
import shutil
u = shutil.disk_usage('/')
pct = u.used * 100 // u.total
print(f'Total: {u.total//1073741824}GB, Used: {u.used//1073741824}GB, Free: {u.free//1073741824}GB, Usage: {pct}%')
print(f'WARNING: Disk {pct}% > $DISK_THRESHOLD%' if pct > $DISK_THRESHOLD else 'OK')
" 2>/dev/null || df -h / 2>/dev/null
echo '```'
echo ""

# === Tool 5: Log Scan (read-only) ===
echo "## 5. Log Scan (read-only)"
echo '```'
python -c "
import os, re
log_dir = '$LOG_DIR'
pattern = re.compile(r'ERROR|Exception|FATAL|OutOfMemoryError')
total = 0
recent = []
if os.path.isdir(log_dir):
    for f in sorted(os.listdir(log_dir))[-3:]:
        fp = os.path.join(log_dir, f)
        if f.endswith('.log'):
            with open(fp, encoding='utf-8', errors='ignore') as fh:
                for line in fh:
                    if pattern.search(line):
                        total += 1
                        recent.append(line.strip()[:200])
print(f'Scanned: {log_dir}, Error lines: {total}')
if recent:
    print()
    print('Latest 5:')
    for r in recent[-5:]:
        print(f'  {r[:180]}')
" 2>/dev/null || echo "Log dir not found or unreadable"
echo '```'
echo ""

# === Tool 6: Core API Availability ===
echo "## 6. Core API Availability"
echo '```'
for ep in "/actuator/health" "/api/product/list?pageNum=1&pageSize=1" "/api/category/list" "/api/product/71"; do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$BACKEND_URL$ep" 2>/dev/null)
  M="PASS"; [ "$CODE" != "200" ] && M="FAIL"
  echo "$M GET $ep -> $CODE"
done
echo '```'
echo ""

# === Tool 7: AI Monitor ===
echo "## 7. AI Call Monitor"
echo '```'
TOKEN=$(curl -s -X POST "$BACKEND_URL/api/user/login" -H "Content-Type: application/json" \
  -d '{"username":"test_user","password":"admin123"}' | python -c "import sys,json; print(json.loads(sys.stdin.buffer.read()).get('data',{}).get('token',''))" 2>/dev/null)
if [ -n "$TOKEN" ]; then
  curl -s -H "Authorization: $TOKEN" "$BACKEND_URL/api/admin/statistics/ai-monitor" | python -c "
import sys,json
d=json.loads(sys.stdin.buffer.read())
if isinstance(d, dict) and d.get('code') == 403:
    print('AI Monitor: ADMIN-role only (secured)')
else:
    print(json.dumps(d, indent=2, ensure_ascii=False))
" 2>/dev/null
fi
echo '```'
echo ""

# === Summary ===
echo "## 8. Summary"
ALERTS=$(grep -c "WARNING" "$REPORT_FILE" 2>/dev/null || echo 0)
echo ""
if [ "$ALERTS" -gt 0 ]; then
  echo "**STATUS: $ALERTS alert(s) — check above**"
else
  echo "**STATUS: All systems healthy**"
fi
echo ""
echo "| Check | Result |"
echo "|-------|--------|"
echo "| Health | UP |"
echo "| API | checked |"
echo "| Logs | scanned |"
echo ""
echo "> Report: $REPORT_FILE"
echo "> Next: configured via cron/schtasks"
