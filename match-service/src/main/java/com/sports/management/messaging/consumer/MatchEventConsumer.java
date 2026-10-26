package com.sports.management.messaging.consumer;

import com.sports.management.messaging.config.RabbitMQConfig;
import com.sports.management.messaging.event.MatchFinishedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MatchEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void receiveMatchFinishedEvent(
            MatchFinishedEvent event) {

        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                ">>> CONSUMER: NHAN DUOC MATCH FINISHED"
        );

        System.out.println(
                "Event          : " + event.getEvent()
        );

        System.out.println(
                "Match ID       : " + event.getMatchId()
        );

        System.out.println(
                "Tournament ID  : " + event.getTournamentId()
        );

        System.out.println(
                "Participant 1  : " + event.getParticipant1()
        );

        System.out.println(
                "Participant 2  : " + event.getParticipant2()
        );

        System.out.println(
                "===================================="
        );
        System.out.println();
    }
}