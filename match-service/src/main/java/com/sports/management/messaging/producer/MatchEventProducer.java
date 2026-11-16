package com.sports.management.messaging.producer;

import com.sports.management.messaging.config.RabbitMQConfig;
import com.sports.management.messaging.event.MatchFinishedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MatchEventProducer {

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
            System.out.println(
                    "[RABBITMQ] Published MATCH_FINISHED event to "
                            + RabbitMQConfig.EXCHANGE + " [" + RabbitMQConfig.ROUTING_KEY + "], Match ID = "
                            + event.getMatchId()
            );
        } catch (Exception ex) {
            System.out.println(
                    "[RABBITMQ WARN] Broker unavailable, skipped publishing MATCH_FINISHED event for Match ID = "
                            + event.getMatchId() + " (" + ex.getMessage() + ")"
            );
        }
    }
}
