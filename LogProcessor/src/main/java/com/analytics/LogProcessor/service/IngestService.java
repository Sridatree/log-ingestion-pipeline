package com.analytics.LogProcessor.service;

import java.util.List;

import com.analytics.LogProcessor.exception.MessagePublishException;
import com.analytics.LogProcessor.model.ActivityRecord;
import com.analytics.LogProcessor.model.IngestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import static com.analytics.LogProcessor.constants.LogProcessorConstants.RAW_ROUTING_KEY;




@Service
@Slf4j
public class IngestService {

    private final RabbitTemplate rabbitTemplate;
    private final MetricsService metricsService;

    @Value("${exchange.main}")
    private String mainExchange;

    public IngestService(RabbitTemplate rabbitTemplate, MetricsService metricsService){
        this.rabbitTemplate = rabbitTemplate;
        this.metricsService = metricsService;
    }

    public IngestResponse ingest(List<ActivityRecord> activityRecordList, String authToken){
        int successCount = 0;

        for (ActivityRecord activityRecord : activityRecordList) {
            try{
                rabbitTemplate.convertAndSend(mainExchange,RAW_ROUTING_KEY, activityRecord);
                successCount++;
                log.debug("Published record {} to raw queue", activityRecord.id());
            }catch(Exception ex){
                log.error("Failed to publish record {} to raw queue: {}",
                        activityRecord.id(), ex.getMessage());
            }
        }

        metricsService.incrementRecordsIngested(successCount);
        log.info("Ingested {} out of {} records to raw queue (Total ingested: {})",
                successCount, activityRecordList.size(), metricsService.getRecordsIngested().get());

        return IngestResponse.builder()
                .recordsIngested(successCount)
                .message("Records Ingested to raw queue")
                .build();
    }
}
