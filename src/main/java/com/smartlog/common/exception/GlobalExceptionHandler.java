package com.smartlog.common.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.smartlog.ingestion.kafka.KafkaPublishException;
import com.smartlog.ingestion.pipeline.LogQueueFullException;
import com.smartlog.ingestion.pipeline.LogPipelineMetrics;
import com.smartlog.ingestion.validation.InvalidLogRequestException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final LogPipelineMetrics metrics;

    public GlobalExceptionHandler(LogPipelineMetrics metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        metrics.incrementRejected(1);
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return badRequest(details);
    }

    @ExceptionHandler(InvalidLogRequestException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidLogRequest(InvalidLogRequestException exception) {
        metrics.incrementRejected(1);
        return badRequest(List.of(exception.getMessage()));
    }

    @ExceptionHandler(LogQueueFullException.class)
    ResponseEntity<ApiErrorResponse> handleQueueFull(LogQueueFullException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiErrorResponse("INGESTION_QUEUE_FULL", List.of(exception.getMessage()), Instant.now()));
    }

    @ExceptionHandler(KafkaPublishException.class)
    ResponseEntity<ApiErrorResponse> handleKafkaPublish(KafkaPublishException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("KAFKA_UNAVAILABLE", List.of(exception.getMessage()), Instant.now()));
    }

    private ResponseEntity<ApiErrorResponse> badRequest(List<String> details) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_REQUEST", details, Instant.now()));
    }
}
