package com.analytics.LogProcessor.exception;

/**
 * Exception thrown when rate limit is exceeded for external API calls
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

