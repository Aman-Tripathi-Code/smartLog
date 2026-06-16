package com.smartlog.ingestion.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.dto.LogIngestionRequest;

@Component
public class LogEventMapper {

    private final Clock clock;

    public LogEventMapper(Clock clock) {
        this.clock = clock;
    }

    public LogEvent toLogEvent(LogIngestionRequest request) {
        Instant receivedAt = Instant.now(clock);
        return new LogEvent(
                eventIdOrGenerate(request.eventId()),
                request.timestamp() == null ? receivedAt : request.timestamp(),
                receivedAt,
                request.serviceName().trim(),
                trimToNull(request.environment()),
                LogLevel.from(request.level()),
                request.message().trim(),
                trimToNull(request.correlationId()),
                trimToNull(request.traceId()),
                trimToNull(request.spanId()),
                trimToNull(request.parentSpanId()),
                trimToNull(request.userId()),
                trimToNull(request.transactionId()),
                trimToNull(request.module()),
                trimToNull(request.exceptionType()),
                request.stackTrace(),
                request.attributes() == null ? Map.of() : Map.copyOf(request.attributes())
        );
    }

    private String eventIdOrGenerate(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return eventId.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
