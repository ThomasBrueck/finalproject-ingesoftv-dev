package com.circleguard.promotion.service;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationResolutionServiceTest {

    @Mock
    private AccessPointRepository accessPointRepository;
    @Mock
    private MacSessionRegistry sessionRegistry;
    @Mock
    private GraphService graphService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;

    private LocationResolutionService locationResolutionService;

    private UUID buildingId;
    private UUID floorId;
    private UUID apId;
    private Building building;
    private Floor floor;
    private AccessPoint accessPoint;
    private String apMac;
    private String deviceMac;
    private String anonymousId;
    private Double rssi;

    @BeforeEach
    void setUp() {
        locationResolutionService = new LocationResolutionService(
                accessPointRepository, sessionRegistry, graphService,
                kafkaTemplate, redisTemplate);

        buildingId = UUID.randomUUID();
        floorId = UUID.randomUUID();
        apId = UUID.randomUUID();
        apMac = "aa:bb:cc:dd:ee:ff";
        deviceMac = "11:22:33:44:55:66";
        anonymousId = "anon-user-123";
        rssi = -65.0;

        building = Building.builder()
                .id(buildingId)
                .name("Main Building")
                .build();

        floor = Floor.builder()
                .id(floorId)
                .building(building)
                .floorNumber(1)
                .name("Floor 1")
                .build();

        accessPoint = AccessPoint.builder()
                .id(apId)
                .macAddress(apMac)
                .floor(floor)
                .coordinateX(10.0)
                .coordinateY(20.0)
                .name("AP-1")
                .build();
    }

    @Test
    void processSignal_withKnownMacUser_producesProximityDetectedEvent() {
        when(accessPointRepository.findByMacAddress(apMac)).thenReturn(Optional.of(accessPoint));
        when(sessionRegistry.getAnonymousId(deviceMac)).thenReturn(anonymousId);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(anyString())).thenReturn(Set.of("other-user"));
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);

        locationResolutionService.processSignal(apMac, deviceMac, rssi);

        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq("proximity.detected"), eq(anonymousId), eventCaptor.capture());

        Map<String, Object> event = eventCaptor.getValue();
        assertEquals(anonymousId, event.get("anonymousId"));
        assertEquals(buildingId, event.get("buildingId"));
        assertEquals(floorId, event.get("floorId"));
        assertEquals(rssi, event.get("rssi"));
        assertNotNull(event.get("timestamp"));

        Map<String, Object> coords = (Map<String, Object>) event.get("coordinates");
        assertEquals(10.0, coords.get("x"));
        assertEquals(20.0, coords.get("y"));

        verify(graphService).recordEncounter(eq(anonymousId), anyString(), eq(apId.toString()));
        verify(graphService).detectAndFormCircles(apId.toString());
    }

    @Test
    void processSignal_withUnknownAccessPoint_noEventProduced() {
        when(accessPointRepository.findByMacAddress(apMac)).thenReturn(Optional.empty());

        locationResolutionService.processSignal(apMac, deviceMac, rssi);

        verifyNoInteractions(sessionRegistry, kafkaTemplate, graphService);
    }

    @Test
    void processSignal_withUnknownMac_noEventProduced() {
        when(accessPointRepository.findByMacAddress(apMac)).thenReturn(Optional.of(accessPoint));
        when(sessionRegistry.getAnonymousId(deviceMac)).thenReturn(null);

        locationResolutionService.processSignal(apMac, deviceMac, rssi);

        verify(sessionRegistry).getAnonymousId(deviceMac);
        verifyNoInteractions(kafkaTemplate, graphService);
    }

    @Test
    void processSignal_withNullApMac_doesNotThrow() {
        assertDoesNotThrow(() -> locationResolutionService.processSignal(null, deviceMac, rssi));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void processSignal_withNullDeviceMac_doesNotThrow() {
        assertDoesNotThrow(() -> locationResolutionService.processSignal(apMac, null, rssi));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void processSignal_withEmptySignals_doesNotThrow() {
        assertDoesNotThrow(() -> locationResolutionService.processSignal("", "", rssi));
        verifyNoInteractions(kafkaTemplate);
    }
}
