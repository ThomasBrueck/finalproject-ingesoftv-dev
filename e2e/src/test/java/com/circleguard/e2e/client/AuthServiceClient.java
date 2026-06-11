package com.circleguard.e2e.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class AuthServiceClient {

    private final String baseUrl;

    public AuthServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode login(String username, String password) {
        return HttpClientUtil.post(baseUrl + "/api/v1/auth/login",
                Map.of("username", username, "password", password), null);
    }

    public JsonNode generateQrToken(String jwt) {
        return HttpClientUtil.get(baseUrl + "/api/v1/auth/qr/generate", jwt);
    }

    public JsonNode visitorHandoff(String jwt) {
        return HttpClientUtil.post(baseUrl + "/api/v1/auth/visitor/handoff",
                Map.of("anonymousId", java.util.UUID.randomUUID().toString()), jwt);
    }
}
