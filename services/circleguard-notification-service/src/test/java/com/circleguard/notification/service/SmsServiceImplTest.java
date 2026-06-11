package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@SpringBootTest
class SmsServiceImplTest {

    @Autowired
    private SmsService smsService;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    void shouldSendSms() {
        smsService.sendAsync("user-123", "Test message");

        verify(auditLogService).logDelivery(eq("user-123"), eq("SMS"), eq("SUCCESS"), any());
    }

    @Test
    void shouldHandleTwilioException() {
        SmsServiceImpl impl = (SmsServiceImpl) smsService;
        ReflectionTestUtils.setField(impl, "accountSid", "AC_REAL_SID");

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                smsService.sendAsync("user-123", "message").join());

        verify(auditLogService).logDelivery(eq("user-123"), eq("SMS"), eq("RETRY"), any());
    }
}
