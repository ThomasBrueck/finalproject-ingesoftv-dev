package com.circleguard.notification.integration;

import com.circleguard.notification.service.ExposureNotificationListener;
import com.circleguard.notification.service.LmsService;
import com.circleguard.notification.service.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class StatusChangeNotificationIntegrationTest {

    @Autowired
    private ExposureNotificationListener listener;

    @MockBean
    private NotificationDispatcher dispatcher;

    @MockBean
    private LmsService lmsService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @MockBean
    private org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;

    @Test
    void shouldDispatchAndSyncForSuspectStatus() {
        String event = "{\"anonymousId\": \"user-1\", \"status\": \"SUSPECT\"}";

        listener.handleStatusChange(event);

        verify(dispatcher).dispatch("user-1", "SUSPECT");
        verify(lmsService).syncRemoteAttendance("user-1", "SUSPECT");
    }

    @Test
    void shouldDispatchAndSyncForProbableStatus() {
        String event = "{\"anonymousId\": \"user-2\", \"status\": \"PROBABLE\"}";

        listener.handleStatusChange(event);

        verify(dispatcher).dispatch("user-2", "PROBABLE");
        verify(lmsService).syncRemoteAttendance("user-2", "PROBABLE");
    }

    @Test
    void shouldDispatchAndSyncForConfirmedStatus() {
        String event = "{\"anonymousId\": \"user-3\", \"status\": \"CONFIRMED\"}";

        listener.handleStatusChange(event);

        verify(dispatcher).dispatch("user-3", "CONFIRMED");
        verify(lmsService).syncRemoteAttendance("user-3", "CONFIRMED");
    }

    @Test
    void shouldNotDispatchWhenStatusIsActive() {
        String event = "{\"anonymousId\": \"user-4\", \"status\": \"ACTIVE\"}";

        listener.handleStatusChange(event);

        verify(dispatcher, never()).dispatch(anyString(), anyString());
        verify(lmsService, never()).syncRemoteAttendance(anyString(), anyString());
    }

    @Test
    void shouldHandleMalformedEventWithoutError() {
        String event = "invalid-json";

        listener.handleStatusChange(event);

        verify(dispatcher, never()).dispatch(anyString(), anyString());
        verify(lmsService, never()).syncRemoteAttendance(anyString(), anyString());
    }
}
