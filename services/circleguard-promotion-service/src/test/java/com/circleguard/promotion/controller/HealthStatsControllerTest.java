package com.circleguard.promotion.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthStatsControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Neo4jClient neo4jClient;

    private HealthStatsController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthStatsController(neo4jClient);
    }

    @Test
    void getStats_ShouldReturnAggregatedCounts() {
        Collection<Map<String, Object>> rows = List.of(
                Map.of("status", "HEALTHY", "total", 10L),
                Map.of("status", "SYMPTOMATIC", "total", 3L)
        );
        when(neo4jClient.query(anyString()).fetch().all()).thenReturn(rows);

        ResponseEntity<Map<String, Object>> response = controller.getStats();

        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().get("healthyCount"));
        assertEquals(3L, response.getBody().get("symptomaticCount"));
        assertEquals(13L, response.getBody().get("totalUsers"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }

    @Test
    void getStats_WithNullStatus_ShouldUseUnknown() {
        Map<String, Object> row = new HashMap<>();
        row.put("status", null);
        row.put("total", 5L);
        Collection<Map<String, Object>> rows = List.of(row);
        when(neo4jClient.query(anyString()).fetch().all()).thenReturn(rows);

        ResponseEntity<Map<String, Object>> response = controller.getStats();

        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().get("unknownCount"));
        assertEquals(5L, response.getBody().get("totalUsers"));
    }

    @Test
    void getDepartmentStats_ShouldReturnFilteredCounts() {
        String department = "Engineering";
        Collection<Map<String, Object>> rows = List.of(
                Map.of("status", "HEALTHY", "total", 7L)
        );
        when(neo4jClient.query(anyString()).bind(department).to("dept").fetch().all()).thenReturn(rows);

        ResponseEntity<Map<String, Object>> response = controller.getStatsByDepartment(department);

        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().get("healthyCount"));
        assertEquals("Engineering", response.getBody().get("department"));
        assertEquals(7L, response.getBody().get("totalUsers"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }

    @Test
    void getDepartmentStats_WithNoRows_ShouldReturnZeroTotal() {
        String department = "EmptyDept";
        when(neo4jClient.query(anyString()).bind(department).to("dept").fetch().all()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getStatsByDepartment(department);

        assertNotNull(response.getBody());
        assertEquals(0L, response.getBody().get("totalUsers"));
        assertEquals(department, response.getBody().get("department"));
    }
}
