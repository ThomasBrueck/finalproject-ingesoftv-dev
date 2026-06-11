package com.circleguard.e2e.flow;

import com.circleguard.e2e.client.AuthServiceClient;
import com.circleguard.e2e.client.GatewayServiceClient;
import com.circleguard.e2e.client.HttpClientUtil;
import com.circleguard.e2e.config.E2ETestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationFlowE2ETest extends E2ETestBase {

    private static AuthServiceClient auth;
    private static GatewayServiceClient gateway;

    @BeforeAll
    static void init() {
        auth = new AuthServiceClient(authServiceUrl);
        gateway = new GatewayServiceClient(gatewayServiceUrl);
    }

    @Test
    void ca11_loginWithValidCredentialsReturnsJwt() {
        JsonNode response = auth.login("staff_guard", "password");

        assertEquals(200, HttpClientUtil.getStatus(response));
        assertNotNull(response.get("token"));
        assertNotNull(response.get("anonymousId"));
        assertEquals("Bearer", response.get("type").asText());
    }

    @Test
    void ca12_authenticatedUserCanGenerateQrToken() {
        JsonNode loginResponse = auth.login("staff_guard", "password");
        String jwt = loginResponse.get("token").asText();

        JsonNode qrResponse = auth.generateQrToken(jwt);

        assertEquals(200, HttpClientUtil.getStatus(qrResponse));
        assertNotNull(qrResponse.get("qrToken"));
        assertTrue(qrResponse.get("expiresIn").asInt() > 0);
    }

    @Test
    void ca13_gateAcceptsValidQrToken() {
        JsonNode loginResponse = auth.login("staff_guard", "password");
        String jwt = loginResponse.get("token").asText();
        JsonNode qrResponse = auth.generateQrToken(jwt);
        String qrToken = qrResponse.get("qrToken").asText();

        JsonNode gateResponse = gateway.validateQrToken(qrToken);

        assertEquals(200, HttpClientUtil.getStatus(gateResponse));
        assertTrue(gateResponse.get("valid").asBoolean());
        assertEquals("GREEN", gateResponse.get("status").asText());
    }

    @Test
    void ca14_gateRejectsInvalidQrToken() {
        JsonNode gateResponse = gateway.validateQrToken("invalid.token.payload");

        assertEquals(200, HttpClientUtil.getStatus(gateResponse));
        assertFalse(gateResponse.get("valid").asBoolean());
        assertEquals("RED", gateResponse.get("status").asText());
    }
}
