package com.analytics.LogProcessor.controller;

import com.analytics.LogProcessor.model.IngestRequest;
import com.analytics.LogProcessor.model.IngestResponse;
import com.analytics.LogProcessor.service.IngestService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * This controller exposes a REST API endpoint for the LogProcessor service.
 * The endpoint allows the CLI script to send batches of raw activity records
 * to be raw queue. The listeners are then responsible for enriching messages
 * from the raw queue and ultimately sending them to the analytics service.
 */

@RestController
@RequestMapping("/private/v1")
@Slf4j
public class LogProcessorController {

    private final IngestService ingestService;

    public LogProcessorController(IngestService ingestService){
        this.ingestService = ingestService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest(
            @Valid @RequestBody IngestRequest ingestRequest,
            @RequestHeader("Authorization") String authorization){
        log.info("Received ingest request with {} records",ingestRequest.activityRecordList().size());

        IngestResponse ingestResponse = ingestService.ingest(ingestRequest.activityRecordList(),authorization);
        return ResponseEntity.ok(ingestResponse);

    }
}
