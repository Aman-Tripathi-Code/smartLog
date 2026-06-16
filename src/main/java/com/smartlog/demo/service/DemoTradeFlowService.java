package com.smartlog.demo.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smartlog.sdk.client.SmartLogClient;
import com.smartlog.sdk.context.LogContext;
import com.smartlog.sdk.sender.AsyncLogSender;

@Service
public class DemoTradeFlowService {

    private final InProcessSmartLogTransport transport;

    public DemoTradeFlowService(InProcessSmartLogTransport transport) {
        this.transport = transport;
    }

    public Map<String, Object> success() {
        String correlationId = "demo-success-" + UUID.randomUUID();
        String transactionId = "TF-" + System.currentTimeMillis();
        emit(List.of(
                step(correlationId, transactionId, "api-gateway-service", "INFO", "Request received", 0, null),
                step(correlationId, transactionId, "auth-service", "INFO", "User authenticated", 1, null),
                step(correlationId, transactionId, "trade-service", "INFO", "Trade transaction created", 2, null),
                step(correlationId, transactionId, "limit-check-service", "INFO", "Customer limit approved", 3, null),
                step(correlationId, transactionId, "workflow-service", "INFO", "Workflow submitted", 4, null)
        ));
        return Map.of("status", "SUCCESS_DEMO_ACCEPTED", "correlationId", correlationId, "transactionId", transactionId);
    }

    public Map<String, Object> failLimit() {
        String correlationId = "demo-fail-" + UUID.randomUUID();
        String transactionId = "TF-" + System.currentTimeMillis();
        emit(List.of(
                step(correlationId, transactionId, "api-gateway-service", "INFO", "Request received", 0, null),
                step(correlationId, transactionId, "auth-service", "INFO", "User authenticated", 1, null),
                step(correlationId, transactionId, "trade-service", "INFO", "Trade transaction created", 2, null),
                step(correlationId, transactionId, "limit-check-service", "ERROR", "Customer limit validation failed", 3, new LimitExceededException()),
                step(correlationId, transactionId, "workflow-service", "WARN", "Workflow stopped due to validation failure", 4, null)
        ));
        return Map.of("status", "FAILED_DEMO_ACCEPTED", "correlationId", correlationId, "transactionId", transactionId);
    }

    private void emit(List<DemoStep> steps) {
        for (DemoStep step : steps) {
            try (SmartLogClient client = client(step.serviceName(), step.timestamp())) {
                LogContext context = LogContext.builder()
                        .correlationId(step.correlationId())
                        .traceId("trace-" + step.correlationId())
                        .spanId("span-" + step.serviceName())
                        .userId("U-DEMO")
                        .transactionId(step.transactionId())
                        .module("DEMO_TRADE_FLOW")
                        .attributes(Map.of("demo", true))
                        .build();
                switch (step.level()) {
                    case "ERROR" -> client.error(step.message(), context, step.throwable());
                    case "WARN" -> client.warn(step.message(), context);
                    case "DEBUG" -> client.debug(step.message(), context);
                    default -> client.info(step.message(), context);
                }
            }
        }
    }

    private SmartLogClient client(String serviceName, Instant timestamp) {
        return SmartLogClient.builder()
                .serviceName(serviceName)
                .environment("dev")
                .clock(java.time.Clock.fixed(timestamp, java.time.ZoneOffset.UTC))
                .sender(new AsyncLogSender(transport, 10))
                .build();
    }

    private DemoStep step(
            String correlationId,
            String transactionId,
            String serviceName,
            String level,
            String message,
            long offsetSeconds,
            Throwable throwable
    ) {
        return new DemoStep(
                correlationId,
                transactionId,
                serviceName,
                level,
                message,
                Instant.now().plusSeconds(offsetSeconds),
                throwable
        );
    }

    private record DemoStep(
            String correlationId,
            String transactionId,
            String serviceName,
            String level,
            String message,
            Instant timestamp,
            Throwable throwable
    ) {
    }

    private static final class LimitExceededException extends RuntimeException {
        private LimitExceededException() {
            super("Customer limit validation failed");
        }
    }
}
