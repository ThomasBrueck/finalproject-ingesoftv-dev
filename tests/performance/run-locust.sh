#!/bin/bash
# Run Locust performance tests in headless mode
# Usage: ./run-locust.sh [host] [users] [spawn-rate] [run-time]

HOST="${1:-http://localhost}"
USERS="${2:-10}"
SPAWN_RATE="${3:-2}"
RUN_TIME="${4:-60s}"
REPORT_DIR="build/reports/performance"

mkdir -p "$REPORT_DIR"

cd "$(dirname "$0")/locust"

pip install -q -r requirements.txt

locust --host="$HOST" \
       --headless \
       --users "$USERS" \
       --spawn-rate "$SPAWN_RATE" \
       --run-time "$RUN_TIME" \
       --html="$REPORT_DIR/report.html" \
       --csv="$REPORT_DIR/results" \
       --only-summary

echo "=== Performance Test Results ==="
echo "Report: $REPORT_DIR/report.html"
echo "CSV:    $REPORT_DIR/results_stats.csv"
