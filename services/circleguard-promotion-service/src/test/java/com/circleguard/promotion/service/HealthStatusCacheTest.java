package com.circleguard.promotion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthStatusCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    private HealthStatusService healthStatusService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        healthStatusService = new HealthStatusService(null, null, redisTemplate, null, null, null, meterRegistry);
    }

    @Test
    void shouldReturnCachedStatus() {
        String anonymousId = "user-cache-123";
        when(valueOperations.get("user:status:" + anonymousId)).thenReturn("CONFIRMED");

        String result = healthStatusService.getCachedStatus(anonymousId);

        assertEquals("CONFIRMED", result);
    }

    @Test
    void shouldReturnNullWhenNotCached() {
        String anonymousId = "user-cache-null";
        when(valueOperations.get("user:status:" + anonymousId)).thenReturn(null);

        String result = healthStatusService.getCachedStatus(anonymousId);

        assertNull(result);
    }

    @Test
    void shouldEvictCache() {
        String anonymousId = "user-cache-evict";
        when(valueOperations.get("user:status:" + anonymousId)).thenReturn("ACTIVE");

        healthStatusService.getCachedStatus(anonymousId);
        healthStatusService.evictUserCache(anonymousId);
        healthStatusService.getCachedStatus(anonymousId);

        verify(valueOperations, times(2)).get("user:status:" + anonymousId);
    }
}
