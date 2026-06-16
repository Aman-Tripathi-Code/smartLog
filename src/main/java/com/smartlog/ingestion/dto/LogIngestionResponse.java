package com.smartlog.ingestion.dto;

import java.time.Instant;

public record LogIngestionResponse(
        String status,
        String eventId,
        Instant acceptedAt
) {
}
