package com.smartlog.sdk.context;

import java.util.Map;

public record LogContext(
        String correlationId,
        String traceId,
        String spanId,
        String parentSpanId,
        String userId,
        String transactionId,
        String module,
        Map<String, Object> attributes
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String correlationId;
        private String traceId;
        private String spanId;
        private String parentSpanId;
        private String userId;
        private String transactionId;
        private String module;
        private Map<String, Object> attributes = Map.of();

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder parentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
            return this;
        }

        public LogContext build() {
            return new LogContext(correlationId, traceId, spanId, parentSpanId, userId, transactionId, module, attributes);
        }
    }
}
