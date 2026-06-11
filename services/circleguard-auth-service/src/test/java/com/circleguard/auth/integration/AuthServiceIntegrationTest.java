package com.circleguard.auth.integration;

import com.circleguard.auth.client.IdentityClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private IdentityClient identityClient;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void shouldCallIdentityServiceMapEndpointAndReturnAnonymousId() {
        String realIdentity = "auth.integration@universidad.edu";
        UUID expectedUUID = UUID.randomUUID();
        Map<String, Object> mockResponse = Map.of("anonymousId", expectedUUID.toString());

        when(restTemplate.postForObject(
                eq("http://localhost:8083/api/v1/identities/map"),
                eq(Map.of("realIdentity", realIdentity)),
                eq(Map.class)))
                .thenReturn(mockResponse);

        UUID result = identityClient.getAnonymousId(realIdentity);

        assertThat(result).isEqualTo(expectedUUID);
        verify(restTemplate).postForObject(
                eq("http://localhost:8083/api/v1/identities/map"),
                eq(Map.of("realIdentity", realIdentity)),
                eq(Map.class));
    }

    @Test
    void shouldReturnDeterministicFallbackWhenIdentityServiceIsUnavailable() {
        String realIdentity = "fallback.test@universidad.edu";

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        UUID result = identityClient.getAnonymousId(realIdentity);

        UUID expectedFallback = UUID.nameUUIDFromBytes(realIdentity.getBytes());
        assertThat(result).isEqualTo(expectedFallback);
    }
}
