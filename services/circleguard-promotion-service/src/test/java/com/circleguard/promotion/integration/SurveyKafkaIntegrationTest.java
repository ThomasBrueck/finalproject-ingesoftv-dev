package com.circleguard.promotion.integration;

import com.circleguard.promotion.listener.SurveyListener;
import com.circleguard.promotion.service.HealthStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class SurveyKafkaIntegrationTest {

    @Autowired
    private SurveyListener surveyListener;

    @MockBean
    private HealthStatusService healthStatusService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private Neo4jClient neo4jClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    void onSurveySubmittedWithSymptomsShouldPromoteToSuspect() {
        Map<String, Object> event = Map.of(
            "anonymousId", "survey-user-1",
            "hasSymptoms", true,
            "timestamp", System.currentTimeMillis()
        );

        surveyListener.onSurveySubmitted(event);

        verify(healthStatusService).updateStatus("survey-user-1", "SUSPECT");
    }

    @Test
    void onSurveySubmittedWithoutSymptomsShouldNotPromote() {
        Map<String, Object> event = Map.of(
            "anonymousId", "survey-user-2",
            "hasSymptoms", false,
            "timestamp", System.currentTimeMillis()
        );

        surveyListener.onSurveySubmitted(event);

        verify(healthStatusService, never()).updateStatus(anyString(), anyString());
    }

    @Test
    void onCertificateValidatedWithApprovedShouldRestoreToActive() {
        Map<String, Object> event = Map.of(
            "anonymousId", "survey-user-3",
            "status", "APPROVED",
            "adminId", "admin-123",
            "timestamp", System.currentTimeMillis()
        );

        surveyListener.onCertificateValidated(event);

        verify(healthStatusService).updateStatus("survey-user-3", "ACTIVE");
    }

    @Test
    void onCertificateValidatedWithRejectedShouldNotRestore() {
        Map<String, Object> event = Map.of(
            "anonymousId", "survey-user-4",
            "status", "REJECTED",
            "adminId", "admin-123",
            "timestamp", System.currentTimeMillis()
        );

        surveyListener.onCertificateValidated(event);

        verify(healthStatusService, never()).updateStatus(anyString(), anyString());
    }
}
