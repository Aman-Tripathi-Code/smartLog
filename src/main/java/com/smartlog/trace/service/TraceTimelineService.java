package com.smartlog.trace.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.storage.repository.LogRepository;
import com.smartlog.trace.analyzer.BasicRootCauseAnalyzer;
import com.smartlog.trace.dto.RootCauseResponse;
import com.smartlog.trace.dto.TraceLogEvent;
import com.smartlog.trace.dto.TraceTimelineResponse;

@Service
public class TraceTimelineService {

    private final LogRepository repository;
    private final BasicRootCauseAnalyzer rootCauseAnalyzer;

    public TraceTimelineService(LogRepository repository, BasicRootCauseAnalyzer rootCauseAnalyzer) {
        this.repository = repository;
        this.rootCauseAnalyzer = rootCauseAnalyzer;
    }

    public TraceTimelineResponse timeline(String correlationId) {
        return timeline(correlationId, null, null, false);
    }

    public TraceTimelineResponse timeline(String correlationId, String level, String serviceName, boolean includeStackTrace) {
        List<TraceLogEvent> events = filter(findEvents(correlationId), level, serviceName, includeStackTrace);
        if (events.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No logs matched trace filters for correlationId " + correlationId);
        }
        return response(events.getFirst().correlationId(), events);
    }

    public TraceTimelineResponse timelineByTransaction(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }
        List<TraceLogEvent> events = repository.findByTransactionId(transactionId.trim());
        if (events.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No logs found for transactionId " + transactionId);
        }
        return response(events.getFirst().correlationId(), events);
    }

    public List<TraceTimelineResponse> timelinesByUser(String userId, int limit) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        List<String> correlationIds = repository.findRecentCorrelationIdsByUserId(userId.trim(), Math.max(1, Math.min(limit, 50)));
        if (correlationIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No logs found for userId " + userId);
        }
        return correlationIds.stream()
                .map(this::timeline)
                .toList();
    }

    public RootCauseResponse rootCause(String correlationId) {
        List<TraceLogEvent> events = findEvents(correlationId);
        return rootCauseAnalyzer.analyze(correlationId, events)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No ERROR or FATAL log found for correlationId " + correlationId
                ));
    }

    private List<TraceLogEvent> findEvents(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "correlationId is required");
        }

        String normalizedCorrelationId = correlationId.trim();
        List<TraceLogEvent> events = repository.findByCorrelationId(normalizedCorrelationId);
        if (events.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No logs found for correlationId " + normalizedCorrelationId
            );
        }
        return events;
    }

    private List<TraceLogEvent> filter(List<TraceLogEvent> events, String level, String serviceName, boolean includeStackTrace) {
        Predicate<TraceLogEvent> predicate = ignored -> true;
        if (level != null && !level.isBlank()) {
            String normalizedLevel = LogLevel.from(level).name();
            predicate = predicate.and(event -> normalizedLevel.equalsIgnoreCase(event.level()));
        }
        if (serviceName != null && !serviceName.isBlank()) {
            String normalizedService = serviceName.trim();
            predicate = predicate.and(event -> normalizedService.equals(event.serviceName()));
        }

        return events.stream()
                .filter(predicate)
                .map(event -> includeStackTrace ? event : withoutStackTrace(event))
                .toList();
    }

    private TraceTimelineResponse response(String correlationId, List<TraceLogEvent> events) {
        TraceLogEvent first = events.getFirst();
        TraceLogEvent last = events.getLast();
        LogLevel highest = events.stream()
                .map(event -> LogLevel.from(event.level()))
                .max(java.util.Comparator.comparingInt(LogLevel::severityScore))
                .orElse(LogLevel.INFO);
        LinkedHashSet<String> serviceNames = new LinkedHashSet<>();
        events.stream()
                .map(TraceLogEvent::serviceName)
                .filter(serviceName -> serviceName != null && !serviceName.isBlank())
                .forEach(serviceNames::add);

        return new TraceTimelineResponse(
                correlationId,
                firstNonBlank(events.stream().map(TraceLogEvent::transactionId).toList()),
                firstNonBlank(events.stream().map(TraceLogEvent::userId).toList()),
                status(highest),
                durationMs(first.timestamp(), last.timestamp()),
                highest.name(),
                serviceNames.stream().toList(),
                first.timestamp(),
                last.timestamp(),
                events.size(),
                events
        );
    }

    private TraceLogEvent withoutStackTrace(TraceLogEvent event) {
        return new TraceLogEvent(
                event.eventId(),
                event.timestamp(),
                event.receivedAt(),
                event.serviceName(),
                event.environment(),
                event.level(),
                event.message(),
                event.correlationId(),
                event.traceId(),
                event.spanId(),
                event.parentSpanId(),
                event.userId(),
                event.transactionId(),
                event.module(),
                event.exceptionType(),
                null
        );
    }

    private String firstNonBlank(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private long durationMs(Instant first, Instant last) {
        if (first == null || last == null) {
            return 0;
        }
        return Math.max(0, Duration.between(first, last).toMillis());
    }

    private String status(LogLevel highest) {
        if (highest == LogLevel.ERROR || highest == LogLevel.FATAL) {
            return "FAILED";
        }
        if (highest == LogLevel.WARN) {
            return "WARNING";
        }
        return "SUCCESS";
    }
}
