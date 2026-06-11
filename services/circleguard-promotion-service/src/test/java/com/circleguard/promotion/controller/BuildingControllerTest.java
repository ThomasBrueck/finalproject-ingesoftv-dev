package com.circleguard.promotion.controller;

import com.circleguard.promotion.dto.BuildingDTO;
import com.circleguard.promotion.dto.FloorDTO;
import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.service.BuildingService;
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
class BuildingControllerTest {

    @Mock
    private BuildingService buildingService;

    @Mock
    private FloorService floorService;

    private BuildingController controller;

    @BeforeEach
    void setUp() {
        controller = new BuildingController(buildingService, floorService);
    }

    @Test
    void createBuilding_ShouldReturnCreated() {
        BuildingDTO request = BuildingDTO.builder()
                .name("Test Building")
                .code("TB-001")
                .description("A test")
                .latitude(40.7128)
                .longitude(-74.0060)
                .address("123 Test St")
                .build();
        UUID id = UUID.randomUUID();
        Building building = Building.builder()
                .id(id)
                .name("Test Building")
                .code("TB-001")
                .description("A test")
                .latitude(40.7128)
                .longitude(-74.0060)
                .address("123 Test St")
                .build();
        when(buildingService.createBuilding("Test Building", "TB-001", "A test", 40.7128, -74.0060, "123 Test St"))
                .thenReturn(building);

        ResponseEntity<BuildingDTO> response = controller.createBuilding(request);

        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
        assertEquals("Test Building", response.getBody().getName());
        verify(buildingService).createBuilding("Test Building", "TB-001", "A test", 40.7128, -74.0060, "123 Test St");
    }

    @Test
    void listBuildings_ShouldReturnAll() {
        Building b1 = Building.builder().id(UUID.randomUUID()).name("B1").code("B1").build();
        Building b2 = Building.builder().id(UUID.randomUUID()).name("B2").code("B2").build();
        when(buildingService.getAllBuildings()).thenReturn(List.of(b1, b2));

        ResponseEntity<List<BuildingDTO>> response = controller.listBuildings();

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(buildingService).getAllBuildings();
    }

    @Test
    void updateBuilding_ShouldReturnUpdated() {
        UUID id = UUID.randomUUID();
        BuildingDTO request = BuildingDTO.builder()
                .name("Updated")
                .code("UP-001")
                .description("Updated desc")
                .latitude(10.0)
                .longitude(20.0)
                .address("456 New St")
                .build();
        Building updated = Building.builder()
                .id(id)
                .name("Updated")
                .code("UP-001")
                .description("Updated desc")
                .latitude(10.0)
                .longitude(20.0)
                .address("456 New St")
                .build();
        when(buildingService.updateBuilding(id, "Updated", "UP-001", "Updated desc", 10.0, 20.0, "456 New St"))
                .thenReturn(updated);

        ResponseEntity<BuildingDTO> response = controller.updateBuilding(id, request);

        assertNotNull(response.getBody());
        assertEquals("Updated", response.getBody().getName());
        verify(buildingService).updateBuilding(id, "Updated", "UP-001", "Updated desc", 10.0, 20.0, "456 New St");
    }

    @Test
    void deleteBuilding_ShouldReturnOk() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteBuilding(id);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        verify(buildingService).deleteBuilding(id);
    }

    @Test
    void getFloors_ShouldReturnList() {
        UUID buildingId = UUID.randomUUID();
        Floor floor = Floor.builder()
                .id(UUID.randomUUID())
                .floorNumber(1)
                .name("Floor 1")
                .floorPlanUrl("http://plan.url")
                .building(Building.builder().id(buildingId).build())
                .build();
        when(floorService.getFloorsByBuilding(buildingId)).thenReturn(List.of(floor));

        ResponseEntity<List<FloorDTO>> response = controller.getFloors(buildingId);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1, response.getBody().get(0).getFloorNumber());
        verify(floorService).getFloorsByBuilding(buildingId);
    }

    @Test
    void addFloor_ShouldReturnCreated() {
        UUID buildingId = UUID.randomUUID();
        FloorDTO request = FloorDTO.builder()
                .floorNumber(2)
                .name("Floor 2")
                .build();
        Floor saved = Floor.builder()
                .id(UUID.randomUUID())
                .floorNumber(2)
                .name("Floor 2")
                .building(Building.builder().id(buildingId).build())
                .build();
        when(floorService.addFloor(buildingId, 2, "Floor 2")).thenReturn(saved);

        ResponseEntity<FloorDTO> response = controller.addFloor(buildingId, request);

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getFloorNumber());
        verify(floorService).addFloor(buildingId, 2, "Floor 2");
    }
}
