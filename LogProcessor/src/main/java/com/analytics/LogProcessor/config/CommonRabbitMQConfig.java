package com.analytics.LogProcessor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized RabbitMQ configuration class.
 *
 * Responsibilities:
 *  - Define all exchanges, queues, and bindings used by the LogProcessor microservice.
 *  - Configure JSON-based message conversion for RabbitMQ message serialization/deserialization.
 *  - Configure RabbitTemplate and listener container for consistent messaging behavior.
 */
@Configuration
public class CommonRabbitMQConfig {

    @Value("${queue.raw-records}")
    private String rawRecordsQueue;

    @Value("${queue.enriched-records}")
    private String enrichedRecordsQueue;

    @Value("${queue.dead-letter}")
    private String deadLetterQueue;

    @Value("${exchange.main}")
    private String mainExchange;

    @Value("${exchange.dlx}")
    private String dlxExchange;

    // ==================== Message Converter ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());

        return factory;
    }

    // ==================== Exchanges ====================

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(mainExchange, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxExchange, true, false);
    }

    // ==================== Queues ====================

    @Bean
    public Queue rawRecordsQueue() {
        return QueueBuilder.durable(rawRecordsQueue)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-routing-key", "dlq")  //Configured DLQ so failed message are re-routed automatically
                .build();
    }

    @Bean
    public Queue enrichedRecordsQueue() {
        return QueueBuilder.durable(enrichedRecordsQueue)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    // ==================== Bindings ====================

    @Bean
    public Binding rawRecordsBinding(Queue rawRecordsQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(rawRecordsQueue)
                .to(mainExchange)
                .with("raw");
    }

    @Bean
    public Binding enrichedRecordsBinding(Queue enrichedRecordsQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(enrichedRecordsQueue)
                .to(mainExchange)
                .with("enriched");
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("dlq");
    }
}

