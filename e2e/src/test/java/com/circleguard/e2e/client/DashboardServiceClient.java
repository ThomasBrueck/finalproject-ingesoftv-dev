package com.circleguard.e2e.client;

import com.fasterxml.jackson.databind.JsonNode;

public class DashboardServiceClient {

    private final String baseUrl;

    public DashboardServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode getHealthBoard() {
        return HttpClientUtil.get(baseUrl + "/api/v1/analytics/health-board", null);
    }

    public JsonNode getCampusSummary() {
        return HttpClientUtil.get(baseUrl + "/api/v1/analytics/summary", null);
    }
}
