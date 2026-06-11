#!/bin/bash
# Locust performance/stress test runner
# Usage: ./run-locust.sh [scenario]
# Scenarios: light (100 users), medium (500 users), stress (1000 users)
set -euo pipefail

SCENARIO="${1:-medium}"
REPORT_DIR="build/reports/performance"
FAILURES=0

case "$SCENARIO" in
  light)
    USERS_PER_SERVICE=25
    SPAWN_RATE=5
    RUN_TIME=120s
    P95_THRESHOLD=500 ;;
  medium)
    USERS_PER_SERVICE=125
    SPAWN_RATE=20
    RUN_TIME=180s
    P95_THRESHOLD=500 ;;
  stress)
    USERS_PER_SERVICE=250
    SPAWN_RATE=50
    RUN_TIME=300s
    P95_THRESHOLD=1000 ;;
  *)
    echo "Usage: $0 {light|medium|stress}"
    exit 1 ;;
esac

mkdir -p "$REPORT_DIR"
cd "$(dirname "$0")/locust"
pip install -q -r requirements.txt

echo "=== Starting $SCENARIO test: $((USERS_PER_SERVICE * 4)) total users ==="

run_locust() {
  local name="$1"
  local port="$2"
  local users="$3"
  local out="$REPORT_DIR/${name}"
  mkdir -p "$out"

  locust -f locustfile.py \
    --headless \
    --web-port "$((8089 + RANDOM % 1000))" \
    --host "http://localhost:${port}" \
    --users "$users" \
    --spawn-rate "$SPAWN_RATE" \
    --run-time "$RUN_TIME" \
    --html "$out/report.html" \
    --csv "$out/results" \
    --only-summary \
    --tags "$name" 2>&1 | tee "$out/locust.log"

  # Parse results for threshold validation
  local p95
  p95=$(grep -oP '95th percentile\s*:\s*\K[0-9.]+' "$out/locust.log" || echo "9999")
  local failures
  failures=$(grep -oP 'Failures\s*:\s*\K[0-9]+' "$out/locust.log" || echo "9999")
  local total_reqs
  total_reqs=$(grep -oP 'Total requests\s*:\s*\K[0-9]+' "$out/locust.log" || echo "0")

  local error_rate=0
  if [ "$total_reqs" -gt 0 ] 2>/dev/null; then
    error_rate=$((failures * 100 / total_reqs))
  fi

  echo "$name: p95=${p95}ms, errors=${failures}/${total_reqs} (${error_rate}%)"

  # CA3: Validate thresholds
  local svc_fail=0
  if [ "$(echo "$p95 > $P95_THRESHOLD" | bc -l 2>/dev/null || echo 1)" -eq 1 ]; then
    echo "FAIL [$name] p95 latency ${p95}ms exceeds threshold ${P95_THRESHOLD}ms"
    svc_fail=1
  fi
  if [ "$error_rate" -gt 1 ] 2>/dev/null; then
    echo "FAIL [$name] error rate ${error_rate}% exceeds 1%"
    svc_fail=1
  fi
  if [ "$svc_fail" -eq 1 ]; then
    FAILURES=$((FAILURES + 1))
  fi
  return "$svc_fail"
}

# Run 4 parallel Locust processes, one per service
run_locust "gateway-service" "8087" "$USERS_PER_SERVICE" &
PID1=$!
run_locust "form-service" "8086" "$USERS_PER_SERVICE" &
PID2=$!
run_locust "dashboard-service" "8084" "$USERS_PER_SERVICE" &
PID3=$!
run_locust "identity-service" "8083" "$USERS_PER_SERVICE" &
PID4=$!

# Wait for all and collect exit codes
wait $PID1 || true
wait $PID2 || true
wait $PID3 || true
wait $PID4 || true

# Generate combined summary
echo "" | tee "$REPORT_DIR/summary.txt"
echo "============================================" | tee -a "$REPORT_DIR/summary.txt"
echo " CircleGuard Performance Test - $SCENARIO" | tee -a "$REPORT_DIR/summary.txt"
echo "============================================" | tee -a "$REPORT_DIR/summary.txt"
for svc in "gateway-service" "form-service" "dashboard-service" "identity-service"; do
  echo "" | tee -a "$REPORT_DIR/summary.txt"
  echo "--- $svc ---" | tee -a "$REPORT_DIR/summary.txt"
  tail -5 "$REPORT_DIR/$svc/locust.log" 2>/dev/null | tee -a "$REPORT_DIR/summary.txt"
done

echo "" | tee -a "$REPORT_DIR/summary.txt"
echo "Total failures: $FAILURES service(s) exceeded thresholds" | tee -a "$REPORT_DIR/summary.txt"

exit "$FAILURES"
