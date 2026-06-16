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
        Map<String, Object> attributes
) {
}
