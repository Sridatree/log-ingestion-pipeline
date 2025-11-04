package com.analytics.LogProcessor.consumer;

import com.analytics.LogProcessor.model.EnrichedRecord;
import com.analytics.LogProcessor.service.AnalyticsService;
import com.analytics.LogProcessor.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AnalyticsConsumerTest {

    private AnalyticsService analyticsService;
    private MetricsService metricsService;
    private AnalyticsConsumer analyticsConsumer;

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        metricsService = spy(new MetricsService());
        analyticsConsumer = new AnalyticsConsumer(analyticsService, metricsService);
    }

    @Test
    void processBatch_sendsToAnalytics_andUpdatesMetrics() {
        List<EnrichedRecord> batch = List.of(
                new EnrichedRecord(1L, "a.mp4", "1.1.1.1", "AUTH", "AS1", 100L),
                new EnrichedRecord(2L, "b.mp4", "2.2.2.2", "MEDIA", "AS2", 101L)
        );

        analyticsConsumer.consumeBatch(batch);

        verify(analyticsService, times(1)).sendBatchToAnalytics(batch);
        assertEquals(2, metricsService.getRecordsSentToAnalytics().get());
        assertEquals(1, metricsService.getBatchesProcessed().get());
    }
}
