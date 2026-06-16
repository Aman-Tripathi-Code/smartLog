package com.smartlog.ingestion.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.dto.BatchLogIngestionRequest;
import com.smartlog.ingestion.dto.BatchLogIngestionResponse;
import com.smartlog.ingestion.dto.LogIngestionRequest;
import com.smartlog.ingestion.dto.LogIngestionResponse;
import com.smartlog.ingestion.pipeline.LogEventPublisher;
import com.smartlog.ingestion.ratelimit.InMemoryRateLimiter;
import com.smartlog.ingestion.validation.LogValidator;

@Service
public class LogIngestionService {

    private static final String ACCEPTED = "ACCEPTED";

    private final LogValidator validator;
    private final LogEventMapper mapper;
    private final LogEventPublisher publisher;
    private final InMemoryRateLimiter rateLimiter;

    public LogIngestionService(
            LogValidator validator,
            LogEventMapper mapper,
            LogEventPublisher publisher,
            InMemoryRateLimiter rateLimiter
    ) {
        this.validator = validator;
        this.mapper = mapper;
        this.publisher = publisher;
        this.rateLimiter = rateLimiter;
    }

    public LogIngestionResponse ingest(LogIngestionRequest request) {
        validator.validate(request);
        rateLimiter.check(request.serviceName(), 1);
        LogEvent event = mapper.toLogEvent(request);
        publisher.publish(event);
        return new LogIngestionResponse(ACCEPTED, event.eventId(), event.receivedAt());
    }

    public BatchLogIngestionResponse ingestBatch(BatchLogIngestionRequest request) {
        List<LogEvent> events = request.logs().stream()
                .peek(validator::validate)
                .map(mapper::toLogEvent)
                .toList();
        events.stream()
                .collect(java.util.stream.Collectors.groupingBy(LogEvent::serviceName, java.util.stream.Collectors.counting()))
                .forEach((serviceName, count) -> rateLimiter.check(serviceName, Math.toIntExact(count)));

        publisher.publishAll(events);
        Instant acceptedAt = events.stream()
                .map(LogEvent::receivedAt)
                .max(Instant::compareTo)
                .orElseGet(Instant::now);

        return new BatchLogIngestionResponse(
                ACCEPTED,
                events.size(),
                0,
                events.stream().map(LogEvent::eventId).toList(),
                acceptedAt
        );
    }
}
