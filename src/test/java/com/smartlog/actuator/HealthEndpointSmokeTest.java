package com.smartlog.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void actuatorHealthReturnsUp() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void prometheusEndpointExposesSmartLogMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("smartlog_logs_accepted_total");
        assertThat(response.getBody()).contains("smartlog_logs_rejected_total");
        assertThat(response.getBody()).contains("smartlog_ingestion_queue_size");
        assertThat(response.getBody()).contains("smartlog_alerts_generated_total");
    }
}
