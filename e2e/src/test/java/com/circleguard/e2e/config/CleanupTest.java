package com.circleguard.e2e.config;

import com.circleguard.e2e.client.HttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class CleanupTest {

    private static boolean initialized = false;
    private static String adminJwt;

    public static synchronized void resetTestData(String authUrl, String identityUrl,
                                                   String promotionUrl, String formUrl) {
        if (!initialized) {
            JsonNode login = HttpClientUtil.post(authUrl + "/api/v1/auth/login",
                    Map.of("username", "staff_guard", "password", "password"), null);
            adminJwt = login.has("_status") && login.get("_status").asInt() == 200
                    ? login.get("token").asText() : null;
            initialized = true;
        }

        if (adminJwt != null) {
            HttpClientUtil.post(identityUrl + "/api/v1/identities/admin/purge-test-users",
                    Map.of("confirm", true), adminJwt);
            HttpClientUtil.post(promotionUrl + "/api/v1/health/admin/reset-test-data",
                    Map.of("confirm", true), adminJwt);
            HttpClientUtil.post(formUrl + "/api/v1/questionnaires/admin/reset-test-data",
                    Map.of("confirm", true), adminJwt);
        }
    }

    public static void reset() {
        initialized = false;
        adminJwt = null;
    }
}
