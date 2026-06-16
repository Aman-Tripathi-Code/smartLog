package com.smartlog.ingestion.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlog.ingestion.dto.BatchLogIngestionRequest;
import com.smartlog.ingestion.dto.BatchLogIngestionResponse;
import com.smartlog.ingestion.dto.LogIngestionRequest;
import com.smartlog.ingestion.dto.LogIngestionResponse;
import com.smartlog.ingestion.service.LogIngestionService;

@RestController
@RequestMapping("/api/v1/logs")
public class LogIngestionController {

    private final LogIngestionService ingestionService;

    public LogIngestionController(LogIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<LogIngestionResponse> ingest(@Valid @RequestBody LogIngestionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ingestionService.ingest(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchLogIngestionResponse> ingestBatch(@Valid @RequestBody BatchLogIngestionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ingestionService.ingestBatch(request));
    }
}
