package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RoomReservationServiceTest {

    @Autowired
    private RoomReservationService roomReservationService;

    @Test
    void shouldCancelReservation() {
        CompletableFuture<Void> future = roomReservationService.cancelReservation("circle-1", "loc-1");
        future.join();
        assertThat(future).isCompleted();
    }
}
