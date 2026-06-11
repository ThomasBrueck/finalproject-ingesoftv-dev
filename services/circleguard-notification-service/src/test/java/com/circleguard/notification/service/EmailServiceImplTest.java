package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

@SpringBootTest
class EmailServiceImplTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    void shouldSendEmail() {
        emailService.sendAsync("user-123", "Health alert message").join();

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(auditLogService).logDelivery(eq("user-123"), eq("EMAIL"), eq("SUCCESS"), any());
    }

    @Test
    void shouldHandleMailException() {
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                emailService.sendAsync("user-123", "message").join());

        verify(auditLogService, times(3)).logDelivery(eq("user-123"), eq("EMAIL"), eq("RETRY"), any());
    }
}
