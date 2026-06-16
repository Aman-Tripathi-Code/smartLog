package com.smartlog.ingestion.dto;

import java.time.Instant;
import java.util.List;

public record BatchLogIngestionResponse(
        String status,
        int accepted,
        int rejected,
        List<String> eventIds,
        Instant acceptedAt
) {
}
