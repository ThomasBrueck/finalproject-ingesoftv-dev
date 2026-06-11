#!/bin/bash
# OWASP ZAP API Security Scan
# Usage: ./zap-scan.sh <target-url> [output-dir]

TARGET_URL="${1:-http://localhost:8083}"
OUTPUT_DIR="${2:-build/reports/security}"
REPORT_FILE="$OUTPUT_DIR/zap-report.html"

mkdir -p "$OUTPUT_DIR"

docker run --rm \
  --network host \
  -v "$(pwd)/$OUTPUT_DIR:/zap/wrk" \
  softwaresecurityprojects/zap-stable \
  zap-api-scan.py \
  -t "$TARGET_URL/v3/api-docs" \
  -f openapi \
  -r zap-report.html \
  -w zap-report.md \
  -I \
  -z "-config globalexcludeurl.url_list.url\(0\).regex='.*/actuator/health.*'" \
  || true

echo "=== ZAP Scan Complete ==="
echo "HTML Report: $REPORT_FILE"
if grep -q "HIGH\|CRITICAL" "$OUTPUT_DIR/zap-report.md" 2>/dev/null; then
  echo "WARNING: HIGH or CRITICAL alerts found in ZAP scan!"
  grep "HIGH\|CRITICAL" "$OUTPUT_DIR/zap-report.md"
fi
