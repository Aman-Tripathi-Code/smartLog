package com.smartlog.ingestion.kafka;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "smartlog.ingestion.mode", havingValue = "kafka", matchIfMissing = true)
class SpringKafkaMessagePublisher implements KafkaMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties properties;

    SpringKafkaMessagePublisher(KafkaTemplate<String, String> kafkaTemplate, KafkaTopicsProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void send(String topic, String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload)
                    .get(properties.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("Interrupted while publishing Kafka message", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaPublishException("Could not publish Kafka message to topic " + topic, exception);
        }
    }
}
