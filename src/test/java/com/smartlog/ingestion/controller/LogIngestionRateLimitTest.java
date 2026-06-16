package com.smartlog.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "smartlog.ingestion.rate-limit.enabled=true",
                "smartlog.ingestion.rate-limit.default-limit-per-window=1",
                "smartlog.ingestion.rate-limit.window=1m"
        }
)
class LogIngestionRateLimitTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanLogs() {
        jdbcTemplate.update("DELETE FROM logs");
    }

    @Test
    void returnsTooManyRequestsWhenServiceExceedsRateLimitWindow() {
        ResponseEntity<Map> first = restTemplate.postForEntity("/api/v1/logs", log("evt-rate-1"), Map.class);
        ResponseEntity<Map> second = restTemplate.postForEntity("/api/v1/logs", log("evt-rate-2"), Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(second.getBody()).containsEntry("error", "RATE_LIMIT_EXCEEDED");
    }

    private Map<String, Object> log(String eventId) {
        return Map.of(
                "eventId", eventId,
                "serviceName", "rate-limited-service",
                "level", "INFO",
                "message", "Accepted until service rate limit is reached"
        );
    }
}
