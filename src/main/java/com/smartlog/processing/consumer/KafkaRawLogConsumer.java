package com.smartlog.processing.consumer;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.kafka.KafkaMessagePublisher;
import com.smartlog.ingestion.kafka.KafkaTopicsProperties;
import com.smartlog.ingestion.pipeline.LogPipelineMetrics;
import com.smartlog.storage.repository.LogRepository;

@Component
@ConditionalOnProperty(name = "smartlog.ingestion.mode", havingValue = "kafka", matchIfMissing = true)
public class KafkaRawLogConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaMessagePublisher messagePublisher;
    private final KafkaTopicsProperties properties;
    private final LogRepository repository;
    private final LogPipelineMetrics metrics;

    public KafkaRawLogConsumer(
            ObjectMapper objectMapper,
            KafkaMessagePublisher messagePublisher,
            KafkaTopicsProperties properties,
            LogRepository repository,
            LogPipelineMetrics metrics
    ) {
        this.objectMapper = objectMapper;
        this.messagePublisher = messagePublisher;
        this.properties = properties;
        this.repository = repository;
        this.metrics = metrics;
    }

    @KafkaListener(topics = "${smartlog.kafka.topics.raw}", groupId = "${spring.kafka.consumer.group-id:smartlog-processor}")
    public void consumeRawLog(String payload) {
        try {
            LogEvent event = parseAndValidate(payload);
            messagePublisher.send(properties.topics().enriched(), kafkaKey(event), objectMapper.writeValueAsString(event));
            repository.save(event);
            metrics.incrementPersisted(1);
        } catch (Exception exception) {
            metrics.incrementFailed(1);
            publishDeadLetter(payload, exception);
        }
    }

    private LogEvent parseAndValidate(String payload) throws JsonProcessingException {
        LogEvent event = objectMapper.readValue(payload, LogEvent.class);
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (event.serviceName() == null || event.serviceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        if (event.level() == null) {
            throw new IllegalArgumentException("level is required");
        }
        if (event.message() == null || event.message().isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        return event;
    }

    private void publishDeadLetter(String payload, Exception exception) {
        try {
            String deadLetterPayload = objectMapper.writeValueAsString(Map.of(
                    "rawPayload", payload == null ? "" : payload,
                    "reason", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                    "createdAt", Instant.now().toString()
            ));
            messagePublisher.send(properties.topics().deadLetter(), null, deadLetterPayload);
        } catch (JsonProcessingException jsonException) {
            throw new IllegalStateException("Could not serialize dead-letter message", jsonException);
        }
    }

    private String kafkaKey(LogEvent event) {
        if (event.correlationId() != null && !event.correlationId().isBlank()) {
            return event.correlationId();
        }
        return event.serviceName();
    }
}
