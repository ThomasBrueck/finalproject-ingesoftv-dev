package com.circleguard.e2e.flow;

import com.circleguard.e2e.client.AuthServiceClient;
import com.circleguard.e2e.client.GatewayServiceClient;
import com.circleguard.e2e.client.HttpClientUtil;
import com.circleguard.e2e.config.E2ETestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NegativeQrFlowTest extends E2ETestBase {

    private static AuthServiceClient auth;
    private static GatewayServiceClient gateway;

    @BeforeAll
    static void init() {
        auth = new AuthServiceClient(authServiceUrl);
        gateway = new GatewayServiceClient(gatewayServiceUrl);
    }

    @Test
    void shouldRejectEmptyQrToken() {
        JsonNode response = gateway.validateQrToken("");

        assertEquals(200, HttpClientUtil.getStatus(response));
        assertFalse(response.get("valid").asBoolean());
        assertEquals("RED", response.get("status").asText());
    }

    @Test
    void shouldRejectNullQrToken() {
        Map<String, String> body = new HashMap<>();
        body.put("token", null);

        JsonNode response = HttpClientUtil.post(
                gatewayServiceUrl + "/api/v1/gate/validate",
                body, null);

        assertEquals(200, HttpClientUtil.getStatus(response));
        assertFalse(response.get("valid").asBoolean());
        assertEquals("RED", response.get("status").asText());
    }

    @Test
    void shouldRejectTamperedQrToken() {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"test\",\"exp\":9999999999}".getBytes());
        String tamperedToken = header + "." + payload + ".tampered-signature";

        JsonNode response = gateway.validateQrToken(tamperedToken);

        assertEquals(200, HttpClientUtil.getStatus(response));
        assertFalse(response.get("valid").asBoolean());
        assertEquals("RED", response.get("status").asText());
    }
}
