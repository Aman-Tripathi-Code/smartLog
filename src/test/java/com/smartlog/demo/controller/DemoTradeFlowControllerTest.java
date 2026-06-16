package com.smartlog.demo.controller;

import static com.smartlog.testsupport.AsyncAssertions.awaitAsserted;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoTradeFlowControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM incident_summaries");
        jdbcTemplate.update("DELETE FROM alerts");
        jdbcTemplate.update("DELETE FROM logs");
    }

    @Test
    void failedDemoFlowEmitsTraceThroughSdkPath() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/demo/trade-transactions/fail-limit",
                null,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String correlationId = response.getBody().get("correlationId").toString();
        assertThat(correlationId).startsWith("demo-fail-");
        awaitAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logs WHERE correlation_id = ?",
                Integer.class,
                correlationId
        )).isEqualTo(5));

        ResponseEntity<Map> trace = restTemplate.getForEntity("/api/v1/traces/" + correlationId, Map.class);
        assertThat(trace.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(trace.getBody()).containsEntry("status", "FAILED");
        assertThat(trace.getBody()).containsEntry("totalEvents", 5);
    }
}
