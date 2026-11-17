package com.sports.management.messaging.producer;

import com.sports.management.messaging.config.RabbitMQConfig;
import com.sports.management.messaging.event.MatchFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MatchEventProducer {

    private static final Logger log = LoggerFactory.getLogger(MatchEventProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public MatchEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMatchFinishedAfterCommit(MatchFinishedEvent event) {
        publishMatchFinished(event);
    }

    public void publishMatchFinished(MatchFinishedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            log.info("[RABBITMQ] Published MATCH_FINISHED event to {} [{}], matchId={}",
                    RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event.getMatchId());
        } catch (Exception ex) {
            log.warn("[RABBITMQ] Broker unavailable, skipped publishing MATCH_FINISHED event for matchId={}: {}",
                    event.getMatchId(), ex.getMessage());
        }
    }
}
