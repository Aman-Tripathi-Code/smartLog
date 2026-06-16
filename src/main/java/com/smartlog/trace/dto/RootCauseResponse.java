package com.smartlog.trace.dto;

import java.time.Instant;
import java.util.List;

public record RootCauseResponse(
        String correlationId,
        String serviceName,
        String message,
        String exceptionType,
        Instant timestamp,
        String transactionId,
        String userId,
        String confidence,
        String reason,
        List<TraceLogEvent> supportingEvents
) {
}
