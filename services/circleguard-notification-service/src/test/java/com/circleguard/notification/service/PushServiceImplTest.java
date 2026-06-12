package com.circleguard.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushServiceImplTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private AuditLogService auditLogService;

    private PushServiceImpl pushService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(mock(WebClient.class));
        pushService = new PushServiceImpl(webClientBuilder, "http://localhost:8080");
        ReflectionTestUtils.setField(pushService, "auditLogService", auditLogService);
    }

    @Test
    void shouldSendMockPushAndLogSuccess() {
        ReflectionTestUtils.setField(pushService, "gotifyToken", "MOCK_TOKEN");

        pushService.sendAsync("user-123", "Test push", Map.of("url", "circleguard://alert"));

        verify(auditLogService).logDelivery(eq("user-123"), eq("PUSH"), eq("SUCCESS"), any());
    }

    @Test
    void shouldSendMockPushWithoutMetadata() {
        ReflectionTestUtils.setField(pushService, "gotifyToken", "MOCK_TOKEN");

        pushService.sendAsync("user-123", "Simple push");

        verify(auditLogService).logDelivery(eq("user-123"), eq("PUSH"), eq("SUCCESS"), any());
    }

    @Test
    void shouldSendRealPushWhenTokenConfigured() {
        var exchange = (org.springframework.web.reactive.function.client.ExchangeFunction) request ->
                reactor.core.publisher.Mono.just(
                        org.springframework.web.reactive.function.client.ClientResponse
                                .create(org.springframework.http.HttpStatus.OK).build());
        PushServiceImpl realService = new PushServiceImpl(
                WebClient.builder().exchangeFunction(exchange), "http://gotify.test");
        ReflectionTestUtils.setField(realService, "auditLogService", auditLogService);
        ReflectionTestUtils.setField(realService, "gotifyToken", "token-real");

        realService.sendAsync("user-9", "Alerta", Map.of("url", "circleguard://x"));

        verify(auditLogService).logDelivery(eq("user-9"), eq("PUSH"), eq("SUCCESS"), any());
    }

    @Test
    void shouldLogRetryAndRethrowOnHttpError() {
        var exchange = (org.springframework.web.reactive.function.client.ExchangeFunction) request ->
                reactor.core.publisher.Mono.just(
                        org.springframework.web.reactive.function.client.ClientResponse
                                .create(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build());
        PushServiceImpl realService = new PushServiceImpl(
                WebClient.builder().exchangeFunction(exchange), "http://gotify.test");
        ReflectionTestUtils.setField(realService, "auditLogService", auditLogService);
        ReflectionTestUtils.setField(realService, "gotifyToken", "token-real");

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> realService.sendAsync("user-9", "Alerta", Map.of()));

        verify(auditLogService).logDelivery(eq("user-9"), eq("PUSH"), eq("RETRY"), any());
    }

    @Test
    void shouldLogFailedOnRecover() {
        Exception ex = new RuntimeException("Push fail");
        var result = pushService.recover(ex, "user-123", "msg", Map.of());

        verify(auditLogService).logDelivery(eq("user-123"), eq("PUSH"), eq("FAILED"), eq(null));
        org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
    }
}
