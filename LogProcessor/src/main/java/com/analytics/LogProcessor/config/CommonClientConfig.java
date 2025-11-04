package com.analytics.LogProcessor.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * WebClient configuration for HTTP calls to enrichment and analytics services.
 */
@Configuration
public class CommonClientConfig {

    @Value("${app.enrichment.url}")
    private String enrichmentUrl;

    @Value("${app.analytics.url}")
    private String analyticsUrl;

    @Value("${app.auth.header}")
    private String authHeader;

    /**
     * Web client configured for enrichment service to make use of
     * event loop to send requests concurrent to Enrichment API
     * @return enriched response
     */
    @Bean
    public WebClient enrichmentWebClient() {

        ConnectionProvider provider = ConnectionProvider.builder("enrichment-pool")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.ofMillis(500))
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofMinutes(5))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(10));



        return WebClient.builder()
                .baseUrl(enrichmentUrl)
                .defaultHeader("Authorization", authHeader)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Rest client configured for analytics service
     * Blocking rest client used , sequential processing as 1 request per 10 seconds
     * @return analytics service response
     */
    @Bean
    public RestClient analyticsRestClient() {
        // Simple blocking HTTP client for analytics service (sequential batches, 1 request per 10 seconds)
        return RestClient.builder()
                .baseUrl(analyticsUrl)
                .defaultHeader("Authorization", authHeader)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(Duration.ofSeconds(10));
                    setReadTimeout(Duration.ofSeconds(10));
                }})
                .build();
    }
}

