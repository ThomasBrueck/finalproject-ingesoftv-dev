package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void shouldSendEmailAndLogSuccess() {
        emailService.sendAsync("user-123", "Health alert message");

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(auditLogService).logDelivery(eq("user-123"), eq("EMAIL"), eq("SUCCESS"), any());
    }

    @Test
    void shouldLogRetryOnFailure() {
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            emailService.sendAsync("user-123", "message").join();
        });

        verify(auditLogService).logDelivery(eq("user-123"), eq("EMAIL"), eq("RETRY"), any());
    }

    @Test
    void shouldLogFailedOnRecover() {
        Exception ex = new RuntimeException("fail");
        var result = emailService.recover(ex, "user-123", "msg");

        verify(auditLogService).logDelivery(eq("user-123"), eq("EMAIL"), eq("FAILED"), eq(null));
        org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
    }
}
