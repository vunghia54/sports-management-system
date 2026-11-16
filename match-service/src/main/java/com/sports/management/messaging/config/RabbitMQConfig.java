package com.sports.management.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE =
            "match.finished.queue";

    public static final String EXCHANGE =
            "match.exchange";

    public static final String ROUTING_KEY =
            "match.finished";

    @Bean
    public Queue matchFinishedQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public DirectExchange matchExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding matchBinding(
            Queue matchFinishedQueue,
            DirectExchange matchExchange) {

        return BindingBuilder
                .bind(matchFinishedQueue)
                .to(matchExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}