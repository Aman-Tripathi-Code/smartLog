package com.smartlog.analytics.controller;

import static com.smartlog.testsupport.AsyncAssertions.awaitAsserted;
import static org.assertj.core.api.Assertions.assertThat;

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
class AnalyticsControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedErrors() {
        jdbcTemplate.update("DELETE FROM logs");
        ingest("evt-analytics-1", "trade-service", "Limit validation failed", "LimitExceededException");
        ingest("evt-analytics-2", "trade-service", "Limit validation failed", "LimitExceededException");
        ingest("evt-analytics-3", "trade-service", "Database timed out", "TimeoutException");
        ingest("evt-analytics-4", "auth-service", "Token rejected", "AuthenticationException");
        awaitAsserted(() -> assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logs", Integer.class))
                .isEqualTo(4));
    }

    @Test
    void returnsServiceErrorRateForWindow() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/analytics/error-rate?serviceName=trade-service&window=10m",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("serviceName", "trade-service");
        assertThat(response.getBody()).containsEntry("window", "10m");
        assertThat(((Number) response.getBody().get("errorCount")).longValue()).isEqualTo(3L);
        assertThat(((Number) response.getBody().get("errorRatePerMinute")).doubleValue()).isEqualTo(0.3d);
    }

    @Test
    void returnsTopExceptionsForWindow() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/analytics/top-exceptions?window=10m&limit=2",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("window", "10m");
        List<Map<String, Object>> items = items(response.getBody());
        assertThat(items)
                .extracting(item -> item.get("exceptionType"))
                .containsExactly("LimitExceededException", "AuthenticationException");
        assertThat(items)
                .extracting(item -> ((Number) item.get("count")).longValue())
                .containsExactly(2L, 1L);
    }

    private void ingest(String eventId, String serviceName, String message, String exceptionType) {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/logs", Map.of(
                "eventId", eventId,
                "serviceName", serviceName,
                "level", "ERROR",
                "message", message,
                "exceptionType", exceptionType
        ), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> response) {
        return (List<Map<String, Object>>) response.get("items");
    }
}
