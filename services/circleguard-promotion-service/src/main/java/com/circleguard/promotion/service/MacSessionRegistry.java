package com.circleguard.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MacSessionRegistry {
    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "session:mac:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(8);

    public void registerSession(String macAddress, String anonymousId) {
        String key = KEY_PREFIX + sanitize(macAddress).toLowerCase();
        redisTemplate.opsForValue().set(key, sanitize(anonymousId), DEFAULT_TTL);
    }

    public String getAnonymousId(String macAddress) {
        String key = KEY_PREFIX + sanitize(macAddress).toLowerCase();
        return redisTemplate.opsForValue().get(key);
    }

    public void closeSession(String macAddress) {
        String key = KEY_PREFIX + sanitize(macAddress).toLowerCase();
        redisTemplate.delete(key);
    }

    // Neutralize control characters from user-controlled data before use (S5145).
    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("\\p{Cntrl}", "");
    }
}
