package com.circleguard.promotion.service;

import com.circleguard.promotion.exception.FenceException;
import com.circleguard.promotion.model.graph.UserNode;
import com.circleguard.promotion.model.jpa.SystemSettings;
import com.circleguard.promotion.repository.jpa.SystemSettingsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class FenceWindowTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public org.springframework.transaction.PlatformTransactionManager transactionManager() {
            return Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        }

        @Bean(name = "neo4jTransactionManager")
        public org.springframework.transaction.PlatformTransactionManager neo4jTransactionManager() {
            return Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        }
    }

    @Autowired
    private HealthStatusService healthStatusService;

    @MockBean
    private com.circleguard.promotion.repository.graph.UserNodeRepository userNodeRepository;

    @MockBean
    private Neo4jClient neo4jClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private SystemSettingsRepository systemSettingsRepository;

    @MockBean
    private com.circleguard.promotion.repository.graph.CircleNodeRepository circleNodeRepository;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    @Test
    void shouldThrowFenceExceptionWhenWithinWindow() {
        String anonymousId = "user-fenced-within";

        long fiveDaysAgo = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000);
        UserNode user = UserNode.builder()
                .anonymousId(anonymousId)
                .status("SUSPECT")
                .statusUpdatedAt(fiveDaysAgo)
                .build();

        when(userNodeRepository.findById(anonymousId)).thenReturn(Optional.of(user));

        SystemSettings settings = SystemSettings.builder()
                .mandatoryFenceDays(14)
                .unconfirmedFencingEnabled(true)
                .build();
        when(systemSettingsRepository.getSettings()).thenReturn(Optional.of(settings));

        assertThrows(FenceException.class, () -> healthStatusService.resolveStatus(anonymousId));
    }

    @Test
    void shouldNotThrowWhenOutsideWindow() {
        String anonymousId = "user-fenced-outside";

        long twentyDaysAgo = System.currentTimeMillis() - (20L * 24 * 60 * 60 * 1000);
        UserNode user = UserNode.builder()
                .anonymousId(anonymousId)
                .status("SUSPECT")
                .statusUpdatedAt(twentyDaysAgo)
                .build();

        when(userNodeRepository.findById(anonymousId)).thenReturn(Optional.of(user));

        SystemSettings settings = SystemSettings.builder()
                .mandatoryFenceDays(14)
                .unconfirmedFencingEnabled(true)
                .build();
        when(systemSettingsRepository.getSettings()).thenReturn(Optional.of(settings));

        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);

        Map<String, Object> phase1Result = Map.of("releasedIds", Collections.emptyList());
        when(runnableSpec.bind(anyString()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(phase1Result));

        Map<String, Object> phase2Result = Map.of("releasedIds", Collections.emptyList());
        when(runnableSpec.bind(org.mockito.ArgumentMatchers.anyList()).to(anyString())
                .bind(anyString()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(phase2Result));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> healthStatusService.resolveStatus(anonymousId));
    }

    @Test
    void shouldNotThrowWhenUnconfirmedFencingDisabled() {
        String anonymousId = "user-fenced-disabled";

        long fiveDaysAgo = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000);
        UserNode user = UserNode.builder()
                .anonymousId(anonymousId)
                .status("SUSPECT")
                .statusUpdatedAt(fiveDaysAgo)
                .build();

        when(userNodeRepository.findById(anonymousId)).thenReturn(Optional.of(user));

        SystemSettings settings = SystemSettings.builder()
                .mandatoryFenceDays(14)
                .unconfirmedFencingEnabled(false)
                .build();
        when(systemSettingsRepository.getSettings()).thenReturn(Optional.of(settings));

        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);

        Map<String, Object> phase1Result = Map.of("releasedIds", Collections.emptyList());
        when(runnableSpec.bind(anyString()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(phase1Result));

        Map<String, Object> phase2Result = Map.of("releasedIds", Collections.emptyList());
        when(runnableSpec.bind(org.mockito.ArgumentMatchers.anyList()).to(anyString())
                .bind(anyString()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(phase2Result));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> healthStatusService.resolveStatus(anonymousId));
    }
}
