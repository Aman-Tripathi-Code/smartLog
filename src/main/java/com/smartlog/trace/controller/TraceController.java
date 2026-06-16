package com.smartlog.trace.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlog.ai.incident.dto.IncidentSummaryResponse;
import com.smartlog.ai.incident.service.IncidentSummaryService;
import com.smartlog.trace.dto.RootCauseResponse;
import com.smartlog.trace.dto.TraceTimelineResponse;
import com.smartlog.trace.service.TraceTimelineService;

@RestController
@RequestMapping("/api/v1/traces")
public class TraceController {

    private final TraceTimelineService traceTimelineService;
    private final IncidentSummaryService incidentSummaryService;

    public TraceController(TraceTimelineService traceTimelineService, IncidentSummaryService incidentSummaryService) {
        this.traceTimelineService = traceTimelineService;
        this.incidentSummaryService = incidentSummaryService;
    }

    @GetMapping("/{correlationId}")
    public TraceTimelineResponse timeline(
            @PathVariable String correlationId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "false") boolean includeStackTrace
    ) {
        return traceTimelineService.timeline(correlationId, level, serviceName, includeStackTrace);
    }

    @GetMapping("/by-transaction/{transactionId}")
    public TraceTimelineResponse byTransaction(@PathVariable String transactionId) {
        return traceTimelineService.timelineByTransaction(transactionId);
    }

    @GetMapping("/by-user/{userId}")
    public List<TraceTimelineResponse> byUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return traceTimelineService.timelinesByUser(userId, limit);
    }

    @GetMapping("/{correlationId}/root-cause")
    public RootCauseResponse rootCause(@PathVariable String correlationId) {
        return traceTimelineService.rootCause(correlationId);
    }

    @PostMapping("/{correlationId}/incident-summary")
    public IncidentSummaryResponse incidentSummary(@PathVariable String correlationId) {
        return incidentSummaryService.summarizeTraceIncident(correlationId);
    }
}
