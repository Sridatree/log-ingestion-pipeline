package com.analytics.LogProcessor.consumer;

import com.analytics.LogProcessor.model.ActivityRecord;
import com.analytics.LogProcessor.model.EnrichedRecord;
import com.analytics.LogProcessor.service.EnrichmentService;
import com.analytics.LogProcessor.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnrichmentConsumerTest {

    private EnrichmentService enrichmentService;
    private MetricsService metricsService;
    private RabbitTemplate rabbitTemplate;
    private EnrichmentConsumer enrichmentConsumer;

    @BeforeEach
    void setUp() {
        enrichmentService = mock(EnrichmentService.class);
        metricsService = spy(new MetricsService());
        rabbitTemplate = mock(RabbitTemplate.class);
        enrichmentConsumer = new EnrichmentConsumer(enrichmentService, rabbitTemplate, metricsService);

        ReflectionTestUtils.setField(enrichmentConsumer, "deadLetterQueue", "dlq");
    }

    @Test
    void processRawRecord_success_publishesToAnalyticsQueue() {
        ActivityRecord record = new ActivityRecord(10L, "asset1", "9.9.9.9", "phishing");
        EnrichedRecord enriched = new EnrichedRecord(10L, "asset1", "9.9.9.9", "TS159", "AS1234", 23599);

        when(enrichmentService.enrichRecords(record)).thenReturn(Mono.just(enriched));

        enrichmentConsumer.processRawRecord(record);

        verify(rabbitTemplate, timeout(1000))
                .convertAndSend(any(), eq("enriched"), eq(enriched));

        verify(metricsService, atLeastOnce()).incrementRecordsEnriched();
        verify(metricsService, atLeastOnce()).recordEnrichmentTime(anyLong());

    }

    @Test
    void processRawRecord_failure_publishesToDlq() {
        ActivityRecord record = new ActivityRecord(11L, "x", "8.8.8.8", "phishing");
        when(enrichmentService.enrichRecords(record)).thenReturn(Mono.error(new RuntimeException("fail")));

        enrichmentConsumer.processRawRecord(record);

        verify(rabbitTemplate, timeout(1000)).convertAndSend(eq("dlq"), any(EnrichmentConsumer.FailedRecordMessage.class));
        // Do not throw: method should swallow publish errors and log them.
    }
}
