package com.analytics.LogProcessor.consumer;

import com.analytics.LogProcessor.annotation.TrackExecutionTime;
import com.analytics.LogProcessor.exception.MessagePublishException;
import com.analytics.LogProcessor.service.EnrichmentService;
import com.analytics.LogProcessor.service.MetricsService;
import com.analytics.LogProcessor.model.ActivityRecord;
import com.analytics.LogProcessor.model.EnrichedRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.analytics.LogProcessor.constants.LogProcessorConstants.ENRICHED_ROUTING_KEY;

/**
 * Consumer for enriching raw activity records.
 * Listens to raw-records-queue, enriches via Enrichment Service,
 * and publishes to enriched-records-queue.
 * Features:
 * - Concurrent processing (10-20 threads)
 * - Circuit breaker for Enrichment Service failures
 * - Automatic retry with exponential backoff
 * - Dead letter queue for failed records
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichmentConsumer {

    private final EnrichmentService enrichmentClient;
    private final RabbitTemplate rabbitTemplate;
    private final MetricsService metricsService;

    @Value("${exchange.main}")
    private String mainExchange;

    @Value("${queue.dead-letter}")
    private String deadLetterQueue;

    /**
     * Process messages from raw records queue.
     * Concurrent listeners: 4-8 threads (configured in application.yml)
     * Prefetch: 32 messages
     */
    @RabbitListener(queues = "${queue.raw-records}")
    public void processRawRecord(ActivityRecord record) {
        long startTime = System.currentTimeMillis();
        log.debug("Processing raw record: {} ", record.id());

        try {
            enrichmentClient.enrichRecords(record)
                    .doOnNext(enrichedRecord -> {
                        publishToEnrichedQueue(enrichedRecord, record);
                        metricsService.incrementRecordsEnriched();
                        long duration = System.currentTimeMillis() - startTime;
                        metricsService.recordEnrichmentTime(duration);
                    })
                    .block();

        } catch (Exception e) {
            metricsService.incrementEnrichmentFailures();
            sendToDeadLetterQueue(record, e);
        }
    }
    private void publishToEnrichedQueue(EnrichedRecord enrichedRecord,ActivityRecord rawRecord){
        try{
            rabbitTemplate.convertAndSend(mainExchange,ENRICHED_ROUTING_KEY, enrichedRecord);
        }catch(Exception e){
            log.error("Failed to publish enriched record {}: {}",rawRecord.id(),e.getMessage());
            sendToDeadLetterQueue(rawRecord, e);
            throw new MessagePublishException("Error publishing records to enriched queue",e);
        }
    }

    private void sendToDeadLetterQueue(ActivityRecord record,Throwable error){
        log.error("Enrichment failed for record: {}", record.id());
        try{
            FailedRecordMessage failedRecordMessage = new FailedRecordMessage(record, error.getClass().getSimpleName(), error.getMessage());
            rabbitTemplate.convertAndSend(deadLetterQueue, failedRecordMessage);
            log.error("Sent failed record :{} to DLQ", record.id());
        }catch (Exception e){
            log.error("Critical: Failed to publish record to DLQ: {} with Exception :{}  ", record.id(),  e.getMessage());
        }
    }

    /**
     * Message wrapper for failed records.
     */
    public record FailedRecordMessage(ActivityRecord record, String errorType, String error) {}
}

