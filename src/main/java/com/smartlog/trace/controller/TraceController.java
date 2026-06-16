package com.smartlog.trace.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlog.trace.dto.RootCauseResponse;
import com.smartlog.trace.dto.TraceTimelineResponse;
import com.smartlog.trace.service.TraceTimelineService;

@RestController
@RequestMapping("/api/v1/traces")
public class TraceController {

    private final TraceTimelineService traceTimelineService;

    public TraceController(TraceTimelineService traceTimelineService) {
        this.traceTimelineService = traceTimelineService;
    }

    @GetMapping("/{correlationId}")
    public TraceTimelineResponse timeline(@PathVariable String correlationId) {
        return traceTimelineService.timeline(correlationId);
    }

    @GetMapping("/{correlationId}/root-cause")
    public RootCauseResponse rootCause(@PathVariable String correlationId) {
        return traceTimelineService.rootCause(correlationId);
    }
}
