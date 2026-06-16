package com.smartlog.trace.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.smartlog.trace.dto.RootCauseResponse;
import com.smartlog.trace.dto.TraceLogEvent;

class BasicRootCauseAnalyzerTest {

    private final BasicRootCauseAnalyzer analyzer = new BasicRootCauseAnalyzer();

    @Test
    void selectsFirstErrorOrFatalEventInTimelineOrder() {
        List<TraceLogEvent> events = List.of(
                event("auth-service", "INFO", "User authenticated", null, "2026-06-16T10:30:01Z"),
                event("limit-check-service", "ERROR", "Customer limit validation failed",
                        "LimitExceededException", "2026-06-16T10:30:05Z"),
                event("workflow-service", "FATAL", "Workflow crashed",
                        "WorkflowException", "2026-06-16T10:30:06Z")
        );

        Optional<RootCauseResponse> rootCause = analyzer.analyze("corr-12345", events);

        assertThat(rootCause).isPresent();
        assertThat(rootCause.get().serviceName()).isEqualTo("limit-check-service");
        assertThat(rootCause.get().message()).isEqualTo("Customer limit validation failed");
        assertThat(rootCause.get().exceptionType()).isEqualTo("LimitExceededException");
        assertThat(rootCause.get().timestamp()).isEqualTo(Instant.parse("2026-06-16T10:30:05Z"));
        assertThat(rootCause.get().transactionId()).isEqualTo("TF-9081");
        assertThat(rootCause.get().userId()).isEqualTo("U1001");
        assertThat(rootCause.get().confidence()).isEqualTo(BasicRootCauseAnalyzer.CONFIDENCE);
    }

    @Test
    void returnsEmptyWhenTraceHasNoErrorOrFatalEvent() {
        List<TraceLogEvent> events = List.of(
                event("auth-service", "INFO", "User authenticated", null, "2026-06-16T10:30:01Z"),
                event("workflow-service", "WARN", "Workflow waiting for callback", null, "2026-06-16T10:30:06Z")
        );

        Optional<RootCauseResponse> rootCause = analyzer.analyze("corr-12345", events);

        assertThat(rootCause).isEmpty();
    }

    private TraceLogEvent event(
            String serviceName,
            String level,
            String message,
            String exceptionType,
            String timestamp
    ) {
        return new TraceLogEvent(
                "evt-" + serviceName + "-" + level,
                Instant.parse(timestamp),
                Instant.parse(timestamp).plusMillis(100),
                serviceName,
                "dev",
                level,
                message,
                "corr-12345",
                "trace-abc",
                "span-" + serviceName,
                null,
                "U1001",
                "TF-9081",
                "TRACE_TEST",
                exceptionType
        );
    }
}
