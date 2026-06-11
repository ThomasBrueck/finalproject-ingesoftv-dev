package com.circleguard.dashboard.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionClientTest {

    @Mock
    private RestTemplate restTemplate;

    private PromotionClient client;

    @BeforeEach
    void setUp() {
        client = new PromotionClient(restTemplate);
        ReflectionTestUtils.setField(client, "promotionServiceUrl", "http://localhost:8088");
    }

    @Test
    void shouldGetHealthStats() {
        Map<String, Object> expected = Map.of("totalUsers", 100L);
        when(restTemplate.getForObject("http://localhost:8088/api/v1/health-status/stats", Map.class))
                .thenReturn(expected);

        Map<String, Object> result = client.getHealthStats();

        assertEquals(expected, result);
    }

    @Test
    void shouldGetHealthStatsByDepartment() {
        Map<String, Object> expected = Map.of("department", "Engineering");
        when(restTemplate.getForObject(
                "http://localhost:8088/api/v1/health-status/stats/department/{dept}", Map.class, "Engineering"))
                .thenReturn(expected);

        Map<String, Object> result = client.getHealthStatsByDepartment("Engineering");

        assertEquals(expected, result);
    }

    @Test
    void shouldReturnFallbackWhenCircuitBreakerOpen() {
        Exception ex = new RuntimeException("Service down");

        Map<String, Object> result = client.getHealthStatsFallback(ex);

        assertEquals("UNAVAILABLE", result.get("status"));
    }

    @Test
    void shouldReturnDepartmentFallbackWhenCircuitBreakerOpen() {
        Exception ex = new RuntimeException("Service down");

        Map<String, Object> result = client.getHealthStatsByDepartmentFallback("Physics", ex);

        assertEquals("UNAVAILABLE", result.get("status"));
        assertEquals("Physics", result.get("department"));
    }
}
