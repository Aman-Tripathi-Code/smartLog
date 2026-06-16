package com.smartlog.trace.dto;

import java.time.Instant;
import java.util.List;

public record TraceTimelineResponse(
        String correlationId,
        String transactionId,
        String userId,
        String status,
        long durationMs,
        String highestSeverity,
        List<String> services,
        Instant firstEventTime,
        Instant lastEventTime,
        int totalEvents,
        List<TraceLogEvent> events
) {
}
