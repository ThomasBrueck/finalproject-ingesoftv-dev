package com.circleguard.auth.client;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Cliente HTTP para comunicación con circleguard-identity-service.
 *
 * Implementa el patrón Bulkhead (Mamparo) con Resilience4j para aislar
 * los recursos del sistema (hilos) y evitar que una alta concurrencia de
 * peticiones a identity-service agote los recursos del auth-service completo.
 *
 * Configuración del Bulkhead en application.yml:
 * - max-concurrent-calls: Máximo de llamadas simultáneas permitidas.
 * - max-wait-duration: Tiempo máximo de espera antes de rechazar la solicitud.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityClient {

    private final RestTemplate restTemplate;

    @Value("${circleguard.identity-service.url:http://localhost:8083}")
    private String identityServiceUrl;

    /**
     * Obtiene el UUID anónimo de un usuario dado su identidad real.
     *
     * Si el Bulkhead está saturado (más de max-concurrent-calls llamadas
     * simultáneas), se invoca automáticamente getAnonymousIdFallback.
     *
     * CA 1.1, 1.2: Semaphore-based bulkhead con límite de concurrencia configurado.
     * CA 2.1, 2.2: Fallback de respuesta segura y degradada.
     */
    @Bulkhead(name = "identityService", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "getAnonymousIdFallback")
    public UUID getAnonymousId(String realIdentity) {
        Map<String, String> request = Map.of("realIdentity", realIdentity);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                identityServiceUrl + "/api/v1/identities/map",
                request,
                Map.class);
        return UUID.fromString(response.get("anonymousId").toString());
    }

    /**
     * Fallback ejecutado cuando el Bulkhead está saturado o falla la llamada.
     *
     * CA 2.2: Retorna un UUID determinista seguro basado en la identidad real,
     * garantizando que el flujo de autenticación no se interrumpa completamente.
     * CA 2.3: Registra un log WARN indicando la saturación o fallo.
     */
    UUID getAnonymousIdFallback(String realIdentity, BulkheadFullException ex) {
        log.warn("[BULKHEAD] identity-service saturado. Max llamadas concurrentes alcanzadas " +
                "para la identidad '{}'. Retornando UUID determinista de contingencia. Error: {}",
                realIdentity, ex.getMessage());
        // UUID determinista y reproducible derivado de la identidad real del usuario
        return UUID.nameUUIDFromBytes(realIdentity.getBytes());
    }

    /**
     * Fallback genérico ejecutado cuando identity-service falla por cualquier error
     * de red.
     *
     * CA 2.2: Cubre fallos de conexión o timeout además de la saturación del
     * Bulkhead.
     */
    UUID getAnonymousIdFallback(String realIdentity, Exception ex) {
        log.warn("[BULKHEAD] Fallo en llamada a identity-service para la identidad '{}'. " +
                "Retornando UUID determinista de contingencia. Error: {}",
                realIdentity, ex.getMessage());
        return UUID.nameUUIDFromBytes(realIdentity.getBytes());
    }
}
