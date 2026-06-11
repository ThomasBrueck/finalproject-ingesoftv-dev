package com.circleguard.promotion.controller;

import com.circleguard.promotion.controller.MeshController.MeshStatsResponse;
import com.circleguard.promotion.repository.graph.UserNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeshControllerTest {

    @Mock
    private UserNodeRepository userRepository;

    private MeshController controller;

    @BeforeEach
    void setUp() {
        controller = new MeshController(userRepository);
    }

    @Test
    void getMeshStats_ShouldReturnCounts() {
        String anonymousId = "anon-123";
        when(userRepository.getConfirmedConnectionCount(anonymousId)).thenReturn(5L);
        when(userRepository.getUnconfirmedConnectionCount(anonymousId)).thenReturn(3L);

        ResponseEntity<MeshStatsResponse> response = controller.getMeshStats(anonymousId);

        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().getConfirmedCount());
        assertEquals(3L, response.getBody().getUnconfirmedCount());
        verify(userRepository).getConfirmedConnectionCount(anonymousId);
        verify(userRepository).getUnconfirmedConnectionCount(anonymousId);
    }
}
