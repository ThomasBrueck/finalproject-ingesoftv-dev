package com.circleguard.promotion.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class PromoteToRecoveredTest {

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
    private Neo4jClient neo4jClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private com.circleguard.promotion.repository.graph.UserNodeRepository userNodeRepository;

    @MockBean
    private com.circleguard.promotion.repository.jpa.SystemSettingsRepository systemSettingsRepository;

    @MockBean
    private com.circleguard.promotion.repository.graph.CircleNodeRepository circleNodeRepository;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    @Test
    void shouldPromoteToRecovered() {
        String anonymousId = "user-recover-123";

        when(userNodeRepository.findById(anonymousId)).thenReturn(Optional.empty());

        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);

        Map<String, Object> phase1Result = Map.of("releasedIds", Collections.emptyList());
        when(runnableSpec.bind(anyString()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(phase1Result));

        Map<String, Object> phase2Result = Map.of("releasedIds", Collections.emptyList());
        when(runnableSpec.bind(ArgumentMatchers.anyList()).to(anyString())
                .bind(anyString()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(phase2Result));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        healthStatusService.promoteToRecovered(anonymousId);

        verify(valueOperations).set("user:status:" + anonymousId, "RECOVERED");
        verify(redisTemplate).expire("user:status:" + anonymousId, Duration.ofDays(30));
    }

    @Test
    void shouldHandleRecoveredUserInRedis() {
        String anonymousId = "user-recover-redis";

        when(userNodeRepository.findById(anonymousId)).thenReturn(Optional.empty());

        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);

        Map<String, Object> phase1Result = Map.of("releasedIds", Collections.emptyList());
        when(runnableSpec.bind(anyString()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(phase1Result));

        Map<String, Object> phase2Result = Map.of("releasedIds", Collections.emptyList());
        when(runnableSpec.bind(ArgumentMatchers.anyList()).to(anyString())
                .bind(anyString()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(phase2Result));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        healthStatusService.promoteToRecovered(anonymousId);

        verify(redisTemplate).expire("user:status:" + anonymousId, Duration.ofDays(30));
    }
}
