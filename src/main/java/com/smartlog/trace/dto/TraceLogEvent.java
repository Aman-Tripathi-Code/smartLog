package com.smartlog.trace.dto;

import java.time.Instant;

public record TraceLogEvent(
        String eventId,
        Instant timestamp,
        Instant receivedAt,
        String serviceName,
        String environment,
        String level,
        String message,
        String correlationId,
        String traceId,
        String spanId,
        String parentSpanId,
        String userId,
        String transactionId,
        String module,
        String exceptionType,
        String stackTrace
) {
    public TraceLogEvent(
            String eventId,
            Instant timestamp,
            Instant receivedAt,
            String serviceName,
            String environment,
            String level,
            String message,
            String correlationId,
            String traceId,
            String spanId,
            String parentSpanId,
            String userId,
            String transactionId,
            String module,
            String exceptionType
    ) {
        this(
                eventId,
                timestamp,
                receivedAt,
                serviceName,
                environment,
                level,
                message,
                correlationId,
                traceId,
                spanId,
                parentSpanId,
                userId,
                transactionId,
                module,
                exceptionType,
                null
        );
    }
}
