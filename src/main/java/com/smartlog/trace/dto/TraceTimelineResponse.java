package com.smartlog.trace.dto;

import java.util.List;

public record TraceTimelineResponse(
        String correlationId,
        int totalEvents,
        List<TraceLogEvent> events
) {
}
