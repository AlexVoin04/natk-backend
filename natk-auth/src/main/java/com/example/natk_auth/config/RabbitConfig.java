package com.example.natk_auth.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "auth.events";
    public static final String RESET_REQUESTED_QUEUE = "auth.password-reset.requested";
    public static final String RESET_REQUESTED_ROUTING_KEY = "password-reset.requested";

    @Bean
    public DirectExchange authExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue passwordResetQueue() {
        return QueueBuilder.durable(RESET_REQUESTED_QUEUE).build();
    }

    @Bean
    public Binding passwordResetBinding(Queue passwordResetQueue, DirectExchange authExchange) {
        return BindingBuilder.bind(passwordResetQueue)
                .to(authExchange)
                .with(RESET_REQUESTED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }
}
