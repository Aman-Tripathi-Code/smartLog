package com.smartlog.ai.incident.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentSummaryRecord(
        UUID incidentSummaryId,
        UUID alertId,
        String correlationId,
        String summary,
        String probableCause,
        List<String> impactedServices,
        List<String> suggestedActions,
        String confidence,
        String summarizerType,
        Instant createdAt
) {
}
