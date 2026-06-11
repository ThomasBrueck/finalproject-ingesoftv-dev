package com.circleguard.auth.integration;

import com.circleguard.auth.client.IdentityClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("circleguard_auth")
            .withUsername("admin")
            .withPassword("password");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("circleguard.identity-service.url", () -> "http://localhost:8083");
    }

    @Autowired
    private IdentityClient identityClient;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldCallIdentityServiceMapEndpointAndReturnAnonymousId() {
        String realIdentity = "auth.integration@universidad.edu";
        UUID expectedUUID = UUID.randomUUID();
        String responseBody = "{\"anonymousId\":\"" + expectedUUID + "\"}";

        mockRestServiceServer.expect(requestTo("http://localhost:8083/api/v1/identities/map"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        UUID result = identityClient.getAnonymousId(realIdentity);

        assertThat(result).isEqualTo(expectedUUID);
        mockRestServiceServer.verify();
    }

    @Test
    void shouldReturnDeterministicFallbackWhenIdentityServiceIsUnavailable() {
        String realIdentity = "fallback.test@universidad.edu";

        mockRestServiceServer.expect(requestTo("http://localhost:8083/api/v1/identities/map"))
                .andRespond(request -> {
                    throw new java.net.ConnectException("Connection refused");
                });

        UUID result = identityClient.getAnonymousId(realIdentity);

        UUID expectedFallback = UUID.nameUUIDFromBytes(realIdentity.getBytes());
        assertThat(result).isEqualTo(expectedFallback);
        mockRestServiceServer.verify();
    }
}