package com.smartlog.query.dto;

import java.time.Instant;

public record LogSearchCriteria(
        String serviceName,
        String level,
        Instant from,
        Instant to,
        String keyword,
        String correlationId,
        String traceId,
        String userId,
        String transactionId,
        int page,
        int size
) {

    public int offset() {
        return page * size;
    }
}
