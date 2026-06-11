package com.circleguard.promotion.service;

import com.circleguard.promotion.model.graph.CircleNode;
import com.circleguard.promotion.model.graph.UserNode;
import com.circleguard.promotion.repository.graph.CircleNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircleServiceTest {

    @Mock
    private CircleNodeRepository circleRepository;

    @Mock
    private HealthStatusService healthStatusService;

    private CircleService circleService;

    @BeforeEach
    void setUp() {
        circleService = new CircleService(circleRepository, healthStatusService);
    }

    @Test
    void createCircle() {
        String name = "Test Circle";
        String locationId = "loc-1";
        CircleNode saved = CircleNode.builder()
                .id(1L)
                .name(name)
                .inviteCode("MESH-ABCD")
                .locationId(locationId)
                .isActive(true)
                .createdAt(System.currentTimeMillis())
                .build();

        when(circleRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(circleRepository.save(any(CircleNode.class))).thenReturn(saved);

        CircleNode result = circleService.createCircle(name, locationId);

        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(locationId, result.getLocationId());
        assertTrue(result.getIsActive());
        verify(circleRepository).save(any(CircleNode.class));
        verify(circleRepository).existsByInviteCode(anyString());
    }

    @Test
    void joinCircle() {
        String anonymousId = "user-1";
        String inviteCode = "MESH-ABCD";
        CircleNode circle = CircleNode.builder()
                .id(1L)
                .name("Test Circle")
                .inviteCode(inviteCode)
                .build();

        when(circleRepository.joinCircle(anonymousId, inviteCode)).thenReturn(Optional.of(circle));

        CircleNode result = circleService.joinCircle(anonymousId, inviteCode);

        assertNotNull(result);
        assertEquals(inviteCode, result.getInviteCode());
        verify(circleRepository).joinCircle(anonymousId, inviteCode);
    }

    @Test
    void joinCircle_throwsWhenInvalidCode() {
        when(circleRepository.joinCircle(anyString(), anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> circleService.joinCircle("user-1", "INVALID"));
    }

    @Test
    void addMember() {
        Long circleId = 1L;
        String anonymousId = "user-1";
        CircleNode circle = CircleNode.builder()
                .id(circleId)
                .name("Test Circle")
                .build();

        when(circleRepository.addUserToCircle(anonymousId, circleId)).thenReturn(Optional.of(circle));

        CircleNode result = circleService.addMember(circleId, anonymousId);

        assertNotNull(result);
        assertEquals(circleId, result.getId());
        verify(circleRepository).addUserToCircle(anonymousId, circleId);
    }

    @Test
    void addMember_throwsWhenFailed() {
        when(circleRepository.addUserToCircle(anyString(), anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> circleService.addMember(1L, "user-1"));
    }

    @Test
    void getUserCircles() {
        String anonymousId = "user-1";
        List<CircleNode> circles = List.of(
                CircleNode.builder().id(1L).name("Circle 1").build(),
                CircleNode.builder().id(2L).name("Circle 2").build()
        );

        when(circleRepository.findCirclesByUser(anonymousId)).thenReturn(circles);

        List<CircleNode> result = circleService.getUserCircles(anonymousId);

        assertEquals(2, result.size());
        verify(circleRepository).findCirclesByUser(anonymousId);
    }

    @Test
    void toggleValidity() {
        Long circleId = 1L;
        CircleNode circle = CircleNode.builder()
                .id(circleId)
                .isValid(true)
                .members(Set.of(
                        UserNode.builder().anonymousId("user-1").build(),
                        UserNode.builder().anonymousId("user-2").build()
                ))
                .build();

        when(circleRepository.findById(circleId)).thenReturn(Optional.of(circle));
        when(circleRepository.save(any(CircleNode.class))).thenAnswer(i -> i.getArgument(0));

        circleService.toggleCircleValidity(circleId);

        assertFalse(circle.getIsValid());
        verify(circleRepository).save(circle);
        verify(healthStatusService, times(2)).resolveStatus(anyString());
    }

    @Test
    void toggleValidity_toTrueDoesNotResolveStatus() {
        Long circleId = 1L;
        CircleNode circle = CircleNode.builder()
                .id(circleId)
                .isValid(false)
                .members(Set.of(UserNode.builder().anonymousId("user-1").build()))
                .build();

        when(circleRepository.findById(circleId)).thenReturn(Optional.of(circle));
        when(circleRepository.save(any(CircleNode.class))).thenAnswer(i -> i.getArgument(0));

        circleService.toggleCircleValidity(circleId);

        assertTrue(circle.getIsValid());
        verify(healthStatusService, never()).resolveStatus(anyString());
    }

    @Test
    void toggleValidity_throwsWhenNotFound() {
        when(circleRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> circleService.toggleCircleValidity(999L));
    }

    @Test
    void forceFence() {
        Long circleId = 1L;
        UserNode activeUser = UserNode.builder().anonymousId("user-1").status("ACTIVE").build();
        UserNode nonActiveUser = UserNode.builder().anonymousId("user-2").status("RECOVERED").build();
        CircleNode circle = CircleNode.builder()
                .id(circleId)
                .name("Test Circle")
                .members(Set.of(activeUser, nonActiveUser))
                .build();

        when(circleRepository.findById(circleId)).thenReturn(Optional.of(circle));
        when(circleRepository.save(any(CircleNode.class))).thenAnswer(i -> i.getArgument(0));

        circleService.forceFenceCircle(circleId);

        assertTrue(circle.getForceFence());
        verify(circleRepository).save(circle);
        verify(healthStatusService).updateStatus("user-1", "PROBABLE");
        verify(healthStatusService, never()).updateStatus("user-2", "PROBABLE");
    }

    @Test
    void forceFence_throwsWhenNotFound() {
        when(circleRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> circleService.forceFenceCircle(999L));
    }
}
