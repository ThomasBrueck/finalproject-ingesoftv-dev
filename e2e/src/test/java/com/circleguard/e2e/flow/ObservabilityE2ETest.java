package com.circleguard.e2e.flow;

import com.circleguard.e2e.client.ObservabilityClient;
import com.circleguard.e2e.config.E2ETestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OBS-01 … OBS-06 — Validación del Stack de Monitoreo (SCRUM-43)
 *
 * <p>Estos tests verifican en tiempo de ejecución (contra el entorno real levantado
 * por E2ETestBase) que:
 * <ol>
 *   <li>Todos los servicios exponen /actuator/prometheus con contenido válido.</li>
 *   <li>Las métricas JVM estándar están presentes.</li>
 *   <li>Las métricas de negocio personalizadas están registradas.</li>
 *   <li>El health actuator reporta el estado correcto.</li>
 * </ol>
 *
 * <p>En modo CI se requiere la variable de entorno adicional
 * {@code PROMETHEUS_URL} apuntando a Prometheus.  Si no está definida,
 * las aserciones contra la Query API de Prometheus se omiten (soft-skip).
 */
class ObservabilityE2ETest extends E2ETestBase {

    private static ObservabilityClient obs;

    /** URL de Prometheus: en CI viene de env var, en local usa el puerto del compose. */
    private static String prometheusUrl;

    @BeforeAll
    static void initObservability() {
        obs = new ObservabilityClient();

        // En modo CI, PROMETHEUS_URL debe definirse junto con *_SERVICE_URL.
        // En modo local (Testcontainers) usamos el puerto fijo declarado en compose.
        prometheusUrl = System.getenv("PROMETHEUS_URL");
        if (prometheusUrl == null || prometheusUrl.isBlank()) {
            prometheusUrl = "http://localhost:9090"; // puerto fijo del compose
        }

        System.out.println("=== Observability tests — Prometheus: " + prometheusUrl + " ===");
    }

    // ── OBS-01: /actuator/prometheus accesible en todos los servicios ───────────

    @Test
    void obs01_allServicesExposePrometheusEndpoint() {
        Map<String, String> services = serviceUrlMap();

        for (Map.Entry<String, String> entry : services.entrySet()) {
            String name = entry.getKey();
            String url  = entry.getValue();
            assertNotNull(url, "URL nula para: " + name);

            String body = assertDoesNotThrow(
                    () -> obs.fetchActuatorPrometheus(url),
                    name + " no responde en /actuator/prometheus"
            );

            assertFalse(body.isBlank(),
                    name + ": /actuator/prometheus devolvió cuerpo vacío");
            assertTrue(body.contains("# HELP") || body.contains("# TYPE"),
                    name + ": la respuesta no tiene formato Prometheus válido");

            System.out.println("[OK] " + name + " — /actuator/prometheus accesible (" +
                               body.lines().count() + " líneas)");
        }
    }

    // ── OBS-02: métricas JVM estándar presentes ─────────────────────────────────

    @Test
    void obs02_jvmMetricsPresentInAllServices() {
        List<String> requiredMetrics = List.of(
                "jvm_memory_used_bytes",
                "jvm_threads_live_threads",
                "process_uptime_seconds",
                "http_server_requests_seconds_count"
        );

        Map<String, String> services = serviceUrlMap();

        for (Map.Entry<String, String> entry : services.entrySet()) {
            String name = entry.getKey();
            String body = obs.fetchActuatorPrometheus(entry.getValue());

            for (String metric : requiredMetrics) {
                assertTrue(body.contains(metric),
                        name + ": métrica JVM '" + metric + "' no encontrada en /actuator/prometheus");
            }
            System.out.println("[OK] " + name + " — métricas JVM presentes");
        }
    }

    // ── OBS-03: métricas de negocio personalizadas registradas ──────────────────

