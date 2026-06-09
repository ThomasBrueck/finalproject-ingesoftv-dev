package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SmsServiceImpl smsService;

    @Test
    void shouldSendMockSmsAndLogSuccess() {
        ReflectionTestUtils.setField(smsService, "accountSid", "AC_MOCK_SID");

        smsService.sendAsync("user-123", "Test message");

        verify(auditLogService).logDelivery(eq("user-123"), eq("SMS"), eq("SUCCESS"), any());
    }

    @Test
    void shouldLogFailedOnRecover() {
        Exception ex = new RuntimeException("SMS fail");
        var result = smsService.recover(ex, "user-123", "msg");

        verify(auditLogService).logDelivery(eq("user-123"), eq("SMS"), eq("FAILED"), eq(null));
        org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
    }
}
