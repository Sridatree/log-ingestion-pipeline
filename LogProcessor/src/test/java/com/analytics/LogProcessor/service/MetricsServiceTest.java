package com.analytics.LogProcessor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricsService
 */
class MetricsServiceTest {

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();
    }

    @Test
    void testIncrementRecordsIngested() {
        metricsService.incrementRecordsIngested(5);
        assertEquals(5, metricsService.getRecordsIngested().get());

        metricsService.incrementRecordsIngested(3);
        assertEquals(8, metricsService.getRecordsIngested().get());
    }

    @Test
    void testIncrementRecordsEnriched() {
        metricsService.incrementRecordsEnriched();
        assertEquals(1, metricsService.getRecordsEnriched().get());

        metricsService.incrementRecordsEnriched();
        assertEquals(2, metricsService.getRecordsEnriched().get());
    }

    @Test
    void testIncrementEnrichmentFailures() {
        metricsService.incrementEnrichmentFailures();
        assertEquals(1, metricsService.getEnrichmentFailures().get());

        metricsService.incrementEnrichmentFailures();
        assertEquals(2, metricsService.getEnrichmentFailures().get());
    }

    @Test
    void testRecordEnrichmentTime() {
        metricsService.recordEnrichmentTime(100);
        assertEquals(100, metricsService.getTotalEnrichmentTime().get());

        metricsService.recordEnrichmentTime(200);
        assertEquals(300, metricsService.getTotalEnrichmentTime().get());
    }

    @Test
    void testIncrementBatchesProcessed() {
        metricsService.incrementBatchesProcessed();
        assertEquals(1, metricsService.getBatchesProcessed().get());
    }

    @Test
    void testIncrementRecordsSentToAnalytics() {
        metricsService.incrementRecordsSentToAnalytics(10);
        assertEquals(10, metricsService.getRecordsSentToAnalytics().get());

        metricsService.incrementRecordsSentToAnalytics(5);
        assertEquals(15, metricsService.getRecordsSentToAnalytics().get());
    }

    @Test
    void testIncrementAnalyticsFailures() {
        metricsService.incrementAnalyticsFailures();
        assertEquals(1, metricsService.getAnalyticsFailures().get());
    }
}

