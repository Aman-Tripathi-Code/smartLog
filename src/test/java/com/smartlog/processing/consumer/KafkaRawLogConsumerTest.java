package com.smartlog.processing.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import com.smartlog.alerting.engine.AlertEngine;
import com.smartlog.alerting.engine.AlertingProperties;
import com.smartlog.alerting.model.AlertRecord;
import com.smartlog.alerting.repository.AlertRepository;
import com.smartlog.analytics.topk.TopErrorEvent;
import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.kafka.KafkaMessagePublisher;
import com.smartlog.ingestion.kafka.KafkaTopicsProperties;
import com.smartlog.ingestion.pipeline.LogPipelineMetrics;
import com.smartlog.query.dto.LogSearchCriteria;
import com.smartlog.query.dto.LogSearchPage;
import com.smartlog.query.dto.LogSearchResult;
import com.smartlog.storage.repository.LogRepository;
import com.smartlog.trace.dto.TraceLogEvent;

class KafkaRawLogConsumerTest {

    @Test
    void consumesRawLogPublishesEnrichedAndPersists() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        RecordingKafkaPublisher publisher = new RecordingKafkaPublisher();
        RecordingRepository repository = new RecordingRepository();
        LogPipelineMetrics metrics = new LogPipelineMetrics();
        KafkaRawLogConsumer consumer = new KafkaRawLogConsumer(
                objectMapper,
                publisher,
                properties(),
                repository,
                alertEngine(),
                metrics
        );

        consumer.consumeRawLog(objectMapper.writeValueAsString(event("evt-raw")));

        assertThat(repository.savedEvents)
                .extracting(LogEvent::eventId)
                .containsExactly("evt-raw");
        assertThat(publisher.messages).hasSize(1);
        assertThat(publisher.messages.getFirst().topic()).isEqualTo("logs.enriched");
        assertThat(publisher.messages.getFirst().key()).isEqualTo("corr-12345");
        assertThat(metrics.persisted()).isEqualTo(1);
        assertThat(metrics.failed()).isZero();
    }

    @Test
    void sendsMalformedRawLogToDeadLetterTopic() {
        RecordingKafkaPublisher publisher = new RecordingKafkaPublisher();
        RecordingRepository repository = new RecordingRepository();
        LogPipelineMetrics metrics = new LogPipelineMetrics();
        KafkaRawLogConsumer consumer = new KafkaRawLogConsumer(
                objectMapper(),
                publisher,
                properties(),
                repository,
                alertEngine(),
                metrics
        );

        consumer.consumeRawLog("{not-json");

        assertThat(repository.savedEvents).isEmpty();
        assertThat(publisher.messages).hasSize(1);
        SentMessage deadLetter = publisher.messages.getFirst();
        assertThat(deadLetter.topic()).isEqualTo("logs.dead-letter");
        assertThat(deadLetter.payload()).contains("rawPayload");
        assertThat(deadLetter.payload()).contains("{not-json");
        assertThat(metrics.failed()).isEqualTo(1);
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

    private AlertEngine alertEngine() {
        AlertingProperties properties = new AlertingProperties();
        properties.setErrorThreshold(100);
        return new AlertEngine(new RecordingAlertRepository(), properties, new LogPipelineMetrics());
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

    private static final class RecordingRepository implements LogRepository {

        private final List<LogEvent> savedEvents = new ArrayList<>();

        @Override
        public void save(LogEvent event) {
            savedEvents.add(event);
        }

        @Override
        public void saveAll(List<LogEvent> events) {
            savedEvents.addAll(events);
        }

        @Override
        public LogSearchPage<LogSearchResult> search(LogSearchCriteria criteria) {
            return new LogSearchPage<>(0, 0, 0, List.of());
        }

        @Override
        public List<TraceLogEvent> findByCorrelationId(String correlationId) {
            return List.of();
        }

        @Override
        public List<TopErrorEvent> findErrorEventsSince(Instant from) {
            return List.of();
        }
    }

    private record SentMessage(String topic, String key, String payload) {
    }

    private static final class RecordingAlertRepository implements AlertRepository {

        @Override
        public AlertRecord save(AlertRecord alert) {
            return alert;
        }

        @Override
        public List<AlertRecord> findAll() {
            return List.of();
        }

        @Override
        public java.util.Optional<AlertRecord> findById(java.util.UUID alertId) {
            return java.util.Optional.empty();
        }
    }
}
