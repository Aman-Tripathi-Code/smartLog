package com.smartlog.ingestion.kafka;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartlog.kafka")
public class KafkaTopicsProperties {

    private Duration publishTimeout = Duration.ofSeconds(5);
    private final Topics topics = new Topics();

    public Duration publishTimeout() {
        return publishTimeout;
    }

    public Duration getPublishTimeout() {
        return publishTimeout;
    }

    public void setPublishTimeout(Duration publishTimeout) {
        this.publishTimeout = publishTimeout;
    }

    public Topics topics() {
        return topics;
    }

    public Topics getTopics() {
        return topics;
    }

    public static class Topics {

        private String raw = "logs.raw";
        private String enriched = "logs.enriched";
        private String deadLetter = "logs.dead-letter";

        public String raw() {
            return raw;
        }

        public String getRaw() {
            return raw;
        }

        public void setRaw(String raw) {
            this.raw = raw;
        }

        public String enriched() {
            return enriched;
        }

        public String getEnriched() {
            return enriched;
        }

        public void setEnriched(String enriched) {
            this.enriched = enriched;
        }

        public String deadLetter() {
            return deadLetter;
        }

        public String getDeadLetter() {
            return deadLetter;
        }

        public void setDeadLetter(String deadLetter) {
            this.deadLetter = deadLetter;
        }
    }
}
