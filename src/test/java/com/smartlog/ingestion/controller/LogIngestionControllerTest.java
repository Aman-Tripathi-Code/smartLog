package com.smartlog.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static com.smartlog.testsupport.AsyncAssertions.awaitAsserted;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LogIngestionControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanLogs() {
        jdbcTemplate.update("DELETE FROM logs");
    }

    @Test
    void ingestSingleLogStoresStructuredLog() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("eventId", "evt-001");
        request.put("timestamp", "2026-06-16T10:30:05Z");
        request.put("serviceName", "limit-check-service");
        request.put("environment", "dev");
        request.put("level", "ERROR");
        request.put("message", "Customer limit validation failed");
        request.put("correlationId", "corr-12345");
        request.put("traceId", "trace-abc");
        request.put("spanId", "span-limit-001");
        request.put("parentSpanId", "span-trade-001");
        request.put("userId", "U1001");
        request.put("transactionId", "TF-9081");
        request.put("module", "LIMIT_VALIDATION");
        request.put("exceptionType", "LimitExceededException");
        request.put("attributes", Map.of("customerId", "C1001", "limitType", "IMPORT_LC"));

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/logs", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "ACCEPTED");
        assertThat(response.getBody()).containsEntry("eventId", "evt-001");
        awaitLogCount(1);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT service_name, level, message, correlation_id, attributes, received_at, created_at FROM logs WHERE event_id = ?",
                "evt-001"
        );
        assertThat(row).containsEntry("SERVICE_NAME", "limit-check-service");
        assertThat(row).containsEntry("LEVEL", "ERROR");
        assertThat(row).containsEntry("MESSAGE", "Customer limit validation failed");
        assertThat(row).containsEntry("CORRELATION_ID", "corr-12345");
        assertThat(row.get("ATTRIBUTES").toString()).contains("\"customerId\":\"C1001\"");
        assertThat(row.get("RECEIVED_AT")).isNotNull();
        assertThat(row.get("CREATED_AT")).isNotNull();
    }

    @Test
    void ingestBatchStoresAllLogs() {
        Map<String, Object> request = Map.of(
                "logs", List.of(
                        Map.of(
                                "serviceName", "auth-service",
                                "level", "INFO",
                                "message", "User authenticated",
                                "correlationId", "corr-12345"
                        ),
                        Map.of(
                                "serviceName", "limit-check-service",
                                "level", "ERROR",
                                "message", "Limit validation failed",
                                "correlationId", "corr-12345"
                        )
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/logs/batch", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "ACCEPTED");
        assertThat(response.getBody()).containsEntry("accepted", 2);
        assertThat(response.getBody()).containsEntry("rejected", 0);
        assertThat((List<?>) response.getBody().get("eventIds")).hasSize(2);
        awaitLogCount(2);
    }

    @Test
    void missingServiceNameReturnsBadRequest() {
        Map<String, Object> request = Map.of(
                "level", "INFO",
                "message", "User authenticated"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/logs", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "INVALID_REQUEST");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logs", Integer.class)).isZero();
    }

    @Test
    void invalidLogLevelReturnsBadRequest() {
        Map<String, Object> request = Map.of(
                "serviceName", "auth-service",
                "level", "SEVERE",
                "message", "User authenticated"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/logs", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "INVALID_REQUEST");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logs", Integer.class)).isZero();
    }

    @Test
    void missingEventIdIsGenerated() {
        Map<String, Object> request = Map.of(
                "serviceName", "auth-service",
                "level", "INFO",
                "message", "User authenticated"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/logs", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String eventId = response.getBody().get("eventId").toString();
        assertThat(eventId).isNotBlank();
        awaitAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logs WHERE event_id = ?",
                Integer.class,
                eventId
        )).isEqualTo(1));
    }

    @Test
    void attributesJsonIsStored() {
        Map<String, Object> request = Map.of(
                "eventId", "evt-attributes",
                "serviceName", "document-service",
                "level", "WARN",
                "message", "Document verification delayed",
                "attributes", Map.of("documentType", "LC", "attempt", 2)
        );

        restTemplate.postForEntity("/api/v1/logs", new HttpEntity<>(request), Map.class);
        awaitAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logs WHERE event_id = ?",
                Integer.class,
                "evt-attributes"
        )).isEqualTo(1));

        String attributes = jdbcTemplate.queryForObject(
                "SELECT attributes FROM logs WHERE event_id = ?",
                String.class,
                "evt-attributes"
        );
        assertThat(attributes).contains("\"documentType\":\"LC\"");
        assertThat(attributes).contains("\"attempt\":2");
    }

    private void awaitLogCount(int expected) {
        awaitAsserted(() -> assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logs", Integer.class))
                .isEqualTo(expected));
    }
}
