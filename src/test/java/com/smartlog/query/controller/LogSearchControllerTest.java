package com.smartlog.query.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LogSearchControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedLogs() {
        jdbcTemplate.update("DELETE FROM logs");
        ingest(log("evt-auth", "2026-06-16T10:30:01Z", "auth-service", "INFO",
                "User authenticated", "corr-123", "trace-abc", "U1001", "TF-1001"));
        ingest(log("evt-limit", "2026-06-16T10:30:05Z", "limit-check-service", "ERROR",
                "Customer limit validation failed", "corr-123", "trace-abc", "U1001", "TF-1001"));
        ingest(log("evt-doc", "2026-06-16T10:31:00Z", "document-service", "WARN",
                "Document verification delayed", "corr-999", "trace-doc", "U2002", "TF-2002"));
        awaitAsserted(() -> assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logs", Integer.class))
                .isEqualTo(3));
    }

    @Test
    void searchesByServiceName() {
        Map<String, Object> response = search(Map.of("serviceName", "limit-check-service"));

        assertThat(total(response)).isEqualTo(1);
        assertThat(firstItem(response)).containsEntry("eventId", "evt-limit");
    }

    @Test
    void searchesByLevel() {
        Map<String, Object> response = search(Map.of("level", "WARN"));

        assertThat(total(response)).isEqualTo(1);
        assertThat(firstItem(response)).containsEntry("eventId", "evt-doc");
    }

    @Test
    void searchesByTimeRange() {
        Map<String, Object> response = search(Map.of(
                "from", "2026-06-16T10:30:00Z",
                "to", "2026-06-16T10:30:30Z"
        ));

        assertThat(total(response)).isEqualTo(2);
        assertThat(eventIds(response)).containsExactly("evt-limit", "evt-auth");
    }

    @Test
    void searchesByKeywordCaseInsensitively() {
        Map<String, Object> response = search(Map.of("keyword", "VALIDATION"));

        assertThat(total(response)).isEqualTo(1);
        assertThat(firstItem(response)).containsEntry("eventId", "evt-limit");
    }

    @Test
    void searchesByCorrelationIdAndTraceId() {
        Map<String, Object> response = search(Map.of(
                "correlationId", "corr-123",
                "traceId", "trace-abc"
        ));

        assertThat(total(response)).isEqualTo(2);
        assertThat(eventIds(response)).containsExactly("evt-limit", "evt-auth");
    }

    @Test
    void searchesByUserIdAndTransactionId() {
        Map<String, Object> response = search(Map.of(
                "userId", "U2002",
                "transactionId", "TF-2002"
        ));

        assertThat(total(response)).isEqualTo(1);
        assertThat(firstItem(response)).containsEntry("eventId", "evt-doc");
    }

    @Test
    void paginatesResults() {
        Map<String, Object> response = search(Map.of("page", "1", "size", "1"));

        assertThat(total(response)).isEqualTo(3);
        assertThat(response).containsEntry("page", 1);
        assertThat(response).containsEntry("size", 1);
        assertThat(eventIds(response)).containsExactly("evt-limit");
    }

    @Test
    void sortsByTimestampDescendingByDefault() {
        Map<String, Object> response = search(Map.of());

        assertThat(eventIds(response)).containsExactly("evt-doc", "evt-limit", "evt-auth");
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
            String correlationId,
            String traceId,
            String userId,
            String transactionId
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("eventId", eventId);
        request.put("timestamp", timestamp);
        request.put("serviceName", serviceName);
        request.put("level", level);
        request.put("message", message);
        request.put("correlationId", correlationId);
        request.put("traceId", traceId);
        request.put("userId", userId);
        request.put("transactionId", transactionId);
        return request;
    }

    private Map<String, Object> search(Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/v1/logs/search");
        queryParams.forEach(builder::queryParam);
        String uri = builder.build().encode().toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private long total(Map<String, Object> response) {
        return ((Number) response.get("total")).longValue();
    }

    private Map<String, Object> firstItem(Map<String, Object> response) {
        return items(response).getFirst();
    }

    private List<String> eventIds(Map<String, Object> response) {
        return items(response).stream()
                .map(item -> item.get("eventId").toString())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> response) {
        return (List<Map<String, Object>>) response.get("items");
    }
}
