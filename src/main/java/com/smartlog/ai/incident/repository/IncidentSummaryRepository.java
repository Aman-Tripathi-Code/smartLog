package com.smartlog.ai.incident.repository;

import com.smartlog.ai.incident.model.IncidentSummaryRecord;

public interface IncidentSummaryRepository {

    IncidentSummaryRecord save(IncidentSummaryRecord summary);
}
