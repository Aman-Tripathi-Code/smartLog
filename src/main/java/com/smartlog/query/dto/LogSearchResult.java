package com.smartlog.query.dto;

import java.time.Instant;

public record LogSearchResult(
        String eventId,
        Instant timestamp,
        String serviceName,
        String environment,
        String level,
        String message,
        String correlationId,
        String traceId,
        String userId,
        String transactionId,
        String exceptionType
) {
}
