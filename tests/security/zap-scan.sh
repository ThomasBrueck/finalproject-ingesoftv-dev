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
chmod 777 "$OUTPUT_DIR"

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

  # ZAP corre como uid 1000 dentro del contenedor; el volumen montado debe ser
  # escribible para que pueda dejar los reportes (si no, no se genera el .md y
  # el análisis queda vacío).
  chmod -R 777 "$OUTPUT_DIR" 2>/dev/null || true

  # Resiliencia de port-forward: los full-scan son largos y, al escanear 8
  # servicios en serie, el túnel kubectl de los últimos (dashboard/file) queda
  # ocioso varios minutos y se cae → ZAP no alcanza el target y sale con rc=3.
  # Si el target no responde la conexión y hay kubectl (CI), reiniciamos su
  # port-forward y esperamos a que vuelva a estar disponible.
  port="${service_url##*:}"
  k8s_svc="circleguard-${service_name}"
  ns="${ZAP_NAMESPACE:-stage}"
  if ! curl -s -o /dev/null --max-time 5 "http://localhost:${port}/actuator/health" 2>/dev/null; then
    if command -v kubectl >/dev/null 2>&1; then
      echo "  target no alcanzable, reiniciando port-forward de ${k8s_svc}..." | tee -a "$SUMMARY_FILE"
      pkill -f "port-forward -n ${ns} service/${k8s_svc} " 2>/dev/null || true
      sleep 1
      kubectl port-forward -n "${ns}" "service/${k8s_svc}" "${port}:80" >"/tmp/zap-pf-${service_name}.log" 2>&1 &
    fi
  fi
  # Espera hasta 60s a que el target responda 200 antes de escanear.
  for _ in $(seq 1 20); do
    rc_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://localhost:${port}/actuator/health" 2>/dev/null || echo 000)
    [ "$rc_code" = "200" ] && break
    sleep 3
  done

  # zap-full-scan.py: spider + escaneo activo. -I = informativo (no aborta el
  # contenedor por warnings). Capturamos el código de salida sin que set -e mate
  # el script (las reglas en estado WARN/FAIL hacen exit 1/2, que es esperado).
  scan_rc=0
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
    > "$OUTPUT_DIR/zap-${service_name}.log" 2>&1 || scan_rc=$?

  if [ ! -f "$markdown_file" ]; then
    echo "ERROR: $service_name - no se generó el reporte (rc=$scan_rc). Ver zap-${service_name}.log" | tee -a "$SUMMARY_FILE"
    FAILURES=$((FAILURES + 1))
    echo "" | tee -a "$SUMMARY_FILE"
    continue
  fi

  # Conteo de alertas por nivel desde la tabla "Summary of Alerts" del reporte
  # markdown de ZAP (filas tipo "| High | N |"). Se extrae el primer entero de
  # cada fila; si no aparece, se asume 0. Evita el bug de 'grep -c || echo 0'
  # (que producía "0\n0" y rompía la aritmética con set -e).
  level_count() { grep -iE "^\| *$1 *\|" "$markdown_file" | grep -oE '[0-9]+' | head -1; }
  high_count=$(level_count "High"); high_count=${high_count:-0}
  medium_count=$(level_count "Medium"); medium_count=${medium_count:-0}
  low_count=$(level_count "Low"); low_count=${low_count:-0}

  echo "  Alertas → High: $high_count | Medium: $medium_count | Low: $low_count" | tee -a "$SUMMARY_FILE"

  if [ "$high_count" -gt 0 ]; then
    echo "WARNING: $service_name - $high_count alerta(s) HIGH encontradas" | tee -a "$SUMMARY_FILE"
    FAILURES=$((FAILURES + 1))
  else
    echo "PASS: $service_name - sin alertas HIGH" | tee -a "$SUMMARY_FILE"
  fi
  echo "" | tee -a "$SUMMARY_FILE"
done

echo "========================================" | tee -a "$SUMMARY_FILE"
echo "Servicios con alertas HIGH (o sin reporte): $FAILURES de ${#SCAN_TARGETS[@]}" | tee -a "$SUMMARY_FILE"

exit "$FAILURES"
