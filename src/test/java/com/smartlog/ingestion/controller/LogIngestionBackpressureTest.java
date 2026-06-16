package com.smartlog.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.pipeline.LogEventPublisher;
import com.smartlog.ingestion.pipeline.LogQueueFullException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LogIngestionBackpressureTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsTooManyRequestsWhenIngestionQueueIsFull() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/logs",
                Map.of(
                        "serviceName", "limit-check-service",
                        "level", "ERROR",
                        "message", "Customer limit validation failed"
                ),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsEntry("error", "INGESTION_QUEUE_FULL");
    }

    @TestConfiguration
    static class BackpressureTestConfiguration {

        @Bean
        @Primary
        LogEventPublisher fullQueuePublisher() {
            return new LogEventPublisher() {
                @Override
                public void publish(LogEvent event) {
                    throw new LogQueueFullException("Log ingestion queue is full");
                }

                @Override
                public void publishAll(List<LogEvent> events) {
                    throw new LogQueueFullException("Log ingestion queue is full");
                }
            };
        }
    }
}
