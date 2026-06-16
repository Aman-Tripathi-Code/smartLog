package com.smartlog.trace.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static com.smartlog.testsupport.AsyncAssertions.awaitAsserted;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TraceControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedTrace() {
        jdbcTemplate.update("DELETE FROM incident_summaries");
        jdbcTemplate.update("DELETE FROM alerts");
        jdbcTemplate.update("DELETE FROM logs");

        ingest(log("evt-workflow", "2026-06-16T10:30:06Z", "workflow-service", "WARN",
                "Workflow stopped due to validation failure", null));
        ingest(log("evt-auth", "2026-06-16T10:30:01Z", "auth-service", "INFO",
                "User authenticated", null));
        ingest(log("evt-limit", "2026-06-16T10:30:05Z", "limit-check-service", "ERROR",
                "Customer limit validation failed", "LimitExceededException"));
        ingest(log("evt-trade", "2026-06-16T10:30:03Z", "trade-service", "INFO",
                "Trade transaction created", null));
        awaitAsserted(() -> assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logs", Integer.class))
                .isEqualTo(4));
    }

    @Test
    void returnsTraceTimelineSortedByTimestampAscending() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/traces/corr-12345", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("correlationId", "corr-12345");
        assertThat(body).containsEntry("transactionId", "TF-9081");
        assertThat(body).containsEntry("userId", "U1001");
        assertThat(body).containsEntry("status", "FAILED");
        assertThat(body).containsEntry("highestSeverity", "ERROR");
        assertThat(body).containsEntry("totalEvents", 4);
        assertThat(body.get("durationMs")).isEqualTo(5000);
        assertThat(list(body, "services"))
                .containsExactly("auth-service", "trade-service", "limit-check-service", "workflow-service");
        assertThat(eventIds(body)).containsExactly("evt-auth", "evt-trade", "evt-limit", "evt-workflow");
        assertThat(events(body).get(2)).containsEntry("stackTrace", null);
    }

    @Test
    void filtersTraceTimelineAndCanIncludeStackTrace() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/traces/corr-12345?level=ERROR&serviceName=limit-check-service&includeStackTrace=true",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("totalEvents", 1);
        assertThat(eventIds(body)).containsExactly("evt-limit");
        assertThat(events(body).getFirst().get("stackTrace").toString())
                .contains("token=***MASKED***")
                .doesNotContain("token=secret-token");
    }

    @Test
    void returnsTraceTimelineByTransactionId() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/traces/by-transaction/TF-9081",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("correlationId", "corr-12345");
        assertThat(eventIds(response.getBody())).containsExactly("evt-auth", "evt-trade", "evt-limit", "evt-workflow");
    }

    @Test
    void returnsRecentTraceTimelinesByUser() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/v1/traces/by-user/U1001?limit=5",
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat((Map<String, Object>) response.getBody().getFirst()).containsEntry("correlationId", "corr-12345");
    }

    @Test
    void returnsPrioritizedRootCauseWithSupportingContext() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/traces/corr-12345/root-cause",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("correlationId", "corr-12345");
        assertThat(body).containsEntry("serviceName", "limit-check-service");
        assertThat(body).containsEntry("message", "Customer limit validation failed");
        assertThat(body).containsEntry("exceptionType", "LimitExceededException");
        assertThat(body).containsEntry("transactionId", "TF-9081");
        assertThat(body).containsEntry("userId", "U1001");
        assertThat(body).containsEntry("confidence", "HIGH");
        assertThat(body.get("reason").toString()).contains("ERROR").contains("exception type");
        assertThat(body).containsEntry("timestamp", "2026-06-16T10:30:05Z");
        assertThat((List<?>) body.get("supportingEvents")).hasSize(2);
    }

    @Test
    void createsIncidentSummaryUsingMockLlmClient() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/traces/corr-12345/incident-summary",
                null,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("correlationId", "corr-12345");
        assertThat(body.get("incidentSummaryId")).isNotNull();
        assertThat(body.get("summary").toString())
                .contains("Trace corr-12345 failed at limit-check-service");
        assertThat(body.get("probableCause").toString())
                .contains("LimitExceededException")
                .contains("Customer limit validation failed");
        assertThat(list(body, "impactedServices"))
                .containsExactly("auth-service", "trade-service", "limit-check-service", "workflow-service");
        assertThat(list(body, "suggestedActions"))
                .contains("Inspect recent logs and deployment changes for limit-check-service.",
                        "Replay or inspect transaction TF-9081 in a lower environment.");
        assertThat(body).containsEntry("confidence", "MOCK_RULE_BASED");
        assertThat(body).containsEntry("summarizerType", "MOCK_LLM");
        awaitAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM incident_summaries WHERE correlation_id = ?",
                Integer.class,
                "corr-12345"
        )).isEqualTo(1));
    }

    @Test
    void createsIncidentSummaryForAlert() {
        UUID alertId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO alerts (
                    id, alert_type, severity, service_name, message, window_start, window_end,
                    event_count, status, sample_correlation_id, sample_transaction_id, created_at, updated_at
                )
                VALUES (
                    ?, 'ERROR_THRESHOLD', 'HIGH', 'limit-check-service', 'Error threshold exceeded',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3, 'OPEN', 'corr-12345', 'TF-9081',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """, alertId);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/incidents/" + alertId + "/summary",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("correlationId", "corr-12345");
        assertThat(response.getBody().get("summary").toString())
                .contains("Trace corr-12345 failed at limit-check-service");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM incident_summaries WHERE alert_id = ?",
                Integer.class,
                alertId
        )).isEqualTo(1);
    }

    private void ingest(Map<String, Object> request) {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/logs", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private Map<String, Object> log(
            String eventId,
            String timestamp,
            String serviceName,
            String level,
            String message,
            String exceptionType
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("eventId", eventId);
        request.put("timestamp", timestamp);
        request.put("serviceName", serviceName);
        request.put("environment", "dev");
        request.put("level", level);
        request.put("message", message);
        request.put("correlationId", "corr-12345");
        request.put("traceId", "trace-abc");
        request.put("spanId", "span-" + serviceName);
        request.put("userId", "U1001");
        request.put("transactionId", "TF-9081");
        if (exceptionType != null) {
            request.put("exceptionType", exceptionType);
            request.put("stackTrace", exceptionType + ": request failed token=secret-token");
        }
        return request;
    }

    private List<String> eventIds(Map<String, Object> response) {
        return events(response).stream()
                .map(item -> item.get("eventId").toString())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> events(Map<String, Object> response) {
        return (List<Map<String, Object>>) response.get("events");
    }

    @SuppressWarnings("unchecked")
    private List<String> list(Map<String, Object> response, String fieldName) {
        return (List<String>) response.get(fieldName);
    }
}
