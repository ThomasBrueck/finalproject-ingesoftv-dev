package com.circleguard.auth.client;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para la resiliencia del IdentityClient.
 *
 * CA 4.1: Verifica que:
 * - Las llamadas normales fluyan correctamente a través del Bulkhead.
 * - Al saturar el Bulkhead, se ejecute el fallback por BulkheadFullException.
 * - Al producirse un error de red, se ejecute el fallback genérico.
 */
@ExtendWith(MockitoExtension.class)
class IdentityClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private IdentityClient identityClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(identityClient, "identityServiceUrl", "http://localhost:8083");
    }

    @Test
    void shouldReturnAnonymousIdSuccessfullyWhenServiceIsAvailable() {
        // Arrange
        String realIdentity = "juan.eraso@universidad.edu";
        UUID expectedUUID = UUID.randomUUID();
        Map<String, Object> mockResponse = Map.of("anonymousId", expectedUUID.toString());

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(mockResponse);

        // Act
        UUID result = identityClient.getAnonymousId(realIdentity);

        // Assert
        assertThat(result).isEqualTo(expectedUUID);
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void shouldReturnDeterministicFallbackUUIDWhenBulkheadIsFull() {
        // Arrange
        String realIdentity = "juan.eraso@universidad.edu";
        BulkheadFullException bulkheadException = BulkheadFullException.createBulkheadFullException(
                io.github.resilience4j.bulkhead.Bulkhead.ofDefaults("identityService")
        );

        // Act: llamar directamente al método fallback
        UUID result = identityClient.getAnonymousIdFallback(realIdentity, bulkheadException);

        // Assert: CA 2.2 - UUID determinista generado desde la identidad real
        UUID expectedFallbackUUID = UUID.nameUUIDFromBytes(realIdentity.getBytes());
        assertThat(result).isEqualTo(expectedFallbackUUID);
    }

    @Test
    void shouldReturnDeterministicFallbackUUIDWhenNetworkFails() {
        // Arrange
        String realIdentity = "juan.eraso@universidad.edu";
        RestClientException networkError = new RestClientException("Connection refused: identity-service unreachable");

        // Act: llamar directamente al método fallback genérico
        UUID result = identityClient.getAnonymousIdFallback(realIdentity, networkError);

        // Assert: CA 2.2 - UUID determinista generado desde la identidad real
        UUID expectedFallbackUUID = UUID.nameUUIDFromBytes(realIdentity.getBytes());
        assertThat(result).isEqualTo(expectedFallbackUUID);
    }

    @Test
    void shouldProduceSameFallbackUUIDForSameIdentity() {
        // Arrange
        String realIdentity = "juan.eraso@universidad.edu";
        Exception ex = new RuntimeException("service down");

        // Act: invocar fallback dos veces con la misma identidad
        UUID result1 = identityClient.getAnonymousIdFallback(realIdentity, ex);
        UUID result2 = identityClient.getAnonymousIdFallback(realIdentity, ex);

        // Assert: el UUID determinista debe ser siempre el mismo para la misma identidad
        assertThat(result1).isEqualTo(result2);
    }
}
