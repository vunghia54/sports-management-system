package com.sports.management.notification;

import com.sports.management.notification.messaging.MatchFinishedEvent;
import com.sports.management.notification.messaging.NotificationConsumer;
import com.sports.management.notification.messaging.RabbitMQConfig;
import com.sports.management.notification.model.Notification;
import com.sports.management.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notif_rabbit_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "jwt.secret=SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!"
})
class NotificationRabbitMQAndConsumerTest {

    @Autowired
    private TopicExchange sportsExchange;

    @Autowired
    private Queue notificationQueue;

    @Autowired
    private Binding notificationBinding;

    @Autowired
    private MessageConverter jsonMessageConverter;

    @Autowired
    private NotificationConsumer notificationConsumer;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void brokerContract_matchesProducerExchangeRoutingKeyAndDeserializesForeignTypeId() {
        assertEquals("sports.exchange", RabbitMQConfig.EXCHANGE_NAME);
        assertEquals("sports.notification.queue", RabbitMQConfig.QUEUE_NOTIFICATION);
        assertEquals("match.finished", RabbitMQConfig.ROUTING_KEY_FINISHED);

        assertEquals("sports.exchange", sportsExchange.getName());
        assertEquals("sports.notification.queue", notificationQueue.getName());
        assertEquals("sports.exchange", notificationBinding.getExchange());
        assertEquals("sports.notification.queue", notificationBinding.getDestination());
        assertEquals("match.finished", notificationBinding.getRoutingKey());

        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setHeader("__TypeId__", "com.sports.management.messaging.event.MatchFinishedEvent");
        props.setInferredArgumentType(MatchFinishedEvent.class);

        byte[] body = ("{\"event\":\"MATCH_FINISHED\",\"matchId\":88,\"tournamentId\":1,"
                + "\"participant1\":\"Đội A\",\"participant2\":\"Đội B\"}").getBytes(StandardCharsets.UTF_8);
        Object converted = jsonMessageConverter.fromMessage(new Message(body, props));

        assertInstanceOf(MatchFinishedEvent.class, converted);
        MatchFinishedEvent event = (MatchFinishedEvent) converted;
        assertEquals(88L, event.getMatchId());
        assertEquals("Đội A", event.getParticipant1());
    }

    @Test
    void consumer_ignoresDuplicateMatchFinishedEventsForSameMatchId() {
        MatchFinishedEvent event1 = new MatchFinishedEvent("MATCH_FINISHED", 500L, 1L, "Hà Nội", "Viettel");
        MatchFinishedEvent event2 = new MatchFinishedEvent("MATCH_FINISHED", 501L, 1L, "Nam Định", "CAHN");

        notificationConsumer.handleMatchFinished(event1);
        notificationConsumer.handleMatchFinished(event1); // redelivery must be ignored
        notificationConsumer.handleMatchFinished(event2);

        List<Notification> saved = notificationRepository.findAll();
        assertEquals(2, saved.size());
        assertEquals(1, saved.stream().filter(n -> Long.valueOf(500L).equals(n.getReferenceId())).count());
        assertEquals(1, saved.stream().filter(n -> Long.valueOf(501L).equals(n.getReferenceId())).count());
    }
}
