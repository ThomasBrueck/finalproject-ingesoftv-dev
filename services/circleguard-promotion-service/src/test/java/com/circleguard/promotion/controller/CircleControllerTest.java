package com.circleguard.promotion.controller;

import com.circleguard.promotion.model.graph.CircleNode;
import com.circleguard.promotion.service.CircleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircleControllerTest {

    @Mock
    private CircleService circleService;

    private CircleController controller;

    @BeforeEach
    void setUp() {
        controller = new CircleController(circleService);
    }

    @Test
    void createCircle_ShouldReturnCreatedCircle() {
        CircleController.CircleCreateRequest request = new CircleController.CircleCreateRequest();
        request.setName("Test Circle");
        request.setLocationId("loc-1");

        CircleNode node = CircleNode.builder().name("Test Circle").build();
        when(circleService.createCircle("Test Circle", "loc-1")).thenReturn(node);

        ResponseEntity<CircleNode> response = controller.createCircle(request);

        assertNotNull(response.getBody());
        assertEquals("Test Circle", response.getBody().getName());
        verify(circleService).createCircle("Test Circle", "loc-1");
    }

    @Test
    void joinCircle_ShouldReturnJoinedCircle() {
        CircleNode node = CircleNode.builder().name("Circle").build();
        when(circleService.joinCircle("user-1", "CODE")).thenReturn(node);

        ResponseEntity<CircleNode> response = controller.joinCircle("CODE", "user-1");

        assertEquals("Circle", response.getBody().getName());
        verify(circleService).joinCircle("user-1", "CODE");
    }

    @Test
    void addMember_ShouldReturnUpdatedCircle() {
        CircleNode node = CircleNode.builder().name("Circle").build();
        when(circleService.addMember(1L, "user-2")).thenReturn(node);

        ResponseEntity<CircleNode> response = controller.addMember(1L, "user-2");

        assertEquals("Circle", response.getBody().getName());
        verify(circleService).addMember(1L, "user-2");
    }

    @Test
    void getUserCircles_ShouldReturnCircleList() {
        CircleNode c1 = CircleNode.builder().name("C1").build();
        CircleNode c2 = CircleNode.builder().name("C2").build();
        when(circleService.getUserCircles("user-1")).thenReturn(List.of(c1, c2));

        ResponseEntity<List<CircleNode>> response = controller.getUserCircles("user-1");

        assertEquals(2, response.getBody().size());
        verify(circleService).getUserCircles("user-1");
    }

    @Test
    void toggleValidity_ShouldReturnOk() {
        doNothing().when(circleService).toggleCircleValidity(1L);

        ResponseEntity<Void> response = controller.toggleValidity(1L);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(circleService).toggleCircleValidity(1L);
    }

    @Test
    void forceFence_ShouldReturnOk() {
        doNothing().when(circleService).forceFenceCircle(1L);

        ResponseEntity<Void> response = controller.forceFence(1L);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(circleService).forceFenceCircle(1L);
    }
}
