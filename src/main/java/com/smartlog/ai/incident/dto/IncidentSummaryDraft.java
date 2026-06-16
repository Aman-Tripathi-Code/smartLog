package com.smartlog.ai.incident.dto;

import java.util.List;

public record IncidentSummaryDraft(
        String summary,
        String probableCause,
        List<String> impactedServices,
        List<String> suggestedActions,
        String confidence,
        String summarizerType
) {
}
