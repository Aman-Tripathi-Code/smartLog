package com.smartlog.processing.consumer;

import java.time.Instant;
import java.util.UUID;

public record DeadLetterRecord(
        UUID id,
        String eventId,
        String sourceTopic,
        String rawPayload,
        String reason,
        Instant createdAt
) {
}
