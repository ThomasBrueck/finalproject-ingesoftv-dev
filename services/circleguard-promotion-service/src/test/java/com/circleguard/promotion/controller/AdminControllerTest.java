package com.circleguard.promotion.controller;

import com.circleguard.promotion.model.jpa.SystemSettings;
import com.circleguard.promotion.repository.jpa.SystemSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private SystemSettingsRepository settingsRepository;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(settingsRepository);
    }

    @Test
    void getSettings_ShouldReturnExisting() {
        SystemSettings existing = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .autoThresholdSeconds(3600L)
                .mandatoryFenceDays(14)
                .encounterWindowDays(14)
                .build();
        when(settingsRepository.getSettings()).thenReturn(Optional.of(existing));

        ResponseEntity<SystemSettings> response = controller.getSettings();

        assertNotNull(response.getBody());
        assertTrue(response.getBody().getUnconfirmedFencingEnabled());
        assertEquals(3600L, response.getBody().getAutoThresholdSeconds());
        verify(settingsRepository).getSettings();
        verifyNoMoreInteractions(settingsRepository);
    }

    @Test
    void getSettings_WhenNoneExist_ShouldInitializeDefaults() {
        when(settingsRepository.getSettings()).thenReturn(Optional.empty());
        SystemSettings defaults = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .autoThresholdSeconds(3600L)
                .mandatoryFenceDays(14)
                .encounterWindowDays(14)
                .build();
        when(settingsRepository.save(any(SystemSettings.class))).thenReturn(defaults);

        ResponseEntity<SystemSettings> response = controller.getSettings();

        assertNotNull(response.getBody());
        assertTrue(response.getBody().getUnconfirmedFencingEnabled());
        verify(settingsRepository).getSettings();
        verify(settingsRepository).save(any(SystemSettings.class));
    }

    @Test
    void updateSetting_ShouldUpdateFields() {
        SystemSettings existing = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .autoThresholdSeconds(3600L)
                .mandatoryFenceDays(14)
                .encounterWindowDays(14)
                .build();
        when(settingsRepository.getSettings()).thenReturn(Optional.of(existing));

        SystemSettings newSettings = SystemSettings.builder()
                .unconfirmedFencingEnabled(false)
                .autoThresholdSeconds(7200L)
                .build();
        when(settingsRepository.save(any(SystemSettings.class))).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<SystemSettings> response = controller.updateSettings(newSettings);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().getUnconfirmedFencingEnabled());
        assertEquals(7200L, response.getBody().getAutoThresholdSeconds());
        assertEquals(14, response.getBody().getMandatoryFenceDays());
        assertEquals(14, response.getBody().getEncounterWindowDays());
        verify(settingsRepository).save(existing);
    }

    @Test
    void updateSetting_WithNullValues_ShouldNotOverwrite() {
        SystemSettings existing = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .autoThresholdSeconds(3600L)
                .mandatoryFenceDays(14)
                .encounterWindowDays(14)
                .build();
        when(settingsRepository.getSettings()).thenReturn(Optional.of(existing));

        SystemSettings newSettings = SystemSettings.builder().build();
        when(settingsRepository.save(any(SystemSettings.class))).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<SystemSettings> response = controller.updateSettings(newSettings);

        assertTrue(response.getBody().getUnconfirmedFencingEnabled());
        assertEquals(3600L, response.getBody().getAutoThresholdSeconds());
        assertEquals(14, response.getBody().getMandatoryFenceDays());
        assertEquals(14, response.getBody().getEncounterWindowDays());
        verify(settingsRepository).save(existing);
    }

    @Test
    void toggleUnconfirmedFencing_ShouldEnable() {
        SystemSettings existing = SystemSettings.builder()
                .unconfirmedFencingEnabled(false)
                .autoThresholdSeconds(3600L)
                .mandatoryFenceDays(14)
                .encounterWindowDays(14)
                .build();
        when(settingsRepository.getSettings()).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(SystemSettings.class))).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<SystemSettings> response = controller.toggleUnconfirmedFencing(true);

        assertTrue(response.getBody().getUnconfirmedFencingEnabled());
        verify(settingsRepository).save(existing);
    }

    @Test
    void toggleUnconfirmedFencing_ShouldDisable() {
        SystemSettings existing = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .autoThresholdSeconds(3600L)
                .mandatoryFenceDays(14)
                .encounterWindowDays(14)
                .build();
        when(settingsRepository.getSettings()).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(SystemSettings.class))).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<SystemSettings> response = controller.toggleUnconfirmedFencing(false);

        assertFalse(response.getBody().getUnconfirmedFencingEnabled());
        verify(settingsRepository).save(existing);
    }

    @Test
    void toggleUnconfirmedFencing_WhenNoSettings_ShouldInitializeDefaults() {
        when(settingsRepository.getSettings()).thenReturn(Optional.empty());
        SystemSettings defaults = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .autoThresholdSeconds(3600L)
                .mandatoryFenceDays(14)
                .encounterWindowDays(14)
                .build();
        when(settingsRepository.save(any(SystemSettings.class))).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<SystemSettings> response = controller.toggleUnconfirmedFencing(true);

        assertTrue(response.getBody().getUnconfirmedFencingEnabled());
        verify(settingsRepository).getSettings();
        verify(settingsRepository, times(2)).save(any(SystemSettings.class));
    }
}