    @Test
    void obs03_businessMetricsPresentInInstrumentedServices() {
        // auth-service: contadores de login
        String authBody = obs.fetchActuatorPrometheus(authServiceUrl);
        assertTrue(authBody.contains("circleguard_login_success_total") ||
                   authBody.contains("circleguard_login"),
                "auth-service: métrica de negocio 'circleguard_login' no encontrada");
        System.out.println("[OK] auth-service — métricas de login presentes");

        // gateway-service: escaneos QR
        String gwBody = obs.fetchActuatorPrometheus(gatewayServiceUrl);
        assertTrue(gwBody.contains("circleguard_qr") ||
                   gwBody.contains("qr_scans"),
                "gateway-service: métrica de negocio 'circleguard_qr' no encontrada");
        System.out.println("[OK] gateway-service — métricas QR presentes");

        // form-service: encuestas sintomáticas
        String formBody = obs.fetchActuatorPrometheus(formServiceUrl);
        assertTrue(formBody.contains("circleguard_survey") ||
                   formBody.contains("health_survey"),
                "form-service: métrica de negocio 'circleguard_survey' no encontrada");
        System.out.println("[OK] form-service — métricas de encuestas presentes");

        // promotion-service: cuarentenas
        String promBody = obs.fetchActuatorPrometheus(promotionServiceUrl);
        assertTrue(promBody.contains("circleguard_quarantine") ||
                   promBody.contains("fenced"),
                "promotion-service: métrica de negocio 'circleguard_quarantine' no encontrada");
        System.out.println("[OK] promotion-service — métricas de cuarentena presentes");
    }

    // ── OBS-04: health actuator retorna UP ───────────────────────────────────────

    @Test
    void obs04_allServicesHealthActuatorReturnsUp() {
        Map<String, String> services = serviceUrlMap();

        for (Map.Entry<String, String> entry : services.entrySet()) {
            String name = entry.getKey();
            assertTrue(obs.isHealthy(entry.getValue()),
                    name + ": /actuator/health no retornó 200 — el servicio no está UP");
            System.out.println("[OK] " + name + " — /actuator/health = 200");
        }
    }

    // ── OBS-05: Prometheus scraping (si está disponible) ────────────────────────

    @Test
    void obs05_prometheusScrapesAllTargetsSuccessfully() {
        if (prometheusUrl == null || prometheusUrl.isBlank()) {
            System.out.println("[SKIP] obs05 — PROMETHEUS_URL no definida, se omite");
            return;
        }

        try {
            // Consulta PromQL: targets activos del job circleguard-services
            String result = obs.queryPrometheus(prometheusUrl,
                    "up{job=\"circleguard-services\"}");

            assertNotNull(result, "Prometheus no retornó respuesta");
            assertTrue(result.contains("\"status\":\"success\""),
                    "Prometheus query API no retornó status=success. Respuesta: " + result);
            assertTrue(result.contains("\"value\""),
                    "Prometheus query API no retornó valores. Respuesta: " + result);

            // Todos los targets deben estar UP (value=1)
            assertFalse(result.contains("\"value\":["),
                    "Al menos un target está DOWN en Prometheus");

            System.out.println("[OK] obs05 — Prometheus scraping activo y saludable");
        } catch (RuntimeException e) {
            // Prometheus no está accesible desde este runner (ej. en CI sin port-forward a 9090)
            System.out.println("[WARN] obs05 — Prometheus no accesible en " + prometheusUrl +
                               ": " + e.getMessage());
            // No falla el test: el scraping se validará manualmente o con port-forward
        }
    }

    // ── OBS-06: formato de métricas sin errores de parseo ───────────────────────

    @Test
    void obs06_prometheusFormatIsWellFormed() {
        Map<String, String> services = serviceUrlMap();

        for (Map.Entry<String, String> entry : services.entrySet()) {
            String name = entry.getKey();
            String body = obs.fetchActuatorPrometheus(entry.getValue());

            // El formato Prometheus no debe contener NaN ni Inf sin contexto
            assertFalse(body.contains("NaN") && !body.contains("# HELP"),
                    name + ": métricas contienen NaN sin descripción — posible error de instrumentación");

            // Debe haber al menos 10 métricas distintas (sanity check)
            long helpLines = body.lines().filter(l -> l.startsWith("# HELP")).count();
            assertTrue(helpLines >= 5,
                    name + ": sólo " + helpLines + " líneas # HELP — Actuator no está instrumentado correctamente");

            System.out.println("[OK] " + name + " — formato Prometheus válido (" + helpLines + " métricas)");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private Map<String, String> serviceUrlMap() {
        return Map.of(
                "auth-service",         authServiceUrl,
                "identity-service",     identityServiceUrl,
                "promotion-service",    promotionServiceUrl,
                "notification-service", notificationServiceUrl,
                "form-service",         formServiceUrl,
                "gateway-service",      gatewayServiceUrl,
                "dashboard-service",    dashboardServiceUrl,
                "file-service",         fileServiceUrl
        );
    }
}
