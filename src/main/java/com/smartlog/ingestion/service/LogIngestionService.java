package com.smartlog.ingestion.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.dto.BatchLogIngestionRequest;
import com.smartlog.ingestion.dto.BatchLogIngestionResponse;
import com.smartlog.ingestion.dto.LogIngestionRequest;
import com.smartlog.ingestion.dto.LogIngestionResponse;
import com.smartlog.ingestion.validation.LogValidator;
import com.smartlog.storage.repository.LogRepository;

@Service
public class LogIngestionService {

    private static final String ACCEPTED = "ACCEPTED";

    private final LogValidator validator;
    private final LogEventMapper mapper;
    private final LogRepository repository;

    public LogIngestionService(LogValidator validator, LogEventMapper mapper, LogRepository repository) {
        this.validator = validator;
        this.mapper = mapper;
        this.repository = repository;
    }

    public LogIngestionResponse ingest(LogIngestionRequest request) {
        validator.validate(request);
        LogEvent event = mapper.toLogEvent(request);
        repository.save(event);
        return new LogIngestionResponse(ACCEPTED, event.eventId(), event.receivedAt());
    }

    public BatchLogIngestionResponse ingestBatch(BatchLogIngestionRequest request) {
        List<LogEvent> events = request.logs().stream()
                .peek(validator::validate)
                .map(mapper::toLogEvent)
                .toList();

        repository.saveAll(events);
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
