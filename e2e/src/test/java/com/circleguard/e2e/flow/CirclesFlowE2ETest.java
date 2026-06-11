package com.circleguard.e2e.flow;

import com.circleguard.e2e.client.AuthServiceClient;
import com.circleguard.e2e.client.HttpClientUtil;
import com.circleguard.e2e.client.PromotionServiceClient;
import com.circleguard.e2e.config.E2ETestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CirclesFlowE2ETest extends E2ETestBase {

    private static AuthServiceClient auth;
    private static PromotionServiceClient promotion;

    @BeforeAll
    static void init() {
        auth = new AuthServiceClient(authServiceUrl);
        promotion = new PromotionServiceClient(promotionServiceUrl);
    }

    @Test
    void ca41_userCanCreateCircle() {
        JsonNode loginResponse = auth.login("staff_guard", "password");
        String jwt = loginResponse.get("token").asText();

        JsonNode response = promotion.createCircle("E2E Test Circle", jwt);

        assertEquals(200, HttpClientUtil.getStatus(response));
        assertNotNull(response.get("id"));
        assertNotNull(response.get("inviteCode"));
    }

    @Test
    void ca42_userCanJoinCircleByCode() {
        JsonNode loginResponse = auth.login("staff_guard", "password");
        String jwt = loginResponse.get("token").asText();

        JsonNode createResponse = promotion.createCircle("Joinable Circle", jwt);
        String inviteCode = createResponse.get("inviteCode").asText();
        String newUser = UUID.randomUUID().toString();

        JsonNode joinResponse = promotion.joinCircle(inviteCode, newUser);

        assertEquals(200, HttpClientUtil.getStatus(joinResponse));
    }

    @Test
    void ca43_adminCanForceFenceOnCircle() {
        JsonNode staffLogin = auth.login("staff_guard", "password");
        String staffJwt = staffLogin.get("token").asText();

        JsonNode createResponse = promotion.createCircle("Fenced Circle", staffJwt);
        String circleId = createResponse.get("id").asText();

        JsonNode healthLogin = auth.login("health_user", "password");
        String healthJwt = healthLogin.get("token").asText();

        JsonNode fenceResponse = promotion.forceFence(circleId, healthJwt);

        assertEquals(200, HttpClientUtil.getStatus(fenceResponse));
    }
}
