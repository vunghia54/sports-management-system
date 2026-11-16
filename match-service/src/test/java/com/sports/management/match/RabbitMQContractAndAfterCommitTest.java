package com.sports.management.match;

import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.match.service.MatchService;
import com.sports.management.messaging.config.RabbitMQConfig;
import com.sports.management.messaging.event.MatchFinishedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:match_rabbit_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "jwt.secret=SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!"
})
class RabbitMQContractAndAfterCommitTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private TournamentClient tournamentClient;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private Match match;

    @BeforeEach
    void setUp() {
        clearInvocations(rabbitTemplate);
        matchRepository.deleteAll();
        Match m = new Match(null, 1L, "Chung kết",
                LocalDateTime.of(2026, 12, 20, 19, 0),
                "SVĐ Mỹ Đình", "Hà Nội", "Viettel", "SCHEDULED");
        m.setCompetitionId(10L);
        m.setParticipant1Id(1L);
        m.setParticipant2Id(2L);
        m.setOwnerUsername("organizer1");
        match = matchRepository.save(m);
    }

    @Test
    void rabbitMQConfig_usesUnifiedSportsTopicExchangeAndDeclaresNoQueue() {
        assertEquals("sports.exchange", RabbitMQConfig.EXCHANGE);
        assertEquals("match.finished", RabbitMQConfig.ROUTING_KEY);

        Map<String, TopicExchange> exchanges = applicationContext.getBeansOfType(TopicExchange.class);
        assertTrue(exchanges.values().stream().anyMatch(e -> "sports.exchange".equals(e.getName())));

        Map<String, Queue> queues = applicationContext.getBeansOfType(Queue.class);
        assertTrue(queues.isEmpty(), "Producer must not declare queues such as match.finished.queue");
    }

    @Test
    void finishMatch_publishesToSportsExchangeAfterCommitAndNotBeforeOrOnRollback() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // 1. Rolled-back transaction must never publish to RabbitMQ
        txTemplate.executeWithoutResult(status -> {
            matchService.patchMatch(match.getId(), Map.of("status", "FINISHED"));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
            status.setRollbackOnly();
        });
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        assertEquals("SCHEDULED", matchRepository.findById(match.getId()).orElseThrow().getStatus());

        // 2. Committed transaction must not publish mid-tx, and publishes to sports.exchange after commit
        txTemplate.executeWithoutResult(status -> {
            matchService.patchMatch(match.getId(), Map.of("status", "FINISHED"));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        });

        verify(rabbitTemplate).convertAndSend(
                eq("sports.exchange"),
                eq("match.finished"),
                org.mockito.ArgumentMatchers.<MatchFinishedEvent>argThat(
                        evt -> evt != null && match.getId().equals(evt.getMatchId())
                )
        );
    }
}
