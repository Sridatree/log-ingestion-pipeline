package com.analytics.LogProcessor.exception;

/**
 * Exception thrown when analytics service operations fail
 */
public class AnalyticsException extends RuntimeException {

    public AnalyticsException(String message, Throwable cause) {
        super(message, cause);
    }
}

