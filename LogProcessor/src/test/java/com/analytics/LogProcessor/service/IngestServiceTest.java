package com.analytics.LogProcessor.service;

import com.analytics.LogProcessor.constants.LogProcessorConstants;
import com.analytics.LogProcessor.model.ActivityRecord;
import com.analytics.LogProcessor.model.IngestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IngestService
 */
@ExtendWith(MockitoExtension.class)
class IngestServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private IngestService ingestService;

    private static final String MAIN_EXCHANGE = "test-exchange";
    private static final String AUTH_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ingestService, "mainExchange", MAIN_EXCHANGE);
    }

    @Test
    void testIngestSuccess() {
        // Arrange
        List<ActivityRecord> records = Arrays.asList(
                new ActivityRecord(1L, "asset1", "192.168.1.1", "phishing"),
                new ActivityRecord(2L, "asset2", "192.168.1.2", "phishing")
        );

        doNothing().when(rabbitTemplate).convertAndSend(eq(MAIN_EXCHANGE), eq(LogProcessorConstants.RAW_ROUTING_KEY), any(ActivityRecord.class));
        doNothing().when(metricsService).incrementRecordsIngested(anyInt());
        when(metricsService.getRecordsIngested()).thenReturn(new java.util.concurrent.atomic.AtomicLong(2));

        // Act
        IngestResponse response = ingestService.ingest(records, AUTH_TOKEN);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getRecordsIngested());
        assertEquals("Records Ingested to raw queue", response.getMessage());
        
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(ActivityRecord.class));
        verify(metricsService).incrementRecordsIngested(2);
    }

    @Test
    void testIngestPartialFailure() {
        // Arrange
        List<ActivityRecord> records = Arrays.asList(
                new ActivityRecord(1L, "asset1", "192.168.1.1", "phishing"),
                new ActivityRecord(2L, "asset2", "192.168.1.2", "phishing")
        );

        // First call succeeds, second fails
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(records.get(0)));
        doThrow(new RuntimeException("RabbitMQ error"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(records.get(1)));
        
        doNothing().when(metricsService).incrementRecordsIngested(anyInt());
        when(metricsService.getRecordsIngested()).thenReturn(new java.util.concurrent.atomic.AtomicLong(1));

        // Act
        IngestResponse response = ingestService.ingest(records, AUTH_TOKEN);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getRecordsIngested()); // Only one succeeded
        verify(metricsService).incrementRecordsIngested(1);
    }

    @Test
    void testIngestEmptyList() {
        // Arrange
        List<ActivityRecord> records = List.of();
        doNothing().when(metricsService).incrementRecordsIngested(anyInt());
        when(metricsService.getRecordsIngested()).thenReturn(new java.util.concurrent.atomic.AtomicLong(0));

        // Act
        IngestResponse response = ingestService.ingest(records, AUTH_TOKEN);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getRecordsIngested());
        verify(rabbitTemplate, never()).convertAndSend(eq(MAIN_EXCHANGE), eq(LogProcessorConstants.RAW_ROUTING_KEY), any(ActivityRecord.class));
        verify(metricsService).incrementRecordsIngested(0);
    }

    @Test
    void testIngestAllFailures() {
        // Arrange
        List<ActivityRecord> records = List.of(
                new ActivityRecord(1L, "asset1", "192.168.1.1", "phishing")
        );

        doThrow(new RuntimeException("RabbitMQ error"))
                .when(rabbitTemplate).convertAndSend(eq(MAIN_EXCHANGE), eq(LogProcessorConstants.RAW_ROUTING_KEY), any(ActivityRecord.class));
        
        doNothing().when(metricsService).incrementRecordsIngested(anyInt());
        when(metricsService.getRecordsIngested()).thenReturn(new java.util.concurrent.atomic.AtomicLong(0));

        // Act
        IngestResponse response = ingestService.ingest(records, AUTH_TOKEN);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getRecordsIngested());
        verify(metricsService).incrementRecordsIngested(0);
    }
}

