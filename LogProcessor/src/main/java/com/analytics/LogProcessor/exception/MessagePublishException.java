package com.analytics.LogProcessor.exception;

/**
 * Exception thrown when publishing messages to RabbitMQ fails
 */
public class MessagePublishException extends RuntimeException {

    public MessagePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}

