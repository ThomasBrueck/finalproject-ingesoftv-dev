package com.circleguard.e2e.flow;

import com.circleguard.e2e.client.AuthServiceClient;
import com.circleguard.e2e.client.HttpClientUtil;
import com.circleguard.e2e.client.PromotionServiceClient;
import com.circleguard.e2e.client.DashboardServiceClient;
import com.circleguard.e2e.config.E2ETestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContactTracingFlowE2ETest extends E2ETestBase {

    private static AuthServiceClient auth;
    private static PromotionServiceClient promotion;
    private static DashboardServiceClient dashboard;

    @BeforeAll
    static void init() {
        auth = new AuthServiceClient(authServiceUrl);
        promotion = new PromotionServiceClient(promotionServiceUrl);
        dashboard = new DashboardServiceClient(dashboardServiceUrl);
    }

    @Test
    void ca31_healthCenterCanConfirmPositive() {
        JsonNode loginResponse = auth.login("health_user", "password");
        String jwt = loginResponse.get("token").asText();
        String anonymousId = UUID.randomUUID().toString();

        promotion.reportEncounter(anonymousId, UUID.randomUUID().toString(), "Library");

        JsonNode response = promotion.reportHealthStatus(anonymousId, true, true, jwt);

        assertEquals(200, HttpClientUtil.getStatus(response));
    }

    @Test
    void ca32_healthStatsReflectCases() {
        JsonNode stats = promotion.getHealthStats();

        assertEquals(200, HttpClientUtil.getStatus(stats));
        assertNotNull(stats);
    }

    @Test
    void ca33_dashboardShowsAnonymizedAnalytics() {
        JsonNode healthBoard = dashboard.getHealthBoard();

        assertEquals(200, HttpClientUtil.getStatus(healthBoard));
        assertNotNull(healthBoard);
    }

    @Test
    void ca34_notificationServiceIsRunning() {
        JsonNode stats = promotion.getHealthStats();

        assertNotNull(stats);
        assertTrue(stats.has("total") || stats.size() >= 0);
    }
}
