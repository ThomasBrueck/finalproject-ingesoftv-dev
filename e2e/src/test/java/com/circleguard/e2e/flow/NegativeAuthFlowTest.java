package com.circleguard.e2e.flow;

import com.circleguard.e2e.client.AuthServiceClient;
import com.circleguard.e2e.client.HttpClientUtil;
import com.circleguard.e2e.config.E2ETestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NegativeAuthFlowTest extends E2ETestBase {

    private static AuthServiceClient auth;

    @BeforeAll
    static void init() {
        auth = new AuthServiceClient(authServiceUrl);
    }

    @Test
    void shouldRejectInvalidCredentials() {
        JsonNode response = auth.login("staff_guard", "wrongpassword");

        assertEquals(401, HttpClientUtil.getStatus(response));
        assertTrue(response.has("error") || response.has("message"));
    }

    @Test
    void shouldRejectExpiredQrToken() {
        JsonNode loginResponse = auth.login("staff_guard", "password");
        String jwt = loginResponse.get("token").asText();

        JsonNode qrResponse = auth.generateQrToken(jwt);
        assertEquals(200, HttpClientUtil.getStatus(qrResponse));

        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                + ".eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxNTAwMDAwMDAwfQ"
                + ".invalidsignature";

        JsonNode gateResponse = HttpClientUtil.post(
                gatewayServiceUrl + "/api/v1/gate/validate",
                java.util.Map.of("token", expiredToken), null);

        assertEquals(200, HttpClientUtil.getStatus(gateResponse));
        assertFalse(gateResponse.get("valid").asBoolean());
        assertEquals("RED", gateResponse.get("status").asText());
    }

    @Test
    void shouldRejectMissingJwt() {
        JsonNode response = HttpClientUtil.post(
                authServiceUrl + "/api/v1/auth/qr/generate",
                java.util.Map.of("anonymousId", java.util.UUID.randomUUID().toString()),
                null);

        assertEquals(401, HttpClientUtil.getStatus(response));
    }

    @Test
    void shouldRejectInvalidJwtFormat() {
        JsonNode response = HttpClientUtil.post(
                authServiceUrl + "/api/v1/auth/qr/generate",
                java.util.Map.of("anonymousId", java.util.UUID.randomUUID().toString()),
                "malformed.jwt.token");

        assertEquals(401, HttpClientUtil.getStatus(response));
    }
}
