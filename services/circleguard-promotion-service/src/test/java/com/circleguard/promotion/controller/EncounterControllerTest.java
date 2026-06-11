package com.circleguard.promotion.controller;

import com.circleguard.promotion.repository.graph.UserNodeRepository;
import com.circleguard.promotion.service.AutoCircleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncounterControllerTest {

    @Mock
    private UserNodeRepository userRepository;

    @Mock
    private AutoCircleService autoCircleService;

    private EncounterController controller;

    @BeforeEach
    void setUp() {
        controller = new EncounterController(userRepository, autoCircleService);
    }

    @Test
    void reportEncounter_ShouldRecordAndEvaluate() {
        EncounterController.EncounterRequest request = new EncounterController.EncounterRequest();
        request.setSourceId("src");
        request.setTargetId("tgt");
        request.setLocationId("loc-1");

        doNothing().when(userRepository).recordEncounter(anyString(), anyString(), anyLong(), anyString());
        doNothing().when(autoCircleService).evaluateEncounter(anyString(), anyString());

        ResponseEntity<Void> response = controller.reportEncounter(request);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(userRepository).recordEncounter(eq("src"), eq("tgt"), anyLong(), eq("loc-1"));
        verify(autoCircleService).evaluateEncounter("src", "tgt");
    }

    @Test
    void reportEncounter_WithNullLocation_ShouldUseDefaultMobileBle() {
        EncounterController.EncounterRequest request = new EncounterController.EncounterRequest();
        request.setSourceId("src");
        request.setTargetId("tgt");
        request.setLocationId(null);

        doNothing().when(userRepository).recordEncounter(anyString(), anyString(), anyLong(), anyString());
        doNothing().when(autoCircleService).evaluateEncounter(anyString(), anyString());

        controller.reportEncounter(request);

        verify(userRepository).recordEncounter(eq("src"), eq("tgt"), anyLong(), eq("mobile_ble"));
    }

    @Test
    void toggleValidity_ShouldReturnOk() {
        doNothing().when(userRepository).toggleEncounterValidity(1L);

        ResponseEntity<Void> response = controller.toggleValidity(1L);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(userRepository).toggleEncounterValidity(1L);
    }

    @Test
    void forceFence_ShouldReturnOk() {
        doNothing().when(userRepository).forceEncounterFence(1L);

        ResponseEntity<Void> response = controller.forceFence(1L);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(userRepository).forceEncounterFence(1L);
    }
}
