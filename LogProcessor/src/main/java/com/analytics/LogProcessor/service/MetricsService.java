package com.analytics.LogProcessor.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory metrics service to track processing statistics.
 * Provides counters for monitoring the log processing pipeline.
 */
@Service
@Slf4j
@Getter
public class MetricsService {

    // Ingestion metrics
    private final AtomicLong recordsIngested = new AtomicLong(0);

    // Enrichment metrics
    private final AtomicLong recordsEnriched = new AtomicLong(0);
    private final AtomicLong enrichmentFailures = new AtomicLong(0);

    // Analytics metrics
    private final AtomicLong batchesProcessed = new AtomicLong(0);
    private final AtomicLong recordsSentToAnalytics = new AtomicLong(0);
    private final AtomicLong analyticsFailures = new AtomicLong(0);

    // Timing metrics (in milliseconds)
    private final AtomicLong totalBatchProcessingTime = new AtomicLong(0);
    private final AtomicLong totalEnrichmentTime = new AtomicLong(0);

    // Ingestion
    public void incrementRecordsIngested(int count) {
        recordsIngested.addAndGet(count);
        log.debug("Total records ingested: {}", recordsIngested.get());
    }

    // Enrichment
    public void incrementRecordsEnriched() {
        recordsEnriched.incrementAndGet();
    }

    public void incrementEnrichmentFailures() {
        enrichmentFailures.incrementAndGet();
    }

    public void recordEnrichmentTime(long milliseconds) {
        totalEnrichmentTime.addAndGet(milliseconds);
    }

    // Analytics
    public void incrementBatchesProcessed() {
        batchesProcessed.incrementAndGet();
    }

    public void incrementRecordsSentToAnalytics(int count) {
        recordsSentToAnalytics.addAndGet(count);
    }

    public void incrementAnalyticsFailures() {
        analyticsFailures.incrementAndGet();
    }

    public void recordBatchProcessingTime(long milliseconds) {
        totalBatchProcessingTime.addAndGet(milliseconds);
        log.info("[METRICS] Total batch processing time: {} ms, Batches processed: {}",
                totalBatchProcessingTime.get(), batchesProcessed.get());
    }

}

