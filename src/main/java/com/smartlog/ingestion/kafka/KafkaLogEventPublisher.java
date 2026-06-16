package com.smartlog.ingestion.kafka;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.pipeline.LogEventPublisher;
import com.smartlog.ingestion.pipeline.LogPipelineMetrics;

@Component
@ConditionalOnProperty(name = "smartlog.ingestion.mode", havingValue = "kafka", matchIfMissing = true)
public class KafkaLogEventPublisher implements LogEventPublisher {

    private final KafkaMessagePublisher messagePublisher;
    private final KafkaTopicsProperties properties;
    private final ObjectMapper objectMapper;
    private final LogPipelineMetrics metrics;

    public KafkaLogEventPublisher(
            KafkaMessagePublisher messagePublisher,
            KafkaTopicsProperties properties,
            ObjectMapper objectMapper,
            LogPipelineMetrics metrics
    ) {
        this.messagePublisher = messagePublisher;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    public void publish(LogEvent event) {
        messagePublisher.send(properties.topics().raw(), kafkaKey(event), toJson(event));
        metrics.incrementAccepted(1);
    }

    @Override
    public void publishAll(List<LogEvent> events) {
        for (LogEvent event : events) {
            publish(event);
        }
    }

    private String kafkaKey(LogEvent event) {
        if (event.correlationId() != null && !event.correlationId().isBlank()) {
            return event.correlationId();
        }
        return event.serviceName();
    }

    private String toJson(LogEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new KafkaPublishException("Could not serialize log event for Kafka", exception);
        }
    }
}
