package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void shouldLogNotification() {
        auditLogService.logDelivery("user-1", "EMAIL", "SUCCESS", "corr-123");

        verify(kafkaTemplate).send(eq("notification.audit"), eq("user-1"), argThat((Map<String, Object> m) ->
                "EMAIL".equals(m.get("channel")) && "SUCCESS".equals(m.get("status")) &&
                "user-1".equals(m.get("userId")) && "corr-123".equals(m.get("correlationId"))));
    }

    @Test
    void shouldSendAuditEventToKafka() {
        auditLogService.logDelivery("user-1", "EMAIL", "SUCCESS", "corr-123");

        verify(kafkaTemplate).send(eq("notification.audit"), eq("user-1"), any(Map.class));
    }

    @Test
    void shouldGenerateCorrelationIdWhenNull() {
        auditLogService.logDelivery("user-2", "SMS", "FAILED", null);

        verify(kafkaTemplate).send(eq("notification.audit"), eq("user-2"), argThat((Map<String, Object> m) ->
                m.get("correlationId") != null
        ));
    }
}
