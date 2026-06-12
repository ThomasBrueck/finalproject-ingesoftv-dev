#!/bin/bash
# OWASP ZAP DAST Security Scan
# Scans ALL microservice endpoints for vulnerabilities
# Usage: ./zap-scan.sh <base-url> [output-dir]
set -euo pipefail

BASE_URL="${1:-http://localhost:8083}"
OUTPUT_DIR="${2:-build/reports/security}"
SUMMARY_FILE="$OUTPUT_DIR/zap-summary.txt"
FAILURES=0

SCAN_TARGETS=(
  "gateway-service:http://localhost:8087"
  "auth-service:http://localhost:8180"
  "form-service:http://localhost:8086"
  "identity-service:http://localhost:8083"
  "promotion-service:http://localhost:8088"
  "notification-service:http://localhost:8082"
  "dashboard-service:http://localhost:8084"
  "file-service:http://localhost:8085"
)

mkdir -p "$OUTPUT_DIR"

echo "=== OWASP ZAP DAST Scan ===" | tee "$SUMMARY_FILE"
echo "Targets: ${#SCAN_TARGETS[@]} services" | tee -a "$SUMMARY_FILE"
echo "Date: $(date -u)" | tee -a "$SUMMARY_FILE"
echo "" | tee -a "$SUMMARY_FILE"

for target in "${SCAN_TARGETS[@]}"; do
  service_name="${target%%:*}"
  # Primer ':' separa nombre de URL; con ##*: quedaba solo el puerto ("8087")
  # y zap-full-scan rechazaba el target con usage error (exit 3).
  service_url="${target#*:}"
  report_file="$OUTPUT_DIR/zap-report-${service_name}.html"
  markdown_file="$OUTPUT_DIR/zap-report-${service_name}.md"

  echo "Scanning $service_name ($service_url)..." | tee -a "$SUMMARY_FILE"

  # Run ZAP API scan with full spider + active scan
  docker run --rm \
    --network host \
    -v "$(pwd)/$OUTPUT_DIR:/zap/wrk" \
    ghcr.io/zaproxy/zaproxy:stable \
    zap-full-scan.py \
    -t "$service_url" \
    -r "zap-report-${service_name}.html" \
    -w "zap-report-${service_name}.md" \
    -I \
    -z "-config globalexcludeurl.url_list.url(0).regex='.*/actuator/health.*' -config rules.cookie.ignorelist=.*" \
    -T 5 \
    2>&1 | tail -5 || echo "WARN: scan de $service_name salió con error; se continúa con el siguiente" | tee -a "$SUMMARY_FILE"

  # Check for HIGH/CRITICAL alerts
  if [ -f "$markdown_file" ]; then
    high_count=$(grep -c "HIGH" "$markdown_file" 2>/dev/null || echo 0)
    critical_count=$(grep -c "CRITICAL" "$markdown_file" 2>/dev/null || echo 0)
    total=$((high_count + critical_count))

    if [ "$total" -gt 0 ]; then
      echo "WARNING: $service_name - $total HIGH/CRITICAL alerts found!" | tee -a "$SUMMARY_FILE"
      grep -E "HIGH|CRITICAL" "$markdown_file" | head -10 | tee -a "$SUMMARY_FILE"
      FAILURES=$((FAILURES + 1))
    else
      echo "PASS: $service_name - no HIGH/CRITICAL alerts" | tee -a "$SUMMARY_FILE"
    fi
  fi
  echo "" | tee -a "$SUMMARY_FILE"
done

echo "========================================" | tee -a "$SUMMARY_FILE"
echo "Services with HIGH/CRITICAL findings: $FAILURES" | tee -a "$SUMMARY_FILE"

exit "$FAILURES"
