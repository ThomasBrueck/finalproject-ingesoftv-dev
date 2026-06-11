package com.circleguard.promotion.controller;

import com.circleguard.promotion.dto.AccessPointDTO;
import com.circleguard.promotion.dto.FloorDTO;
import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.service.AccessPointService;
import com.circleguard.promotion.service.FloorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloorControllerTest {

    @Mock
    private FloorService floorService;

    @Mock
    private AccessPointService accessPointService;

    private FloorController controller;

    @BeforeEach
    void setUp() {
        controller = new FloorController(floorService, accessPointService);
    }

    @Test
    void addAccessPoint_ShouldReturnCreated() {
        UUID floorId = UUID.randomUUID();
        AccessPointDTO request = AccessPointDTO.builder()
                .macAddress("AA:BB:CC:DD:EE:FF")
                .coordinateX(10.5)
                .coordinateY(20.3)
                .name("AP-1")
                .build();
        AccessPoint saved = AccessPoint.builder()
                .id(UUID.randomUUID())
                .macAddress("AA:BB:CC:DD:EE:FF")
                .coordinateX(10.5)
                .coordinateY(20.3)
                .name("AP-1")
                .floor(Floor.builder().id(floorId).build())
                .build();
        when(accessPointService.registerAccessPoint(floorId, "AA:BB:CC:DD:EE:FF", 10.5, 20.3, "AP-1"))
                .thenReturn(saved);

        ResponseEntity<AccessPointDTO> response = controller.addAccessPoint(floorId, request);

        assertNotNull(response.getBody());
        assertEquals("AA:BB:CC:DD:EE:FF", response.getBody().getMacAddress());
        verify(accessPointService).registerAccessPoint(floorId, "AA:BB:CC:DD:EE:FF", 10.5, 20.3, "AP-1");
    }

    @Test
    void getAccessPointsByFloor_ShouldReturnList() {
        UUID floorId = UUID.randomUUID();
        AccessPoint ap = AccessPoint.builder()
                .id(UUID.randomUUID())
                .macAddress("11:22:33:44:55:66")
                .coordinateX(1.0)
                .coordinateY(2.0)
                .name("AP-2")
                .floor(Floor.builder().id(floorId).build())
                .build();
        when(accessPointService.getAccessPointsByFloor(floorId)).thenReturn(List.of(ap));

        ResponseEntity<List<AccessPointDTO>> response = controller.getAccessPoints(floorId);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("11:22:33:44:55:66", response.getBody().get(0).getMacAddress());
        verify(accessPointService).getAccessPointsByFloor(floorId);
    }

    @Test
    void updateFloor_ShouldReturnUpdated() {
        UUID floorId = UUID.randomUUID();
        FloorDTO request = FloorDTO.builder()
                .floorNumber(3)
                .name("Third Floor")
                .floorPlanUrl("http://plan.url/3")
                .build();
        Floor updated = Floor.builder()
                .id(floorId)
                .floorNumber(3)
                .name("Third Floor")
                .floorPlanUrl("http://plan.url/3")
                .building(Building.builder().id(UUID.randomUUID()).build())
                .build();
        when(floorService.updateFloor(floorId, 3, "Third Floor", "http://plan.url/3"))
                .thenReturn(updated);

        ResponseEntity<FloorDTO> response = controller.updateFloor(floorId, request);

        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getFloorNumber());
        assertEquals("Third Floor", response.getBody().getName());
        verify(floorService).updateFloor(floorId, 3, "Third Floor", "http://plan.url/3");
    }

    @Test
    void deleteFloor_ShouldReturnOk() {
        UUID floorId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteFloor(floorId);

        assertEquals(200, response.getStatusCodeValue());
        verify(floorService).deleteFloor(floorId);
    }
}
