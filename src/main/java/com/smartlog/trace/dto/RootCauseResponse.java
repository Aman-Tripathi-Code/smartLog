package com.smartlog.trace.dto;

import java.time.Instant;

public record RootCauseResponse(
        String correlationId,
        String serviceName,
        String message,
        String exceptionType,
        Instant timestamp,
        String transactionId,
        String userId,
        String confidence
) {
}
