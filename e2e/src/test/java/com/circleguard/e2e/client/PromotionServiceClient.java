package com.circleguard.e2e.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class PromotionServiceClient {

    private final String baseUrl;

    public PromotionServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode reportHealthStatus(String anonymousId, boolean hasFever, boolean hasCough, String jwt) {
        return HttpClientUtil.post(baseUrl + "/api/v1/health/report",
                Map.of("anonymousId", anonymousId, "hasFever", hasFever, "hasCough", hasCough), jwt);
    }

    public JsonNode confirmPositive(String anonymousId, String jwt) {
        return HttpClientUtil.post(baseUrl + "/api/v1/health/confirmed",
                Map.of("anonymousId", anonymousId), jwt);
    }

    public JsonNode getHealthStats() {
        return HttpClientUtil.get(baseUrl + "/api/v1/health-status/stats", null);
    }

    public JsonNode createCircle(String name, String jwt) {
        return HttpClientUtil.post(baseUrl + "/api/v1/circles",
                Map.of("name", name, "description", "E2E test circle"), jwt);
    }

    public JsonNode joinCircle(String code, String anonymousId) {
        return HttpClientUtil.post(baseUrl + "/api/v1/circles/join/" + code + "/user/" + anonymousId,
                null, null);
    }

    public JsonNode getUserCircles(String anonymousId) {
        return HttpClientUtil.get(baseUrl + "/api/v1/circles/user/" + anonymousId, null);
    }

    public JsonNode forceFence(String circleId, String jwt) {
        return HttpClientUtil.post(baseUrl + "/api/v1/circles/" + circleId + "/force-fence",
                null, jwt);
    }

    public JsonNode reportEncounter(String userA, String userB, String location) {
        return HttpClientUtil.post(baseUrl + "/api/v1/encounters/report",
                Map.of("userA", userA, "userB", userB, "location", location, "timestamp",
                        java.time.Instant.now().toString()), null);
    }
}
