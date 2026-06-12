package com.circleguard.promotion.service;

import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
import com.circleguard.promotion.repository.jpa.BuildingRepository;
import com.circleguard.promotion.repository.jpa.FloorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloorServiceTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private FloorRepository floorRepository;
    @Mock
    private AccessPointRepository accessPointRepository;

    private FloorService floorService;

    @BeforeEach
    void setUp() {
        floorService = new FloorService(buildingRepository, floorRepository, accessPointRepository);
    }

    @Test
    void updateFloor_ShouldUpdateFloorPlanUrl() {
        // Arrange
        UUID floorId = UUID.randomUUID();
        String oldUrl = "http://old.url";
        String newUrl = "http://new.url";
        
        Floor floor = Floor.builder()
                .id(floorId)
                .name("Level 1")
                .floorNumber(1)
                .floorPlanUrl(oldUrl)
                .build();

        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(floorRepository.save(any(Floor.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Floor updated = floorService.updateFloor(floorId, null, null, newUrl);

        // Assert
        assertEquals(newUrl, updated.getFloorPlanUrl());
        assertEquals("Level 1", updated.getName()); // Should remain unchanged
        verify(floorRepository).save(floor);
    }

    @Test
    void updateFloor_ShouldNotUpdateIfUrlIsNull() {
        // Arrange
        UUID floorId = UUID.randomUUID();
        String oldUrl = "http://old.url";
        
        Floor floor = Floor.builder()
                .id(floorId)
                .floorPlanUrl(oldUrl)
                .build();

        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(floorRepository.save(any(Floor.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Floor updated = floorService.updateFloor(floorId, null, null, null);

        // Assert
        assertEquals(oldUrl, updated.getFloorPlanUrl());
        verify(floorRepository).save(floor);
    }

    @Test
    void updateFloor_ShouldThrowWhenMissing() {
        UUID floorId = UUID.randomUUID();
        when(floorRepository.findById(floorId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> floorService.updateFloor(floorId, 1, "P1", null));
        verify(floorRepository, never()).save(any());
    }

    @Test
    void addFloor_ShouldAttachFloorToBuilding() {
        UUID buildingId = UUID.randomUUID();
        com.circleguard.promotion.model.Building building =
                com.circleguard.promotion.model.Building.builder().id(buildingId).name("Bloque A").build();
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(floorRepository.save(any(Floor.class))).thenAnswer(i -> i.getArguments()[0]);

        Floor result = floorService.addFloor(buildingId, 4, "Piso 4");

        assertEquals(building, result.getBuilding());
        assertEquals(4, result.getFloorNumber());
        assertEquals("Piso 4", result.getName());
    }

    @Test
    void addFloor_ShouldThrowWhenBuildingMissing() {
        UUID buildingId = UUID.randomUUID();
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> floorService.addFloor(buildingId, 1, "P1"));
        verify(floorRepository, never()).save(any());
    }

    @Test
    void getFloorsByBuilding_ShouldDelegateToRepository() {
        UUID buildingId = UUID.randomUUID();
        java.util.List<Floor> floors = java.util.List.of(Floor.builder().name("P1").build());
        when(floorRepository.findByBuildingId(buildingId)).thenReturn(floors);

        assertEquals(floors, floorService.getFloorsByBuilding(buildingId));
    }

    @Test
    void deleteFloor_ShouldDeleteWhenNoAccessPoints() {
        UUID floorId = UUID.randomUUID();
        when(accessPointRepository.findByFloorId(floorId)).thenReturn(java.util.List.of());

        floorService.deleteFloor(floorId);

        verify(floorRepository).deleteById(floorId);
    }

    @Test
    void deleteFloor_ShouldRejectWhenAccessPointsExist() {
        UUID floorId = UUID.randomUUID();
        when(accessPointRepository.findByFloorId(floorId)).thenReturn(
                java.util.List.of(com.circleguard.promotion.model.AccessPoint.builder().build()));

        assertThrows(RuntimeException.class, () -> floorService.deleteFloor(floorId));
        verify(floorRepository, never()).deleteById(any());
    }
}
