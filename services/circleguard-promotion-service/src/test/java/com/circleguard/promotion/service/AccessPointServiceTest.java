package com.circleguard.promotion.service;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
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
class AccessPointServiceTest {

    @Mock
    private AccessPointRepository accessPointRepository;
    @Mock
    private FloorRepository floorRepository;

    private AccessPointService accessPointService;

    @BeforeEach
    void setUp() {
        accessPointService = new AccessPointService(accessPointRepository, floorRepository);
    }

    @Test
    void registerAccessPoint_ShouldAttachToFloor() {
        UUID floorId = UUID.randomUUID();
        Floor floor = Floor.builder().id(floorId).name("P1").build();
        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(accessPointRepository.save(any(AccessPoint.class))).thenAnswer(i -> i.getArguments()[0]);

        AccessPoint ap = accessPointService.registerAccessPoint(
                floorId, "AA:BB:CC:DD:EE:FF", 10.5, 20.5, "AP-Biblioteca");

        assertEquals(floor, ap.getFloor());
        assertEquals("AA:BB:CC:DD:EE:FF", ap.getMacAddress());
        assertEquals(10.5, ap.getCoordinateX());
        assertEquals(20.5, ap.getCoordinateY());
        assertEquals("AP-Biblioteca", ap.getName());
    }

    @Test
    void registerAccessPoint_ShouldThrowWhenFloorMissing() {
        UUID floorId = UUID.randomUUID();
        when(floorRepository.findById(floorId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accessPointService.registerAccessPoint(floorId, "mac", 0.0, 0.0, "AP"));
        verify(accessPointRepository, never()).save(any());
    }

    @Test
    void getAccessPoint_ShouldDelegateToRepository() {
        UUID id = UUID.randomUUID();
        AccessPoint ap = AccessPoint.builder().id(id).build();
        when(accessPointRepository.findById(id)).thenReturn(Optional.of(ap));

        assertEquals(Optional.of(ap), accessPointService.getAccessPoint(id));
    }

    @Test
    void getAccessPointsByFloor_ShouldDelegateToRepository() {
        UUID floorId = UUID.randomUUID();
        List<AccessPoint> aps = List.of(AccessPoint.builder().build());
        when(accessPointRepository.findByFloorId(floorId)).thenReturn(aps);

        assertEquals(aps, accessPointService.getAccessPointsByFloor(floorId));
    }

    @Test
    void updateAccessPoint_ShouldOverwriteFields() {
        UUID id = UUID.randomUUID();
        AccessPoint ap = AccessPoint.builder().id(id).macAddress("vieja").build();
        when(accessPointRepository.findById(id)).thenReturn(Optional.of(ap));
        when(accessPointRepository.save(any(AccessPoint.class))).thenAnswer(i -> i.getArguments()[0]);

        AccessPoint result = accessPointService.updateAccessPoint(id, "nueva-mac", 1.0, 2.0, "AP-2");

        assertEquals("nueva-mac", result.getMacAddress());
        assertEquals(1.0, result.getCoordinateX());
        assertEquals(2.0, result.getCoordinateY());
        assertEquals("AP-2", result.getName());
    }

    @Test
    void updateAccessPoint_ShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accessPointService.updateAccessPoint(id, "m", 0.0, 0.0, "n"));
    }

    @Test
    void deleteAccessPoint_ShouldDelegateToRepository() {
        UUID id = UUID.randomUUID();

        accessPointService.deleteAccessPoint(id);

        verify(accessPointRepository).deleteById(id);
    }
}
