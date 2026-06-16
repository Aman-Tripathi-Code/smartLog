package com.smartlog.ingestion.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "smartlog.ingestion.mode", havingValue = "kafka", matchIfMissing = true)
public class KafkaTopicConfiguration {

    @Bean
    NewTopic rawLogsTopic(KafkaTopicsProperties properties) {
        return TopicBuilder.name(properties.topics().raw())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic enrichedLogsTopic(KafkaTopicsProperties properties) {
        return TopicBuilder.name(properties.topics().enriched())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic deadLetterLogsTopic(KafkaTopicsProperties properties) {
        return TopicBuilder.name(properties.topics().deadLetter())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic alertsCreatedTopic(KafkaTopicsProperties properties) {
        return TopicBuilder.name(properties.topics().alertsCreated())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
