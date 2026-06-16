package com.smartlog.ai.incident.dto;

import java.time.Instant;
import java.util.List;

import com.smartlog.ai.incident.model.IncidentSummaryRecord;

public record IncidentSummaryResponse(
        String incidentSummaryId,
        String correlationId,
        String summary,
        String probableCause,
        List<String> impactedServices,
        List<String> suggestedActions,
        String confidence,
        String summarizerType,
        Instant createdAt
) {

    public static IncidentSummaryResponse from(IncidentSummaryRecord record) {
        return new IncidentSummaryResponse(
                record.incidentSummaryId().toString(),
                record.correlationId(),
                record.summary(),
                record.probableCause(),
                record.impactedServices(),
                record.suggestedActions(),
                record.confidence(),
                record.summarizerType(),
                record.createdAt()
        );
    }
}
