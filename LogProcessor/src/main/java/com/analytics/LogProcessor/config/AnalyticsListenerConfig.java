package com.analytics.LogProcessor.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static com.analytics.LogProcessor.constants.LogProcessorConstants.ANALYTICS_MAX_BATCH_SIZE;

/**
 * This configuration class is meant especially for the analytics
 * consumer to enable listening of batched records, capped at 20
 */
@Configuration
public class AnalyticsListenerConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory analyticsBatchContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        factory.setBatchListener(true);
        factory.setBatchSize(ANALYTICS_MAX_BATCH_SIZE);
        factory.setConsumerBatchEnabled(true);
        factory.setReceiveTimeout(1000L);

        //  limit how many messages broker sends at once
        factory.setPrefetchCount(20);
        //no manual acknowledgement of messages, listener monitors and queues to DLQ in case of failure
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);

        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);

        return factory;
    }
}
