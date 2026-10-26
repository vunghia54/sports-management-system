package com.sports.management.notification.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "sports.exchange";
    public static final String QUEUE_NOTIFICATION = "sports.notification.queue";
    public static final String ROUTING_KEY_FINISHED = "match.finished";

    @Bean
    public TopicExchange sportsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(QUEUE_NOTIFICATION, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange sportsExchange) {
        return BindingBuilder.bind(notificationQueue).to(sportsExchange).with(ROUTING_KEY_FINISHED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
