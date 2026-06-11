package com.circleguard.promotion.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class HealthStatusCacheTest {

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
    void shouldReturnCachedStatus() {
        String anonymousId = "user-cache-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:status:" + anonymousId)).thenReturn("CONFIRMED");

        String result = healthStatusService.getCachedStatus(anonymousId);

        assertEquals("CONFIRMED", result);
    }

    @Test
    void shouldReturnNullWhenNotCached() {
        String anonymousId = "user-cache-null";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:status:" + anonymousId)).thenReturn(null);

        String result = healthStatusService.getCachedStatus(anonymousId);

        assertNull(result);
    }

    @Test
    void shouldEvictCache() {
        String anonymousId = "user-cache-evict";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:status:" + anonymousId)).thenReturn("ACTIVE");

        healthStatusService.getCachedStatus(anonymousId);
        healthStatusService.evictUserCache(anonymousId);
        healthStatusService.getCachedStatus(anonymousId);

        Mockito.verify(valueOperations, times(2)).get("user:status:" + anonymousId);
    }
}
