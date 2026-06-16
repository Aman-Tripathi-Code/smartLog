package com.smartlog.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.smartlog.ingestion.dto.LogIngestionRequest;
import com.smartlog.ingestion.service.LogIngestionService;
import com.smartlog.sdk.sender.LogTransport;

@Component
class InProcessSmartLogTransport implements LogTransport {

    private final ObjectMapper objectMapper;
    private final LogIngestionService ingestionService;

    InProcessSmartLogTransport(ObjectMapper objectMapper, LogIngestionService ingestionService) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
    }

    @Override
    public boolean post(String jsonPayload) throws Exception {
        ingestionService.ingest(objectMapper.readValue(jsonPayload, LogIngestionRequest.class));
        return true;
    }
}
