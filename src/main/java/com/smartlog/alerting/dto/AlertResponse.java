package com.smartlog.alerting.dto;

import java.time.Instant;

import com.smartlog.alerting.model.AlertRecord;

public record AlertResponse(
        String alertId,
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

    public static AlertResponse from(AlertRecord alert) {
        return new AlertResponse(
                alert.alertId().toString(),
                alert.alertType(),
                alert.severity(),
                alert.serviceName(),
                alert.message(),
                alert.windowStart(),
                alert.windowEnd(),
                alert.eventCount(),
                alert.status(),
                alert.sampleCorrelationId(),
                alert.sampleTransactionId(),
                alert.createdAt(),
                alert.updatedAt()
        );
    }
}
