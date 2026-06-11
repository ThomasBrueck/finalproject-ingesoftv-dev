package com.circleguard.promotion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MacSessionRegistryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MacSessionRegistry macSessionRegistry;

    @BeforeEach
    void setUp() {
        macSessionRegistry = new MacSessionRegistry(redisTemplate);
    }

    @Test
    void registerSession() {
        String macAddress = "AA:BB:CC:DD:EE:FF";
        String anonymousId = "user-123";
        String expectedKey = "session:mac:aa:bb:cc:dd:ee:ff";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        macSessionRegistry.registerSession(macAddress, anonymousId);

        verify(valueOperations).set(expectedKey, anonymousId, Duration.ofHours(8));
    }

    @Test
    void lookupUserByMac_returnsUserWhenFound() {
        String macAddress = "AA:BB:CC:DD:EE:FF";
        String anonymousId = "user-123";
        String expectedKey = "session:mac:aa:bb:cc:dd:ee:ff";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(anonymousId);

        String result = macSessionRegistry.getAnonymousId(macAddress);

        assertEquals(anonymousId, result);
    }

    @Test
    void lookupUserByMac_returnsNullWhenNotFound() {
        String macAddress = "AA:BB:CC:DD:EE:FF";
        String expectedKey = "session:mac:aa:bb:cc:dd:ee:ff";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        String result = macSessionRegistry.getAnonymousId(macAddress);

        assertNull(result);
    }

    @Test
    void closeSession() {
        String macAddress = "AA:BB:CC:DD:EE:FF";
        String expectedKey = "session:mac:aa:bb:cc:dd:ee:ff";

        macSessionRegistry.closeSession(macAddress);

        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    void closeSession_handlesNonExistentSessionGracefully() {
        String macAddress = "AA:BB:CC:DD:EE:FF";
        String expectedKey = "session:mac:aa:bb:cc:dd:ee:ff";

            when(redisTemplate.delete(expectedKey)).thenReturn(true);

        macSessionRegistry.closeSession(macAddress);

        verify(redisTemplate).delete(expectedKey);
    }
}
