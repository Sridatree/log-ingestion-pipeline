package com.analytics.LogProcessor.consumer;

import com.analytics.LogProcessor.annotation.TrackExecutionTime;
import com.analytics.LogProcessor.exception.AnalyticsException;
import com.analytics.LogProcessor.service.AnalyticsService;
import com.analytics.LogProcessor.service.MetricsService;
import com.analytics.LogProcessor.model.EnrichedRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {

    private final AnalyticsService analyticsService;
    private final MetricsService metricsService;

    @TrackExecutionTime("AnalyticsConsumer#consumeBatch")
    @RabbitListener(
            queues = "${queue.enriched-records}",
            containerFactory = "analyticsBatchContainerFactory"
    )
    public void consumeBatch(List<EnrichedRecord> batch) {
        long startTime = System.currentTimeMillis();
        log.info("Received batch of {} records from RabbitMQ", batch.size());

        try {
            analyticsService.sendBatchToAnalytics(batch);
            metricsService.incrementRecordsSentToAnalytics(batch.size());
            log.info("Successfully sent sub-batch of {} records to Analytics (Total sent: {})",
                    batch.size(), metricsService.getRecordsSentToAnalytics().get());
        } catch (Exception e) {
            metricsService.incrementAnalyticsFailures();
            throw e;
            // With AUTO ack and DLQ configured, throwing the exception will send the batch to DLQ
        }

        // Record metrics after successful batch processing
        long duration = System.currentTimeMillis() - startTime;
        metricsService.incrementBatchesProcessed();
        metricsService.recordBatchProcessingTime(duration);

        // Log both current batch time and cumulative total
        long totalTime = metricsService.getTotalBatchProcessingTime().get();
        long batchCount = metricsService.getBatchesProcessed().get();

        log.info("Completed processing batch of {} records in {} ms", batch.size(), duration);
        log.info(" ANALYTICS TOTAL: {} batches processed, {} records sent, TOTAL TIME: {} ms ({} sec)",
                batchCount,
                metricsService.getRecordsSentToAnalytics().get(),
                totalTime,
                totalTime / 1000.0);
    }
}
