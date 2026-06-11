package com.circleguard.e2e.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class GatewayServiceClient {

    private final String baseUrl;

    public GatewayServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode validateQrToken(String token) {
        return HttpClientUtil.post(baseUrl + "/api/v1/gate/validate",
                Map.of("token", token), null);
    }
}
