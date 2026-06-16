package com.smartlog.ingestion.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.pipeline.LogPipelineMetrics;

class KafkaLogEventPublisherTest {

    @Test
    void publishesAcceptedLogEventToRawTopicWithCorrelationKey() throws Exception {
        RecordingKafkaPublisher publisher = new RecordingKafkaPublisher();
        KafkaTopicsProperties properties = properties();
        LogPipelineMetrics metrics = new LogPipelineMetrics();
        KafkaLogEventPublisher logEventPublisher = new KafkaLogEventPublisher(
                publisher,
                properties,
                objectMapper(),
                metrics
        );

        logEventPublisher.publish(event("evt-raw"));

        assertThat(publisher.messages).hasSize(1);
        SentMessage message = publisher.messages.getFirst();
        assertThat(message.topic()).isEqualTo("logs.raw");
        assertThat(message.key()).isEqualTo("corr-12345");
        assertThat(message.payload()).contains("\"eventId\":\"evt-raw\"");
        assertThat(metrics.accepted()).isEqualTo(1);
    }

    private KafkaTopicsProperties properties() {
        KafkaTopicsProperties properties = new KafkaTopicsProperties();
        properties.topics().setRaw("logs.raw");
        properties.topics().setEnriched("logs.enriched");
        properties.topics().setDeadLetter("logs.dead-letter");
        return properties;
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private LogEvent event(String eventId) {
        Instant timestamp = Instant.parse("2026-06-16T10:30:05Z");
        return new LogEvent(
                eventId,
                timestamp,
                timestamp.plusMillis(10),
                "limit-check-service",
                "dev",
                LogLevel.ERROR,
                "Customer limit validation failed",
                "corr-12345",
                "trace-abc",
                "span-limit-001",
                "span-trade-001",
                "U1001",
                "TF-9081",
                "LIMIT_VALIDATION",
                "LimitExceededException",
                null,
                Map.of("customerId", "C1001")
        );
    }

    private static final class RecordingKafkaPublisher implements KafkaMessagePublisher {

        private final List<SentMessage> messages = new ArrayList<>();

        @Override
        public void send(String topic, String key, String payload) {
            messages.add(new SentMessage(topic, key, payload));
        }
    }

    private record SentMessage(String topic, String key, String payload) {
    }
}
