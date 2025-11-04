package com.analytics.LogProcessor.service;

import com.analytics.LogProcessor.constants.LogProcessorConstants;
import com.analytics.LogProcessor.exception.AnalyticsException;
import com.analytics.LogProcessor.exception.RateLimitExceededException;
import com.analytics.LogProcessor.model.AnalyticsBatchResponse;
import com.analytics.LogProcessor.model.EnrichedRecord;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Client service for calling the Analytics API.
 * Uses Resilience4j for rate limiting and retries for non 429 errors.
 * STRICT LIMIT: Maximum 20 records per batch, 1 batch per 10 seconds.
 */
@Service
@Slf4j
public class AnalyticsService {

    private final RestClient analyticsRestClient;
    private final RateLimiter rateLimiter;
    private final Retry retry;
    @Value("${app.analytics.batch-size}")
    private int batchSize;


    public AnalyticsService(RestClient analyticsRestClient,
                            RateLimiterRegistry rateLimiterRegistry,
                            RetryRegistry retryRegistry){
        this.analyticsRestClient = analyticsRestClient;
        this.rateLimiter = rateLimiterRegistry.rateLimiter(LogProcessorConstants.ANALYTICS_SERVICE);
        this.retry = retryRegistry.retry(LogProcessorConstants.ANALYTICS_SERVICE);
    }

    public AnalyticsBatchResponse sendBatchToAnalytics(List<EnrichedRecord> batch) {

        final List<EnrichedRecord> finalBatch = batch;
        final int batchSize = finalBatch.size();

        log.info("Rate limiter: Waiting for permission to send {} records to Analytics...", batchSize);

        return retry.executeSupplier(() ->
                rateLimiter.executeSupplier(() -> {
                    try {
                        log.info("Rate limiter: Permission granted! Sending {} records to Analytics API", batchSize);

                        AnalyticsBatchResponse response = analyticsRestClient.post()
                                .body(finalBatch)
                                .retrieve()
                                .body(AnalyticsBatchResponse.class);

                        log.info("Successfully sent {} records to Analytics (API ingested: {})",
                                batchSize, response != null ? response.itemsIngested() : 0);
                        return response;

                    } catch (HttpClientErrorException.TooManyRequests e) {
                        // 429 - Too Many Requests: DO NOT RETRY, this means rate limiting failed
                        log.error(" 429 TOO MANY REQUESTS from Analytics API! Tune the rate limiting config.");
                        log.error("Rate limit exceeded. Batch of {} records FAILED. Not retrying 429 errors.", batchSize);
                        throw new RateLimitExceededException("Rate limit exceeded",e);

                    } catch (HttpClientErrorException e) {
                        // Other 4xx errors (not 429)
                        log.error(" Client error sending {} records to Analytics: {} - {}",
                                batchSize, e.getStatusCode(), e.getMessage());
                        throw new AnalyticsException("Request Error sending records to Analytics", e);

                    } catch (Exception e) {
                        log.error(" Error sending {} records to Analytics: {}", batchSize, e.getMessage());
                        throw new AnalyticsException("Error sending records to analytics",e);
                    }
                })
        );
    }

}