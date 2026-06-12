package com.circleguard.promotion.service;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
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
class SpatialServiceTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private FloorRepository floorRepository;
    @Mock
    private AccessPointRepository accessPointRepository;

    private SpatialService spatialService;

    @BeforeEach
    void setUp() {
        spatialService = new SpatialService(buildingRepository, floorRepository, accessPointRepository);
    }

    @Test
    void createBuilding_ShouldSaveWithGivenFields() {
        when(buildingRepository.save(any(Building.class))).thenAnswer(i -> i.getArguments()[0]);

        Building result = spatialService.createBuilding("Bloque A", "BA", "Edificio principal");

        assertEquals("Bloque A", result.getName());
        assertEquals("BA", result.getCode());
        assertEquals("Edificio principal", result.getDescription());
        verify(buildingRepository).save(any(Building.class));
    }

    @Test
    void addFloor_ShouldAttachFloorToBuilding() {
        UUID buildingId = UUID.randomUUID();
        Building building = Building.builder().id(buildingId).name("Bloque A").build();
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(floorRepository.save(any(Floor.class))).thenAnswer(i -> i.getArguments()[0]);

        Floor result = spatialService.addFloor(buildingId, 2, "Piso 2");

        assertEquals(building, result.getBuilding());
        assertEquals(2, result.getFloorNumber());
        assertEquals("Piso 2", result.getName());
    }

    @Test
    void addFloor_ShouldThrowWhenBuildingMissing() {
        UUID buildingId = UUID.randomUUID();
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> spatialService.addFloor(buildingId, 1, "Piso 1"));
        verify(floorRepository, never()).save(any());
    }

    @Test
    void getAllBuildings_ShouldDelegateToRepository() {
        List<Building> buildings = List.of(Building.builder().name("A").build());
        when(buildingRepository.findAll()).thenReturn(buildings);

        assertEquals(buildings, spatialService.getAllBuildings());
    }

    @Test
    void getFloorsByBuilding_ShouldDelegateToRepository() {
        UUID buildingId = UUID.randomUUID();
        List<Floor> floors = List.of(Floor.builder().name("P1").build());
        when(floorRepository.findByBuildingId(buildingId)).thenReturn(floors);

        assertEquals(floors, spatialService.getFloorsByBuilding(buildingId));
    }

    @Test
    void updateBuilding_ShouldOverwriteFields() {
        UUID id = UUID.randomUUID();
        Building building = Building.builder().id(id).name("Viejo").code("V").description("v").build();
        when(buildingRepository.findById(id)).thenReturn(Optional.of(building));
        when(buildingRepository.save(any(Building.class))).thenAnswer(i -> i.getArguments()[0]);

        Building result = spatialService.updateBuilding(id, "Nuevo", "N", "desc nueva");

        assertEquals("Nuevo", result.getName());
        assertEquals("N", result.getCode());
        assertEquals("desc nueva", result.getDescription());
    }

    @Test
    void updateBuilding_ShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(buildingRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> spatialService.updateBuilding(id, "n", "c", "d"));
    }

    @Test
    void deleteBuilding_ShouldDeleteWhenNoFloors() {
        UUID id = UUID.randomUUID();
        when(floorRepository.findByBuildingId(id)).thenReturn(List.of());

        spatialService.deleteBuilding(id);

        verify(buildingRepository).deleteById(id);
    }

    @Test
    void deleteBuilding_ShouldRejectWhenFloorsExist() {
        UUID id = UUID.randomUUID();
        when(floorRepository.findByBuildingId(id)).thenReturn(List.of(Floor.builder().build()));

        assertThrows(RuntimeException.class, () -> spatialService.deleteBuilding(id));
        verify(buildingRepository, never()).deleteById(any());
    }

    @Test
    void updateFloor_ShouldOverwriteFields() {
        UUID id = UUID.randomUUID();
        Floor floor = Floor.builder().id(id).floorNumber(1).name("P1").build();
        when(floorRepository.findById(id)).thenReturn(Optional.of(floor));
        when(floorRepository.save(any(Floor.class))).thenAnswer(i -> i.getArguments()[0]);

        Floor result = spatialService.updateFloor(id, 3, "Piso 3");

        assertEquals(3, result.getFloorNumber());
        assertEquals("Piso 3", result.getName());
    }

    @Test
    void updateFloor_ShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(floorRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> spatialService.updateFloor(id, 1, "P"));
    }

    @Test
    void deleteFloor_ShouldDeleteWhenNoAccessPoints() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findByFloorId(id)).thenReturn(List.of());

        spatialService.deleteFloor(id);

        verify(floorRepository).deleteById(id);
    }

    @Test
    void deleteFloor_ShouldRejectWhenAccessPointsExist() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findByFloorId(id)).thenReturn(List.of(AccessPoint.builder().build()));

        assertThrows(RuntimeException.class, () -> spatialService.deleteFloor(id));
        verify(floorRepository, never()).deleteById(any());
    }

    @Test
    void registerAccessPoint_ShouldAttachToFloor() {
        UUID floorId = UUID.randomUUID();
        Floor floor = Floor.builder().id(floorId).name("P1").build();
        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(accessPointRepository.save(any(AccessPoint.class))).thenAnswer(i -> i.getArguments()[0]);

        AccessPoint ap = spatialService.registerAccessPoint(floorId, "AA:BB:CC:DD:EE:FF", 1.0, 2.0, "AP-1");

        assertEquals(floor, ap.getFloor());
        assertEquals("AA:BB:CC:DD:EE:FF", ap.getMacAddress());
        assertEquals(1.0, ap.getCoordinateX());
        assertEquals(2.0, ap.getCoordinateY());
        assertEquals("AP-1", ap.getName());
    }

    @Test
    void registerAccessPoint_ShouldThrowWhenFloorMissing() {
        UUID floorId = UUID.randomUUID();
        when(floorRepository.findById(floorId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> spatialService.registerAccessPoint(floorId, "mac", 0.0, 0.0, "AP"));
    }

    @Test
    void getAccessPoint_ShouldDelegateToRepository() {
        UUID id = UUID.randomUUID();
        AccessPoint ap = AccessPoint.builder().id(id).build();
        when(accessPointRepository.findById(id)).thenReturn(Optional.of(ap));

        assertEquals(Optional.of(ap), spatialService.getAccessPoint(id));
    }

    @Test
    void getAccessPointsByFloor_ShouldDelegateToRepository() {
        UUID floorId = UUID.randomUUID();
        List<AccessPoint> aps = List.of(AccessPoint.builder().build());
        when(accessPointRepository.findByFloorId(floorId)).thenReturn(aps);

        assertEquals(aps, spatialService.getAccessPointsByFloor(floorId));
    }

    @Test
    void updateAccessPoint_ShouldOverwriteFields() {
        UUID id = UUID.randomUUID();
        AccessPoint ap = AccessPoint.builder().id(id).macAddress("old").build();
        when(accessPointRepository.findById(id)).thenReturn(Optional.of(ap));
        when(accessPointRepository.save(any(AccessPoint.class))).thenAnswer(i -> i.getArguments()[0]);

        AccessPoint result = spatialService.updateAccessPoint(id, "new-mac", 5.0, 6.0, "AP-2");

        assertEquals("new-mac", result.getMacAddress());
        assertEquals(5.0, result.getCoordinateX());
        assertEquals(6.0, result.getCoordinateY());
        assertEquals("AP-2", result.getName());
    }

    @Test
    void updateAccessPoint_ShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> spatialService.updateAccessPoint(id, "m", 0.0, 0.0, "n"));
    }

    @Test
    void deleteAccessPoint_ShouldDelegateToRepository() {
        UUID id = UUID.randomUUID();

        spatialService.deleteAccessPoint(id);

        verify(accessPointRepository).deleteById(id);
    }
}
