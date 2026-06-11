package com.circleguard.dashboard.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionClient {

    private final RestTemplate restTemplate;

    @Value("${circleguard.promotion-service.url:http://localhost:8088}")
    private String promotionServiceUrl;

    @CircuitBreaker(name = "promotionService", fallbackMethod = "getHealthStatsFallback")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHealthStats() {
        return restTemplate.getForObject(
                promotionServiceUrl + "/api/v1/health-status/stats",
                Map.class
        );
    }

    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("[A-Za-z0-9 _-]{1,64}");

    @CircuitBreaker(name = "promotionService", fallbackMethod = "getHealthStatsByDepartmentFallback")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHealthStatsByDepartment(String department) {
        // Validate user-controlled input and pass it as an encoded URI variable
        // instead of concatenating it into the path (S7044 - SSRF).
        if (department == null || !DEPARTMENT_PATTERN.matcher(department).matches()) {
            throw new IllegalArgumentException("Invalid department");
        }
        return restTemplate.getForObject(
                promotionServiceUrl + "/api/v1/health-status/stats/department/{dept}",
                Map.class,
                department
        );
    }

    Map<String, Object> getHealthStatsFallback(Exception e) {
        log.warn("Circuit breaker open for promotion-service [getHealthStats]: {}", e.getMessage());
        return Map.of(
                "status", "UNAVAILABLE",
                "message", "promotion-service is temporarily unavailable",
                "timestamp", Instant.now().toString()
        );
    }

    Map<String, Object> getHealthStatsByDepartmentFallback(String department, Exception e) {
        log.warn("Circuit breaker open for promotion-service [getHealthStatsByDepartment][{}]: {}",
                department, e.getMessage());
        return Map.of(
                "status", "UNAVAILABLE",
                "department", department,
                "message", "promotion-service is temporarily unavailable",
                "timestamp", Instant.now().toString()
        );
    }
}
