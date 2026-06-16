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
import com.smartlog.ingestion.validation.LogValidator;

@Service
public class LogIngestionService {

    private static final String ACCEPTED = "ACCEPTED";

    private final LogValidator validator;
    private final LogEventMapper mapper;
    private final LogEventPublisher publisher;

    public LogIngestionService(LogValidator validator, LogEventMapper mapper, LogEventPublisher publisher) {
        this.validator = validator;
        this.mapper = mapper;
        this.publisher = publisher;
    }

    public LogIngestionResponse ingest(LogIngestionRequest request) {
        validator.validate(request);
        LogEvent event = mapper.toLogEvent(request);
        publisher.publish(event);
        return new LogIngestionResponse(ACCEPTED, event.eventId(), event.receivedAt());
    }

    public BatchLogIngestionResponse ingestBatch(BatchLogIngestionRequest request) {
        List<LogEvent> events = request.logs().stream()
                .peek(validator::validate)
                .map(mapper::toLogEvent)
                .toList();

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
