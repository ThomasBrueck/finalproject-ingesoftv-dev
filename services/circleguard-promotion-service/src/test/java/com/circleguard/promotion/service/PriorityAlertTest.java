package com.circleguard.promotion.service;

import com.circleguard.promotion.model.jpa.SystemSettings;
import com.circleguard.promotion.repository.jpa.SystemSettingsRepository;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class PriorityAlertTest {

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
    void shouldTriggerPriorityAlertOnConfirmedStatus() {
        String anonymousId = "user-priority-confirmed";

        SystemSettings settings = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .encounterWindowDays(14)
                .mandatoryFenceDays(14)
                .build();
        when(systemSettingsRepository.getSettings()).thenReturn(Optional.of(settings));

        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);

        List<Map<String, String>> affectedContacts = new ArrayList<>();
        affectedContacts.add(Map.of("id", "contact-1", "status", "SUSPECT"));

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("sourceId", anonymousId);
        resultMap.put("affectedContacts", affectedContacts);

        when(runnableSpec.bind(anyString()).to(anyString())
                .bind(anyString()).to(anyString())
                .bind(ArgumentMatchers.anyLong()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(resultMap));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(circleNodeRepository.findNewlyFencedCircles(anyString()))
                .thenReturn(java.util.Collections.emptyList());

        healthStatusService.updateStatus(anonymousId, "CONFIRMED");

        verify(kafkaTemplate).send(
                ArgumentMatchers.eq("alert.priority"),
                ArgumentMatchers.eq(anonymousId),
                ArgumentMatchers.anyMap()
        );
    }

    @Test
    void shouldTriggerPriorityAlertOnLargeOutbreak() {
        String anonymousId = "user-priority-outbreak";

        SystemSettings settings = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .encounterWindowDays(14)
                .mandatoryFenceDays(14)
                .build();
        when(systemSettingsRepository.getSettings()).thenReturn(Optional.of(settings));

        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);

        List<Map<String, String>> affectedContacts = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            affectedContacts.add(Map.of("id", "contact-" + i, "status", "SUSPECT"));
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("sourceId", anonymousId);
        resultMap.put("affectedContacts", affectedContacts);

        when(runnableSpec.bind(anyString()).to(anyString())
                .bind(anyString()).to(anyString())
                .bind(ArgumentMatchers.anyLong()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(resultMap));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(circleNodeRepository.findNewlyFencedCircles(anyString()))
                .thenReturn(java.util.Collections.emptyList());

        healthStatusService.updateStatus(anonymousId, "SUSPECT");

        verify(kafkaTemplate).send(
                ArgumentMatchers.eq("alert.priority"),
                ArgumentMatchers.eq(anonymousId),
                ArgumentMatchers.anyMap()
        );
    }

    @Test
    void shouldNotTriggerPriorityAlertOnNormalUpdate() {
        String anonymousId = "user-priority-normal";

        SystemSettings settings = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .encounterWindowDays(14)
                .mandatoryFenceDays(14)
                .build();
        when(systemSettingsRepository.getSettings()).thenReturn(Optional.of(settings));

        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("sourceId", anonymousId);
        resultMap.put("affectedContacts", java.util.Collections.emptyList());

        when(runnableSpec.bind(anyString()).to(anyString())
                .bind(anyString()).to(anyString())
                .bind(ArgumentMatchers.anyLong()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(resultMap));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(circleNodeRepository.findNewlyFencedCircles(anyString()))
                .thenReturn(java.util.Collections.emptyList());

        healthStatusService.updateStatus(anonymousId, "SUSPECT");

        verify(kafkaTemplate, never()).send(
                ArgumentMatchers.eq("alert.priority"),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyMap()
        );
    }
}
