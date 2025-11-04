package com.analytics.LogProcessor.service;

import com.analytics.LogProcessor.model.ActivityRecord;
import com.analytics.LogProcessor.model.EnrichedRecord;
import com.analytics.LogProcessor.model.EnrichmentResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class EnrichmentService {

    private final WebClient enrichmentWebClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public EnrichmentService(WebClient enrichmentWebClient, RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.enrichmentWebClient = enrichmentWebClient;
        this.retry = retryRegistry.retry("enrichmentService");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("enrichmentService");
    }

    public Mono<EnrichedRecord> enrichRecords(ActivityRecord activityRecord){

        return Mono.defer(() ->enrichmentWebClient.post()
                .bodyValue(buildRequestBody(activityRecord))
                .retrieve()
                .bodyToMono(EnrichmentResponse.class)
                .map( response -> mapToEnrichedRecord(activityRecord,response))
                .doOnError(error ->
                        log.debug("Error enriching record {}: {}, retry will happen", activityRecord.id(), error.getMessage())
                        //setting log level to debug to stop flooding app log with false alarms
                ))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .timeout(Duration.ofSeconds(10));
    }
    private Map<String, Object> buildRequestBody(ActivityRecord record){
        return Map.of(
                "id", record.id(),
                "asset", record.asset(),
                "ip", record.ip(),
                "category", record.category()
        );
    }
    private EnrichedRecord mapToEnrichedRecord(ActivityRecord record, EnrichmentResponse response){
        return  new EnrichedRecord(record.id(), record.asset(), record.ip(), response.category(), response.asn(),response.correlationId());
    }
}
