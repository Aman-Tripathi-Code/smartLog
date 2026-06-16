package com.smartlog.ai.incident.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlog.ai.incident.dto.IncidentSummaryResponse;
import com.smartlog.ai.incident.service.IncidentSummaryService;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentSummaryService incidentSummaryService;

    public IncidentController(IncidentSummaryService incidentSummaryService) {
        this.incidentSummaryService = incidentSummaryService;
    }

    @GetMapping("/{alertId}/summary")
    public IncidentSummaryResponse summary(@PathVariable UUID alertId) {
        return incidentSummaryService.summarizeAlertIncident(alertId);
    }
}
