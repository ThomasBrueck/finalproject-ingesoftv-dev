package com.circleguard.auth.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para IdentityClient usando MockRestServiceServer.
 * Simulan el contrato HTTP (API a API) entre el servicio de Autorización y el de Identidad.
 * Validamos el CA2 de las pruebas: Integración entre servicios relacionados verificando
 * que los envíos HTTP (request) y la interpretación de respuestas (response) son correctos.
 */
@RestClientTest(IdentityClient.class)
@ActiveProfiles("test")
@Import(IdentityClientIntegrationTest.RestClientConfig.class)
@TestPropertySource(properties = {
        "circleguard.identity-service.url=http://localhost:8083"
})
class IdentityClientIntegrationTest {

    @TestConfiguration
    static class RestClientConfig {
        @Bean
        public RestTemplate restTemplate(RestTemplateBuilder builder) {
            return builder.build();
        }
    }

    @Autowired
    private IdentityClient identityClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldReturnAnonymousIdWhenIdentityServiceRespondsOk() {
        // Arrange
        String realIdentity = "integracion.e2e@universidad.edu";
        UUID expectedUUID = UUID.randomUUID();
        
        // Simular respuesta JSON válida desde el servicio externo
        String expectedJsonResponse = "{\"anonymousId\": \"" + expectedUUID.toString() + "\"}";

        // Configurar el servidor Mock para interceptar la llamada de red real del RestTemplate
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:8083/api/v1/identities/map"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().json("{\"realIdentity\":\"" + realIdentity + "\"}"))
                .andRespond(MockRestResponseCreators.withSuccess(expectedJsonResponse, MediaType.APPLICATION_JSON));

        // Act
        // Llamada que ejecutará el código HTTP real subyacente
        UUID result = identityClient.getAnonymousId(realIdentity);

        // Assert
        assertThat(result).isEqualTo(expectedUUID);
        server.verify(); // Verifica que la solicitud de red definida en expect() ocurrió exactamente como se declaró
    }

}
