package com.smartlog.ingestion.dto;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record LogIngestionRequest(
        String eventId,
        Instant timestamp,
        @NotBlank(message = "serviceName is required")
        String serviceName,
        String environment,
        @NotBlank(message = "level is required")
        String level,
        @NotBlank(message = "message is required")
        String message,
        String correlationId,
        String traceId,
        String spanId,
        String parentSpanId,
        String userId,
        String transactionId,
        String module,
        String exceptionType,
        String stackTrace,
        Map<String, Object> attributes
) {
}
