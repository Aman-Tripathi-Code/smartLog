package com.smartlog.ai.incident.context;

import java.util.List;

import com.smartlog.trace.dto.RootCauseResponse;
import com.smartlog.trace.dto.TraceLogEvent;

public record IncidentContext(
        String correlationId,
        String transactionId,
        String userId,
        List<String> impactedServices,
        RootCauseResponse rootCause,
        List<TraceLogEvent> traceEvents
) {
}
