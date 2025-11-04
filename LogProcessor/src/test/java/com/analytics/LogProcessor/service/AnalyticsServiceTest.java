package com.analytics.LogProcessor.service;

import com.analytics.LogProcessor.model.AnalyticsBatchResponse;
import com.analytics.LogProcessor.model.EnrichedRecord;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsService
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private RestClient analyticsRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private Retry retry;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        when(rateLimiterRegistry.rateLimiter("analyticsService")).thenReturn(rateLimiter);
        when(retryRegistry.retry("analyticsService")).thenReturn(retry);

        // Configure default rate limiter behavior
        when(rateLimiter.executeSupplier(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, java.util.function.Supplier.class).get();
        });

        // Configure default retry behavior
        when(retry.executeSupplier(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, java.util.function.Supplier.class).get();
        });

        analyticsService = new AnalyticsService(analyticsRestClient, rateLimiterRegistry, retryRegistry);
    }

    @Test
    void testSendBatchToAnalyticsSuccess() {
        // Arrange
        List<EnrichedRecord> batch = Arrays.asList(
                new EnrichedRecord(1L, "asset1", "192.168.1.1", "T1659", "ASN1337", 23517),
                new EnrichedRecord(2L, "asset2", "192.168.1.2", "T1659", "ASN1337", 23518)
        );

        AnalyticsBatchResponse expectedResponse = new AnalyticsBatchResponse("Success",2);

        when(analyticsRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AnalyticsBatchResponse.class)).thenReturn(expectedResponse);

        // Act
        AnalyticsBatchResponse response = analyticsService.sendBatchToAnalytics(batch);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.itemsIngested());
        assertEquals("Success", response.status());

        verify(analyticsRestClient).post();
        verify(rateLimiter).executeSupplier(any());
        verify(retry).executeSupplier(any());
    }

    @Test
    void testSendBatchToAnalyticsTooManyRequests() {
        // Arrange
        List<EnrichedRecord> batch = List.of(
                new EnrichedRecord(1L, "asset1", "192.168.1.1", "T1659", "ASN1337", 12889)
        );

        when(analyticsRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AnalyticsBatchResponse.class))
                .thenThrow(HttpClientErrorException.TooManyRequests.class);

        // Act & Assert
        assertThrows(HttpClientErrorException.TooManyRequests.class, () -> {
            analyticsService.sendBatchToAnalytics(batch);
        });

        verify(analyticsRestClient).post();
    }

}

