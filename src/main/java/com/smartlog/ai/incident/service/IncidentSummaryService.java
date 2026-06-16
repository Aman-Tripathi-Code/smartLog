package com.smartlog.ai.incident.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smartlog.ai.incident.context.IncidentContext;
import com.smartlog.ai.incident.dto.IncidentSummaryDraft;
import com.smartlog.ai.incident.dto.IncidentSummaryResponse;
import com.smartlog.ai.incident.llm.LlmClient;
import com.smartlog.ai.incident.model.IncidentSummaryRecord;
import com.smartlog.ai.incident.repository.IncidentSummaryRepository;
import com.smartlog.trace.dto.RootCauseResponse;
import com.smartlog.trace.dto.TraceLogEvent;
import com.smartlog.trace.dto.TraceTimelineResponse;
import com.smartlog.trace.service.TraceTimelineService;

@Service
public class IncidentSummaryService {

    private final TraceTimelineService traceTimelineService;
    private final LlmClient llmClient;
    private final IncidentSummaryRepository repository;

    public IncidentSummaryService(
            TraceTimelineService traceTimelineService,
            LlmClient llmClient,
            IncidentSummaryRepository repository
    ) {
        this.traceTimelineService = traceTimelineService;
        this.llmClient = llmClient;
        this.repository = repository;
    }

    public IncidentSummaryResponse summarizeTraceIncident(String correlationId) {
        TraceTimelineResponse timeline = traceTimelineService.timeline(correlationId);
        RootCauseResponse rootCause = traceTimelineService.rootCause(correlationId);
        IncidentContext context = context(timeline, rootCause);
        IncidentSummaryDraft draft = llmClient.summarizeIncident(context);

        IncidentSummaryRecord saved = repository.save(new IncidentSummaryRecord(
                UUID.randomUUID(),
                null,
                timeline.correlationId(),
                draft.summary(),
                draft.probableCause(),
                draft.impactedServices(),
                draft.suggestedActions(),
                draft.confidence(),
                draft.summarizerType(),
                Instant.now()
        ));

        return IncidentSummaryResponse.from(saved);
    }

    private IncidentContext context(TraceTimelineResponse timeline, RootCauseResponse rootCause) {
        LinkedHashSet<String> serviceNames = timeline.events().stream()
                .map(TraceLogEvent::serviceName)
                .filter(serviceName -> serviceName != null && !serviceName.isBlank())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        List<String> impactedServices = serviceNames.stream().toList();

        return new IncidentContext(
                timeline.correlationId(),
                rootCause.transactionId(),
                rootCause.userId(),
                impactedServices,
                rootCause,
                timeline.events()
        );
    }
}
