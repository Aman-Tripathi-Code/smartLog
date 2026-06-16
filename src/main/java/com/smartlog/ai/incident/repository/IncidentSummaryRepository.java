package com.smartlog.ai.incident.repository;

import java.util.Optional;
import java.util.UUID;

import com.smartlog.ai.incident.model.IncidentSummaryRecord;

public interface IncidentSummaryRepository {

    IncidentSummaryRecord save(IncidentSummaryRecord summary);

    Optional<IncidentSummaryRecord> findByAlertId(UUID alertId);
}
