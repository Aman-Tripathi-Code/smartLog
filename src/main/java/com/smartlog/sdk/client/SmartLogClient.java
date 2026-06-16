package com.smartlog.sdk.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.smartlog.sdk.context.LogContext;
import com.smartlog.sdk.sender.AsyncLogSender;
import com.smartlog.sdk.sender.HttpLogTransport;

public class SmartLogClient implements AutoCloseable {

    private final String serviceName;
    private final String environment;
    private final AsyncLogSender sender;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private SmartLogClient(Builder builder) {
        this.serviceName = builder.serviceName;
        this.environment = builder.environment;
        this.sender = builder.sender == null
                ? new AsyncLogSender(new HttpLogTransport(builder.endpoint), builder.queueCapacity)
                : builder.sender;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.clock = builder.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void info(String message, LogContext context) {
        log("INFO", message, context, null);
    }

    public void warn(String message, LogContext context) {
        log("WARN", message, context, null);
    }

    public void debug(String message, LogContext context) {
        log("DEBUG", message, context, null);
    }

    public void error(String message, LogContext context, Throwable throwable) {
        log("ERROR", message, context, throwable);
    }

    public long dropped() {
        return sender.dropped();
    }

    @Override
    public void close() {
        sender.close();
    }

    private void log(String level, String message, LogContext context, Throwable throwable) {
        try {
            sender.send(objectMapper.writeValueAsString(payload(level, message, context, throwable)));
        } catch (JsonProcessingException exception) {
            // Logging must not break caller code.
        }
    }

    private Map<String, Object> payload(String level, String message, LogContext context, Throwable throwable) {
        LogContext safeContext = context == null ? LogContext.builder().build() : context;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("timestamp", Instant.now(clock));
        payload.put("serviceName", serviceName);
        payload.put("environment", environment);
        payload.put("level", level);
        payload.put("message", message);
        payload.put("correlationId", safeContext.correlationId());
        payload.put("traceId", safeContext.traceId());
        payload.put("spanId", safeContext.spanId());
        payload.put("parentSpanId", safeContext.parentSpanId());
        payload.put("userId", safeContext.userId());
        payload.put("transactionId", safeContext.transactionId());
        payload.put("module", safeContext.module());
        payload.put("attributes", safeContext.attributes());
        if (throwable != null) {
            payload.put("exceptionType", throwable.getClass().getSimpleName());
            payload.put("stackTrace", stackTrace(throwable));
        }
        return payload;
    }

    private String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public static final class Builder {
        private String serviceName;
        private String environment = "dev";
        private String endpoint = "http://localhost:8080/api/v1/logs";
        private int queueCapacity = 1000;
        private AsyncLogSender sender;
        private Clock clock = Clock.systemUTC();

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder sender(AsyncLogSender sender) {
            this.sender = sender;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public SmartLogClient build() {
            if (serviceName == null || serviceName.isBlank()) {
                throw new IllegalArgumentException("serviceName is required");
            }
            return new SmartLogClient(this);
        }
    }
}
