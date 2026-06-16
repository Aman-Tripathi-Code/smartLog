package com.smartlog.ingestion.kafka;

public interface KafkaMessagePublisher {

    void send(String topic, String key, String payload);
}
