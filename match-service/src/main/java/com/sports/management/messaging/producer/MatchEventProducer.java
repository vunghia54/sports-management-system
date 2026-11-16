package com.sports.management.messaging.producer;

import com.sports.management.messaging.event.MatchFinishedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MatchEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public MatchEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishMatchFinished(MatchFinishedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    "match.exchange",
                    "match.finished",
                    event
            );
            System.out.println(
                    "[RABBITMQ] Published MATCH_FINISHED event, Match ID = "
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
