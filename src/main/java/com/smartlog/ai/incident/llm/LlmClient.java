package com.smartlog.ai.incident.llm;

import com.smartlog.ai.incident.context.IncidentContext;
import com.smartlog.ai.incident.dto.IncidentSummaryDraft;

public interface LlmClient {

    IncidentSummaryDraft summarizeIncident(IncidentContext context);
}
