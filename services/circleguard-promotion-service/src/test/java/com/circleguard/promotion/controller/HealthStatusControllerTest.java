package com.circleguard.promotion.controller;

import com.circleguard.promotion.service.HealthStatusService;
import com.circleguard.promotion.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthStatusController.class)
@Import(SecurityConfig.class)
class HealthStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthStatusService statusService;

    @Test
    @WithMockUser(roles = "HEALTH_CENTER")
    void confirmPositive_WithPermission_CallsUpdateStatus() throws Exception {
        String json = "{\"anonymousId\": \"user-1\"}";

        mockMvc.perform(post("/api/v1/health/confirmed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(statusService).updateStatus("user-1", "CONFIRMED");
    }

    @Test
    @WithMockUser(roles = "HEALTH_CENTER")
    void resolve_WithPermission_CallsResolveStatus() throws Exception {
        String json = "{\"anonymousId\": \"user-1\"}";

        mockMvc.perform(post("/api/v1/health/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(statusService).resolveStatus("user-1", false);
    }

    @Test
    @WithMockUser(authorities = "STUDENT")
    void resolve_WithoutPermission_Returns403() throws Exception {
        String json = "{\"anonymousId\": \"user-1\"}";

        mockMvc.perform(post("/api/v1/health/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void resolve_Unauthenticated_Returns403() throws Exception {
        String json = "{\"anonymousId\": \"user-1\"}";

        mockMvc.perform(post("/api/v1/health/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HEALTH_CENTER")
    void reportStatus_WithoutOverride_DefaultsToFalse() throws Exception {
        String json = "{\"anonymousId\": \"user-2\", \"status\": \"SUSPECT\"}";

        mockMvc.perform(post("/api/v1/health/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(statusService).updateStatus("user-2", "SUSPECT", false);
    }

    @Test
    @WithMockUser(roles = "HEALTH_CENTER")
    void reportStatus_WithAdminOverride_PassesTrue() throws Exception {
        String json = "{\"anonymousId\": \"user-3\", \"status\": \"ACTIVE\", \"adminOverride\": true}";

        mockMvc.perform(post("/api/v1/health/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(statusService).updateStatus("user-3", "ACTIVE", true);
    }

    @Test
    @WithMockUser(roles = "HEALTH_CENTER")
    void recover_WithPermission_PromotesToRecovered() throws Exception {
        mockMvc.perform(post("/api/v1/health/recovery/user-4"))
                .andExpect(status().isOk());

        verify(statusService).promoteToRecovered("user-4");
    }

    @Test
    @WithMockUser(roles = "HEALTH_CENTER")
    void resolve_WithAdminOverride_PassesTrue() throws Exception {
        String json = "{\"anonymousId\": \"user-5\", \"adminOverride\": true}";

        mockMvc.perform(post("/api/v1/health/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(statusService).resolveStatus("user-5", true);
    }
}
