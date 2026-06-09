package com.circleguard.auth.controller;

import com.circleguard.auth.service.QrTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(QrTokenController.class)
@TestPropertySource(properties = {
    "jwt.secret=test-jwt-secret-that-is-at-least-32-characters-long-for-hmac",
    "jwt.expiration=3600000",
    "qr.secret=my-qr-secret-key-for-tests-1234567890ab"
})
class QrTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrTokenService qrService;

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void shouldGenerateQrToken() throws Exception {
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(qrService.generateQrToken(anonymousId)).thenReturn("mock-qr-token");

        mockMvc.perform(get("/api/v1/auth/qr/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrToken").value("mock-qr-token"))
                .andExpect(jsonPath("$.expiresIn").value("60"));

        verify(qrService).generateQrToken(anonymousId);
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/qr/generate"))
                .andExpect(status().isUnauthorized());
    }
}
