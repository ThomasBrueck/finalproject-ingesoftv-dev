package com.circleguard.promotion.task;

import com.circleguard.promotion.repository.graph.UserNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphCleanupTaskTest {

    @Mock
    private UserNodeRepository userNodeRepository;

    private GraphCleanupTask task;

    @BeforeEach
    void setUp() {
        task = new GraphCleanupTask(userNodeRepository);
    }

    @Test
    void purgeStaleEncounters_ShouldCallRepository() {
        when(userNodeRepository.purgeStaleEncounters(anyLong())).thenReturn(42L);

        task.purgeStaleEncounters();

        verify(userNodeRepository).purgeStaleEncounters(anyLong());
    }

    @Test
    void purgeStaleEncounters_WhenRepositoryReturnsNull_ShouldNotThrow() {
        when(userNodeRepository.purgeStaleEncounters(anyLong())).thenReturn(null);

        task.purgeStaleEncounters();

        verify(userNodeRepository).purgeStaleEncounters(anyLong());
    }

    @Test
    void purgeStaleEncounters_WhenRepositoryThrows_ShouldNotPropagate() {
        when(userNodeRepository.purgeStaleEncounters(anyLong())).thenThrow(new RuntimeException("DB error"));

        task.purgeStaleEncounters();

        verify(userNodeRepository).purgeStaleEncounters(anyLong());
    }
}
