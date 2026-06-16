package com.smartlog.alerting.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlog.alerting.dto.AlertListResponse;
import com.smartlog.alerting.dto.AlertResponse;
import com.smartlog.alerting.service.AlertQueryService;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertQueryService alertQueryService;

    public AlertController(AlertQueryService alertQueryService) {
        this.alertQueryService = alertQueryService;
    }

    @GetMapping
    public AlertListResponse alerts() {
        return alertQueryService.findAll();
    }

    @GetMapping("/{alertId}")
    public AlertResponse alert(@PathVariable UUID alertId) {
        return alertQueryService.findById(alertId);
    }
}
