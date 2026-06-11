package com.circleguard.e2e.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class IdentityServiceClient {

    private final String baseUrl;

    public IdentityServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode mapIdentity(String realIdentity, String jwt) {
        return HttpClientUtil.post(baseUrl + "/api/v1/identities/map",
                Map.of("realIdentity", realIdentity), jwt);
    }

    public JsonNode registerVisitor(String name, String email) {
        return HttpClientUtil.post(baseUrl + "/api/v1/identities/visitor",
                Map.of("name", name, "email", email, "reason_for_visit", "E2E Test"), null);
    }

    public JsonNode lookupIdentity(String anonymousId, String jwt) {
        return HttpClientUtil.get(baseUrl + "/api/v1/identities/lookup/" + anonymousId, jwt);
    }
}
