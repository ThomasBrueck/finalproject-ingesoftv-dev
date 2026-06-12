package com.circleguard.promotion.service;

import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.BuildingRepository;
import com.circleguard.promotion.repository.jpa.FloorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private FloorRepository floorRepository;

    private BuildingService buildingService;

    @BeforeEach
    void setUp() {
        buildingService = new BuildingService(buildingRepository, floorRepository);
    }

    @Test
    void createBuilding_ShouldSaveAllFields() {
        when(buildingRepository.save(any(Building.class))).thenAnswer(i -> i.getArguments()[0]);

        Building result = buildingService.createBuilding(
                "Bloque B", "BB", "Laboratorios", 4.6371, -74.0838, "Cra 7 #40-62");

        assertEquals("Bloque B", result.getName());
        assertEquals("BB", result.getCode());
        assertEquals("Laboratorios", result.getDescription());
        assertEquals(4.6371, result.getLatitude());
        assertEquals(-74.0838, result.getLongitude());
        assertEquals("Cra 7 #40-62", result.getAddress());
    }

    @Test
    void getAllBuildings_ShouldDelegateToRepository() {
        List<Building> buildings = List.of(Building.builder().name("A").build());
        when(buildingRepository.findAll()).thenReturn(buildings);

        assertEquals(buildings, buildingService.getAllBuildings());
    }

    @Test
    void updateBuilding_ShouldOverwriteAllFields() {
        UUID id = UUID.randomUUID();
        Building building = Building.builder().id(id).name("Viejo").build();
        when(buildingRepository.findById(id)).thenReturn(Optional.of(building));
        when(buildingRepository.save(any(Building.class))).thenAnswer(i -> i.getArguments()[0]);

        Building result = buildingService.updateBuilding(
                id, "Nuevo", "NV", "desc", 1.0, 2.0, "direccion");

        assertEquals("Nuevo", result.getName());
        assertEquals("NV", result.getCode());
        assertEquals("desc", result.getDescription());
        assertEquals(1.0, result.getLatitude());
        assertEquals(2.0, result.getLongitude());
        assertEquals("direccion", result.getAddress());
    }

    @Test
    void updateBuilding_ShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(buildingRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> buildingService.updateBuilding(id, "n", "c", "d", 0.0, 0.0, "a"));
        verify(buildingRepository, never()).save(any());
    }

    @Test
    void deleteBuilding_ShouldDeleteWhenNoFloors() {
        UUID id = UUID.randomUUID();
        when(floorRepository.findByBuildingId(id)).thenReturn(List.of());

        buildingService.deleteBuilding(id);

        verify(buildingRepository).deleteById(id);
    }

    @Test
    void deleteBuilding_ShouldRejectWhenFloorsExist() {
        UUID id = UUID.randomUUID();
        when(floorRepository.findByBuildingId(id)).thenReturn(List.of(Floor.builder().build()));

        assertThrows(RuntimeException.class, () -> buildingService.deleteBuilding(id));
        verify(buildingRepository, never()).deleteById(any());
    }
}
