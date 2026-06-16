package com.smartlog.trace.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        List<TraceLogEvent> events = findEvents(correlationId);
        return new TraceTimelineResponse(correlationId, events.size(), events);
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
}
