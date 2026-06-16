package com.smartlog.trace.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        jdbcTemplate.update("DELETE FROM logs");

        ingest(log("evt-workflow", "2026-06-16T10:30:06Z", "workflow-service", "WARN",
                "Workflow stopped due to validation failure", null));
        ingest(log("evt-auth", "2026-06-16T10:30:01Z", "auth-service", "INFO",
                "User authenticated", null));
        ingest(log("evt-limit", "2026-06-16T10:30:05Z", "limit-check-service", "ERROR",
                "Customer limit validation failed", "LimitExceededException"));
        ingest(log("evt-trade", "2026-06-16T10:30:03Z", "trade-service", "INFO",
                "Trade transaction created", null));
    }

    @Test
    void returnsTraceTimelineSortedByTimestampAscending() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/traces/corr-12345", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("correlationId", "corr-12345");
        assertThat(body).containsEntry("totalEvents", 4);
        assertThat(eventIds(body)).containsExactly("evt-auth", "evt-trade", "evt-limit", "evt-workflow");
    }

    @Test
    void returnsBasicRootCauseForFirstErrorOrFatalEvent() {
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
        assertThat(body).containsEntry("confidence", "BASIC_RULE_BASED");
        assertThat(body).containsEntry("timestamp", "2026-06-16T10:30:05Z");
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
}
