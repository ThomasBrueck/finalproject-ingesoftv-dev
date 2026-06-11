package com.circleguard.promotion.service;

import com.circleguard.promotion.model.graph.CircleNode;
import com.circleguard.promotion.model.graph.UserNode;
import com.circleguard.promotion.model.jpa.SystemSettings;
import com.circleguard.promotion.repository.graph.CircleNodeRepository;
import com.circleguard.promotion.repository.graph.UserNodeRepository;
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

import java.util.Collections;
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
class CircleFencingTest {

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
    private UserNodeRepository userNodeRepository;

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
    private CircleNodeRepository circleNodeRepository;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    @Test
    void shouldBroadcastFencedCircleEvent() {
        String anonymousId = "user-circle-fence";

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
        resultMap.put("affectedContacts", Collections.emptyList());

        when(runnableSpec.bind(anyString()).to(anyString())
                .bind(anyString()).to(anyString())
                .bind(ArgumentMatchers.anyLong()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(resultMap));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CircleNode fencedCircle = CircleNode.builder()
                .id(1L)
                .name("TestCircle")
                .locationId("loc-1")
                .build();
        when(circleNodeRepository.findNewlyFencedCircles(anonymousId))
                .thenReturn(List.of(fencedCircle));

        healthStatusService.updateStatus(anonymousId, "CONFIRMED");

        verify(kafkaTemplate).send(
                ArgumentMatchers.eq("circle.fenced"),
                ArgumentMatchers.eq("1"),
                ArgumentMatchers.anyMap()
        );
    }

    @Test
    void shouldHandleNoFencedCircles() {
        String anonymousId = "user-no-fence";

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
        resultMap.put("affectedContacts", Collections.emptyList());

        when(runnableSpec.bind(anyString()).to(anyString())
                .bind(anyString()).to(anyString())
                .bind(ArgumentMatchers.anyLong()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(resultMap));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(circleNodeRepository.findNewlyFencedCircles(anonymousId))
                .thenReturn(Collections.emptyList());

        healthStatusService.updateStatus(anonymousId, "CONFIRMED");

        verify(kafkaTemplate, never()).send(
                ArgumentMatchers.eq("circle.fenced"),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyMap()
        );
    }
}
