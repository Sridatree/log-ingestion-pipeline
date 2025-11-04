package com.analytics.LogProcessor.controller;

import com.analytics.LogProcessor.exception.*;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.*;

@Hidden //Hiding this from open api scan so the records in this class does not break the openAPI. Thread: https://github.com/OpenAPITools/openapi-generator/issues/10490
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    public record ErrorResponse(int status, String message, LocalDateTime timeStamp){}
    public record ValidationErrorResponse(int status, String message, Map<String,String> errors, LocalDateTime timeStamp){}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
         ex.getBindingResult()
                 .getFieldErrors()
                 .forEach(fieldError -> {
                     String fieldName = fieldError.getField();
                     String errorMessage = fieldError.getDefaultMessage();
                     errors.put(fieldName,errorMessage);
                 });
         log.error("Validation failed with the following errors: {}", errors);
         ValidationErrorResponse response = new ValidationErrorResponse
                 (HttpStatus.BAD_REQUEST.value(),"Validation failed",errors, LocalDateTime.now());
         return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AnalyticsException.class)
    public ResponseEntity<ErrorResponse> handleAnalyticsException(AnalyticsException ex) {
        log.error("Analytics exception occurred: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Analytics service error: " + ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(MessagePublishException.class)
    public ResponseEntity<ErrorResponse> handleMessagePublishException(MessagePublishException ex) {
        log.error("Message publish exception occurred: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Failed to publish message: " + ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimitExceededException ex) {
        log.error("Rate limit exceeded: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Rate limit exceeded: " + ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex){
        log.error("Unexpected exception occurred", ex);
        ErrorResponse errorResponse = new ErrorResponse
                (HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error:"+ex.getMessage(), LocalDateTime.now());
        return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
