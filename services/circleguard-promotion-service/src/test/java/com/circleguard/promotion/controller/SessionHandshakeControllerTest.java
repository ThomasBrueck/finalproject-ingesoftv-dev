package com.circleguard.promotion.controller;

import com.circleguard.promotion.service.MacSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionHandshakeControllerTest {

    @Mock
    private MacSessionRegistry sessionRegistry;

    private SessionHandshakeController controller;

    @BeforeEach
    void setUp() {
        controller = new SessionHandshakeController(sessionRegistry);
    }

    @Test
    void handshake_WithValidParams_ShouldReturnOk() {
        Map<String, String> request = new HashMap<>();
        request.put("macAddress", "AA:BB:CC:DD:EE:FF");
        request.put("anonymousId", "anon-123");

        ResponseEntity<Void> response = controller.handshake(request);

        assertEquals(200, response.getStatusCodeValue());
        verify(sessionRegistry).registerSession("AA:BB:CC:DD:EE:FF", "anon-123");
    }

    @Test
    void handshake_WhenMacAddressMissing_ShouldReturnBadRequest() {
        Map<String, String> request = new HashMap<>();
        request.put("anonymousId", "anon-123");

        ResponseEntity<Void> response = controller.handshake(request);

        assertEquals(400, response.getStatusCodeValue());
        verifyNoInteractions(sessionRegistry);
    }

    @Test
    void handshake_WhenAnonymousIdMissing_ShouldReturnBadRequest() {
        Map<String, String> request = new HashMap<>();
        request.put("macAddress", "AA:BB:CC:DD:EE:FF");

        ResponseEntity<Void> response = controller.handshake(request);

        assertEquals(400, response.getStatusCodeValue());
        verifyNoInteractions(sessionRegistry);
    }

    @Test
    void closeSession_ShouldReturnNoContent() {
        String macAddress = "AA:BB:CC:DD:EE:FF";

        ResponseEntity<Void> response = controller.closeSession(macAddress);

        assertEquals(204, response.getStatusCodeValue());
        verify(sessionRegistry).closeSession(macAddress);
    }
}
