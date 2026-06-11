package com.circleguard.promotion.controller;

import com.circleguard.promotion.service.LocationResolutionService;
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
class LocationSignalControllerTest {

    @Mock
    private LocationResolutionService locationResolutionService;

    private LocationSignalController controller;

    @BeforeEach
    void setUp() {
        controller = new LocationSignalController(locationResolutionService);
    }

    @Test
    void receiveSignal_WithValidData_ShouldReturnOk() {
        Map<String, Object> request = new HashMap<>();
        request.put("apMac", "AA:BB:CC:DD:EE:FF");
        request.put("deviceMac", "11:22:33:44:55:66");
        request.put("rssi", -65);

        ResponseEntity<Void> response = controller.receiveSignal(request);

        assertEquals(200, response.getStatusCodeValue());
        verify(locationResolutionService).processSignal("AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66", -65.0);
    }

    @Test
    void receiveSignal_WhenApMacMissing_ShouldReturnBadRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("deviceMac", "11:22:33:44:55:66");
        request.put("rssi", -65);

        ResponseEntity<Void> response = controller.receiveSignal(request);

        assertEquals(400, response.getStatusCodeValue());
        verifyNoInteractions(locationResolutionService);
    }

    @Test
    void receiveSignal_WhenDeviceMacMissing_ShouldReturnBadRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("apMac", "AA:BB:CC:DD:EE:FF");
        request.put("rssi", -65);

        ResponseEntity<Void> response = controller.receiveSignal(request);

        assertEquals(400, response.getStatusCodeValue());
        verifyNoInteractions(locationResolutionService);
    }
}
