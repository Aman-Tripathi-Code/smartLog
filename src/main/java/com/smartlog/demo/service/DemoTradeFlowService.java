package com.smartlog.demo.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smartlog.ingestion.dto.BatchLogIngestionRequest;
import com.smartlog.ingestion.dto.LogIngestionRequest;
import com.smartlog.ingestion.service.LogIngestionService;

@Service
public class DemoTradeFlowService {

    private final LogIngestionService ingestionService;

    public DemoTradeFlowService(LogIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public Map<String, Object> success() {
        String correlationId = "demo-success-" + UUID.randomUUID();
        String transactionId = "TF-" + System.currentTimeMillis();
        ingest(List.of(
                log(correlationId, transactionId, "api-gateway-service", "INFO", "Request received", 0, null),
                log(correlationId, transactionId, "auth-service", "INFO", "User authenticated", 1, null),
                log(correlationId, transactionId, "trade-service", "INFO", "Trade transaction created", 2, null),
                log(correlationId, transactionId, "limit-check-service", "INFO", "Customer limit approved", 3, null),
                log(correlationId, transactionId, "workflow-service", "INFO", "Workflow submitted", 4, null)
        ));
        return Map.of("status", "SUCCESS_DEMO_ACCEPTED", "correlationId", correlationId, "transactionId", transactionId);
    }

    public Map<String, Object> failLimit() {
        String correlationId = "demo-fail-" + UUID.randomUUID();
        String transactionId = "TF-" + System.currentTimeMillis();
        ingest(List.of(
                log(correlationId, transactionId, "api-gateway-service", "INFO", "Request received", 0, null),
                log(correlationId, transactionId, "auth-service", "INFO", "User authenticated", 1, null),
                log(correlationId, transactionId, "trade-service", "INFO", "Trade transaction created", 2, null),
                log(correlationId, transactionId, "limit-check-service", "ERROR", "Customer limit validation failed", 3, "LimitExceededException"),
                log(correlationId, transactionId, "workflow-service", "WARN", "Workflow stopped due to validation failure", 4, null)
        ));
        return Map.of("status", "FAILED_DEMO_ACCEPTED", "correlationId", correlationId, "transactionId", transactionId);
    }

    private void ingest(List<LogIngestionRequest> logs) {
        ingestionService.ingestBatch(new BatchLogIngestionRequest(logs));
    }

    private LogIngestionRequest log(
            String correlationId,
            String transactionId,
            String serviceName,
            String level,
            String message,
            long offsetSeconds,
            String exceptionType
    ) {
        return new LogIngestionRequest(
                "evt-" + serviceName + "-" + UUID.randomUUID(),
                Instant.now().plusSeconds(offsetSeconds),
                serviceName,
                "dev",
                level,
                message,
                correlationId,
                "trace-" + correlationId,
                "span-" + serviceName,
                null,
                "U-DEMO",
                transactionId,
                "DEMO_TRADE_FLOW",
                exceptionType,
                null,
                Map.of("demo", true)
        );
    }
}
