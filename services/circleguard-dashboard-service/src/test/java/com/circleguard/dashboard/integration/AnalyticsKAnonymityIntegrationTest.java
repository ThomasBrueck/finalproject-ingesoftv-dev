package com.circleguard.dashboard.integration;

import com.circleguard.dashboard.client.PromotionClient;
import com.circleguard.dashboard.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class AnalyticsKAnonymityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("circleguard_dashboard")
            .withUsername("admin")
            .withPassword("password");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("circleguard.promotion-service.url", () -> "http://localhost:8088");
    }

    @Autowired
    private PromotionClient promotionClient;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldReturnHealthBoardStatsFromService() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 250L);
        stats.put("greenCount", 220L);
        stats.put("redCount", 30L);

        String responseBody = """
            {
                "totalUsers": 250,
                "greenCount": 220,
                "redCount": 30
            }
            """;

        mockRestServiceServer.expect(requestTo("http://localhost:8088/api/v1/health-status/stats"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        Map<String, Object> result = analyticsService.getGlobalHealthStats();

        assertThat(result).containsEntry("totalUsers", 250L);
        assertThat(result).containsEntry("greenCount", 220L);
        mockRestServiceServer.verify();
    }

    @Test
    void shouldReturnDepartmentStatsForGivenDepartment() {
        Map<String, Object> filtered = new LinkedHashMap<>();
        filtered.put("totalUsers", 50L);
        filtered.put("greenCount", 45L);
        filtered.put("redCount", "<5");
        filtered.put("department", "Engineering");

        String responseBody = """
            {
                "totalUsers": 50,
                "greenCount": 45,
                "redCount": "<5",
                "department": "Engineering"
            }
            """;

        mockRestServiceServer.expect(requestTo("http://localhost:8088/api/v1/health-status/stats/department/Engineering"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        Map<String, Object> result = analyticsService.getDepartmentStats("Engineering");

        assertThat(result).containsEntry("department", "Engineering");
        assertThat(result).containsEntry("redCount", "<5");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldReturnCampusSummary() {
        String responseBody = """
            {
                "campus": "main",
                "activeUsers": 500
            }
            """;

        mockRestServiceServer.expect(requestTo("http://localhost:8088/api/v1/health-status/stats"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        Map<String, Object> result = analyticsService.getCampusSummary();

        assertThat(result).containsEntry("campus", "main");
        assertThat(result).containsEntry("activeUsers", 500L);
        mockRestServiceServer.verify();
    }
}