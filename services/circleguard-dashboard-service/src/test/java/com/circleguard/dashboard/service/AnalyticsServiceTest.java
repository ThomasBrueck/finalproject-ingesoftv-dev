package com.circleguard.dashboard.service;

import com.circleguard.dashboard.client.PromotionClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private PromotionClient promotionClient;

    @Mock
    private KAnonymityFilter kAnonymityFilter;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void shouldGetCampusSummaryFromPromotionClient() {
        Map<String, Object> expected = Map.of("totalUsers", 250L);
        when(promotionClient.getHealthStats()).thenReturn(expected);

        Map<String, Object> result = analyticsService.getCampusSummary();

        assertEquals(expected, result);
    }

    @Test
    void shouldGetDepartmentStatsWithKAnonymity() {
        Map<String, Object> raw = Map.of("totalUsers", 50L);
        Map<String, Object> filtered = Map.of("totalUsers", 50L, "department", "Engineering");

        when(promotionClient.getHealthStatsByDepartment("Engineering")).thenReturn(raw);
        when(kAnonymityFilter.apply(raw)).thenReturn(filtered);

        Map<String, Object> result = analyticsService.getDepartmentStats("Engineering");

        assertEquals(filtered, result);
    }

    @Test
    void shouldGetGlobalHealthStats() {
        Map<String, Object> expected = Map.of("totalUsers", 1000L);
        when(promotionClient.getHealthStats()).thenReturn(expected);

        Map<String, Object> result = analyticsService.getGlobalHealthStats();

        assertEquals(expected, result);
    }

    @Test
    void shouldGetEntryTrendsWithKAnonymity() {
        UUID locationId = UUID.randomUUID();
        List<Map<String, Object>> rows = List.of(
                new HashMap<>(Map.of("hour", java.sql.Timestamp.valueOf("2024-01-01 10:00:00"), "entry_count", 12L)),
                new HashMap<>(Map.of("hour", java.sql.Timestamp.valueOf("2024-01-01 11:00:00"), "entry_count", 3L))
        );

        when(jdbc.queryForList(anyString(), eq(locationId))).thenReturn(rows);

        List<Map<String, Object>> result = analyticsService.getEntryTrends(locationId);

        assertEquals(12L, result.get(0).get("entry_count"));
        assertEquals("<5", result.get(1).get("entry_count"));
    }

    @Test
    void shouldGenerateMockTimeSeriesWhenDbQueryFails() {
        when(jdbc.queryForList(anyString(), anyInt())).thenThrow(new RuntimeException("Table not found"));

        List<Map<String, Object>> result = analyticsService.getTimeSeries("hourly", 5);

        assertFalse(result.isEmpty());
        assertTrue(result.get(0).containsKey("bucket"));
    }
}
