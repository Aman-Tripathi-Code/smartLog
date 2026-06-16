package com.smartlog.alerting.model;

import java.time.Instant;
import java.util.UUID;

public record AlertRecord(
        UUID alertId,
        String alertType,
        String severity,
        String serviceName,
        String message,
        Instant windowStart,
        Instant windowEnd,
        int eventCount,
        String status,
        String sampleCorrelationId,
        String sampleTransactionId,
        Instant createdAt,
        Instant updatedAt
) {
}
