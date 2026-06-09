package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ExposureNotificationListenerTest {

    @Autowired
    private ExposureNotificationListener listener;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private NotificationDispatcher dispatcher;

    @MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @MockBean
    private org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmsService smsService;

    @MockBean
    private PushService pushService;

    @MockBean
    private LmsService lmsService;

    @Test
    void shouldHandleStatusChangeEventWithoutError() {
        String mockEvent = "{\"anonymousId\": \"user-123\", \"status\": \"EXPOSED\"}";
        assertDoesNotThrow(() -> listener.handleStatusChange(mockEvent));
    }

    @Test
    void shouldDispatchAndSyncWhenStatusIsNotActive() {
        String event = "{\"anonymousId\": \"user-456\", \"status\": \"SUSPECT\"}";

        listener.handleStatusChange(event);

        verify(dispatcher).dispatch("user-456", "SUSPECT");
        verify(lmsService).syncRemoteAttendance("user-456", "SUSPECT");
    }

    @Test
    void shouldNotDispatchWhenStatusIsActive() {
        String event = "{\"anonymousId\": \"user-789\", \"status\": \"ACTIVE\"}";

        listener.handleStatusChange(event);

        verify(dispatcher, never()).dispatch(anyString(), anyString());
        verify(lmsService, never()).syncRemoteAttendance(anyString(), anyString());
    }
}
