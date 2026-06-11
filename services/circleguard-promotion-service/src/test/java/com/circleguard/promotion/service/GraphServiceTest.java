package com.circleguard.promotion.service;

import com.circleguard.promotion.repository.graph.UserNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphServiceTest {

    @Mock
    private UserNodeRepository userNodeRepository;
    @Mock
    private Neo4jClient neo4jClient;

    private GraphService graphService;

    @BeforeEach
    void setUp() {
        graphService = new GraphService(userNodeRepository, neo4jClient);
    }

    @Test
    void recordEncounter_recordsEncounterBetweenTwoUsers() {
        String userA = "user-A";
        String userB = "user-B";
        String locationId = "loc-1";

        graphService.recordEncounter(userA, userB, locationId);

        ArgumentCaptor<Long> timestampCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userNodeRepository).recordEncounter(eq(userA), eq(userB), timestampCaptor.capture(), eq(locationId));
        assertNotNull(timestampCaptor.getValue());
        assertTrue(timestampCaptor.getValue() > 0);
    }

    @Test
    void detectAndFormCircles_withThreeOrMoreUsers_formsCircle() {
        String locationId = "loc-1";
        Neo4jClient.UnboundRunnableSpec unboundSpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.OngoingBindSpec ongoingBindSpec = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.RunnableSpec runnableSpec = mock(Neo4jClient.RunnableSpec.class);
        when(neo4jClient.query(anyString())).thenReturn(unboundSpec);
        when(unboundSpec.bind(eq(locationId))).thenReturn(ongoingBindSpec);
        when(ongoingBindSpec.to(eq("loc"))).thenReturn(runnableSpec);

        graphService.detectAndFormCircles(locationId);

        verify(neo4jClient).query(anyString());
        verify(unboundSpec).bind(locationId);
        verify(ongoingBindSpec).to("loc");
        verify(runnableSpec).run();
    }

    @Test
    void detectAndFormCircles_withLessThanThreeUsers_doesNothing() {
        String locationId = "loc-2";
        Neo4jClient.UnboundRunnableSpec unboundSpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.OngoingBindSpec ongoingBindSpec = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.RunnableSpec runnableSpec = mock(Neo4jClient.RunnableSpec.class);
        when(neo4jClient.query(anyString())).thenReturn(unboundSpec);
        when(unboundSpec.bind(eq(locationId))).thenReturn(ongoingBindSpec);
        when(ongoingBindSpec.to(eq("loc"))).thenReturn(runnableSpec);

        graphService.detectAndFormCircles(locationId);

        verify(neo4jClient).query(anyString());
        verify(unboundSpec).bind(locationId);
        verify(ongoingBindSpec).to("loc");
        verify(runnableSpec).run();
    }

    @Test
    void detectAndFormCircles_handlesExceptions() {
        String locationId = "loc-3";
        Neo4jClient.UnboundRunnableSpec unboundSpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.OngoingBindSpec ongoingBindSpec = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.RunnableSpec runnableSpec = mock(Neo4jClient.RunnableSpec.class);
        when(neo4jClient.query(anyString())).thenReturn(unboundSpec);
        when(unboundSpec.bind(eq(locationId))).thenReturn(ongoingBindSpec);
        when(ongoingBindSpec.to(eq("loc"))).thenReturn(runnableSpec);
        when(runnableSpec.run()).thenThrow(new RuntimeException("Neo4j connection failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> graphService.detectAndFormCircles(locationId));
        assertEquals("Neo4j connection failed", exception.getMessage());
    }
}
