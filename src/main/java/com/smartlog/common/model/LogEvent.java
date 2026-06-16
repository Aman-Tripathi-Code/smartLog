package com.smartlog.common.model;

import java.time.Instant;
import java.util.Map;

import com.smartlog.common.enums.LogLevel;

public record LogEvent(
        String eventId,
        Instant eventTimestamp,
        Instant receivedAt,
        String serviceName,
        String environment,
        LogLevel level,
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
        Map<String, Object> attributes,
        String messageHash,
        String exceptionFingerprint,
        int severityScore,
        long ingestionDelayMs,
        boolean errorEvent
) {

    public LogEvent(
            String eventId,
            Instant eventTimestamp,
            Instant receivedAt,
            String serviceName,
            String environment,
            LogLevel level,
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
        this(
                eventId,
                eventTimestamp,
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
                stackTrace,
                attributes,
                null,
                null,
                level == null ? 0 : level.severityScore(),
                eventTimestamp == null || receivedAt == null ? 0 : Math.max(0, receivedAt.toEpochMilli() - eventTimestamp.toEpochMilli()),
                level == LogLevel.ERROR || level == LogLevel.FATAL
        );
    }
}
