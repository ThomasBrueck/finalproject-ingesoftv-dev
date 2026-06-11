package com.circleguard.promotion.controller;

import com.circleguard.promotion.dto.AccessPointDTO;
import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.service.AccessPointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessPointControllerTest {

    @Mock
    private AccessPointService accessPointService;

    private AccessPointController controller;

    @BeforeEach
    void setUp() {
        controller = new AccessPointController(accessPointService);
    }

    @Test
    void getAccessPoint_WhenFound_ShouldReturnOk() {
        UUID id = UUID.randomUUID();
        AccessPoint ap = AccessPoint.builder()
                .id(id)
                .macAddress("AA:BB:CC:DD:EE:FF")
                .coordinateX(1.5)
                .coordinateY(2.5)
                .name("AP-Main")
                .floor(Floor.builder().id(UUID.randomUUID()).build())
                .build();
        when(accessPointService.getAccessPoint(id)).thenReturn(Optional.of(ap));

        ResponseEntity<AccessPointDTO> response = controller.getAccessPoint(id);

        assertNotNull(response.getBody());
        assertEquals("AA:BB:CC:DD:EE:FF", response.getBody().getMacAddress());
        verify(accessPointService).getAccessPoint(id);
    }

    @Test
    void getAccessPoint_WhenNotFound_ShouldReturn404() {
        UUID id = UUID.randomUUID();
        when(accessPointService.getAccessPoint(id)).thenReturn(Optional.empty());

        ResponseEntity<AccessPointDTO> response = controller.getAccessPoint(id);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
        verify(accessPointService).getAccessPoint(id);
    }

    @Test
    void updateAccessPoint_ShouldReturnUpdated() {
        UUID id = UUID.randomUUID();
        AccessPointDTO request = AccessPointDTO.builder()
                .macAddress("11:22:33:44:55:66")
                .coordinateX(3.0)
                .coordinateY(4.0)
                .name("AP-Updated")
                .build();
        AccessPoint updated = AccessPoint.builder()
                .id(id)
                .macAddress("11:22:33:44:55:66")
                .coordinateX(3.0)
                .coordinateY(4.0)
                .name("AP-Updated")
                .floor(Floor.builder().id(UUID.randomUUID()).build())
                .build();
        when(accessPointService.updateAccessPoint(id, "11:22:33:44:55:66", 3.0, 4.0, "AP-Updated"))
                .thenReturn(updated);

        ResponseEntity<AccessPointDTO> response = controller.updateAccessPoint(id, request);

        assertNotNull(response.getBody());
        assertEquals("11:22:33:44:55:66", response.getBody().getMacAddress());
        verify(accessPointService).updateAccessPoint(id, "11:22:33:44:55:66", 3.0, 4.0, "AP-Updated");
    }

    @Test
    void deleteAccessPoint_ShouldReturnOk() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteAccessPoint(id);

        assertEquals(200, response.getStatusCodeValue());
        verify(accessPointService).deleteAccessPoint(id);
    }
}
