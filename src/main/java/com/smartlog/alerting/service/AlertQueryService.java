package com.smartlog.alerting.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartlog.alerting.dto.AlertListResponse;
import com.smartlog.alerting.dto.AlertResponse;
import com.smartlog.alerting.repository.AlertRepository;

@Service
public class AlertQueryService {

    private final AlertRepository repository;

    public AlertQueryService(AlertRepository repository) {
        this.repository = repository;
    }

    public AlertListResponse findAll() {
        return new AlertListResponse(repository.findAll().stream()
                .map(AlertResponse::from)
                .toList());
    }

    public AlertResponse findById(UUID alertId) {
        return repository.findById(alertId)
                .map(AlertResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "alert not found"));
    }
}
