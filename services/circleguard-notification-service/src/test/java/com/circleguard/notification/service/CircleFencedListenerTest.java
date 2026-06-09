package com.circleguard.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircleFencedListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RoomReservationService roomReservationService;

    @InjectMocks
    private CircleFencedListener listener;

    @Test
    void shouldCancelReservationWhenLocationIdPresent() throws Exception {
        String message = "{\"circleId\": \"circle-1\", \"locationId\": \"loc-1\"}";
        when(objectMapper.readValue(eq(message), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("circleId", "circle-1", "locationId", "loc-1"));

        listener.handleCircleFenced(message);

        verify(roomReservationService).cancelReservation("circle-1", "loc-1");
    }

    @Test
    void shouldSkipCancellationWhenLocationIdMissing() throws Exception {
        String message = "{\"circleId\": \"circle-2\"}";
        when(objectMapper.readValue(eq(message), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("circleId", "circle-2"));

        listener.handleCircleFenced(message);

        verify(roomReservationService, never()).cancelReservation(any(), any());
    }

    @Test
    void shouldHandleParseErrorGracefully() {
        String invalidJson = "not-json";

        listener.handleCircleFenced(invalidJson);

        verify(roomReservationService, never()).cancelReservation(any(), any());
    }
}
