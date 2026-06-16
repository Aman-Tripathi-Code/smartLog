package com.smartlog.alerting.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartlog.alerting.model.AlertRecord;
import com.smartlog.alerting.repository.AlertRepository;
import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.kafka.KafkaMessagePublisher;
import com.smartlog.ingestion.kafka.KafkaTopicsProperties;
import com.smartlog.ingestion.pipeline.LogPipelineMetrics;

@Service
public class AlertEngine {

    private static final String HIGH_ERROR_RATE = "HIGH_ERROR_RATE";
    private static final String ACTIVE = "ACTIVE";

    private final AlertRepository repository;
    private final AlertingProperties properties;
    private final LogPipelineMetrics metrics;
    private final KafkaMessagePublisher messagePublisher;
    private final KafkaTopicsProperties kafkaTopicsProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, Deque<Instant>> serviceErrorWindows = new HashMap<>();
    private final Map<String, Instant> lastAlertByService = new HashMap<>();

    public AlertEngine(AlertRepository repository, AlertingProperties properties, LogPipelineMetrics metrics) {
        this(
                repository,
                properties,
                metrics,
                (KafkaMessagePublisher) null,
                new KafkaTopicsProperties(),
                new ObjectMapper()
        );
    }

    @Autowired
    public AlertEngine(
            AlertRepository repository,
            AlertingProperties properties,
            LogPipelineMetrics metrics,
            ObjectProvider<KafkaMessagePublisher> messagePublisher,
            KafkaTopicsProperties kafkaTopicsProperties,
            ObjectMapper objectMapper
    ) {
        this(repository, properties, metrics, messagePublisher.getIfAvailable(), kafkaTopicsProperties, objectMapper);
    }

    private AlertEngine(
            AlertRepository repository,
            AlertingProperties properties,
            LogPipelineMetrics metrics,
            KafkaMessagePublisher messagePublisher,
            KafkaTopicsProperties kafkaTopicsProperties,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
        this.messagePublisher = messagePublisher;
        this.kafkaTopicsProperties = kafkaTopicsProperties;
        this.objectMapper = objectMapper;
    }

    public synchronized Optional<AlertRecord> evaluate(LogEvent event) {
        if (!isErrorEvent(event)) {
            return Optional.empty();
        }

        String serviceName = event.serviceName();
        Instant eventTime = event.eventTimestamp();
        Duration window = safeWindow();
        Instant windowStart = eventTime.minus(window);
        Deque<Instant> timestamps = serviceErrorWindows.computeIfAbsent(serviceName, ignored -> new ArrayDeque<>());

        timestamps.addLast(eventTime);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
            timestamps.removeFirst();
        }

        int count = timestamps.size();
        if (count <= positiveThreshold()) {
            return Optional.empty();
        }

        Instant lastAlertAt = lastAlertByService.get(serviceName);
        if (lastAlertAt != null && !lastAlertAt.isBefore(windowStart)) {
            return Optional.empty();
        }
        if (repository.findRecentDuplicate(HIGH_ERROR_RATE, serviceName, windowStart).isPresent()) {
            lastAlertByService.put(serviceName, eventTime);
            return Optional.empty();
        }

        AlertRecord alert = alert(event, windowStart, eventTime, count, window);
        lastAlertByService.put(serviceName, eventTime);
        AlertRecord savedAlert = repository.save(alert);
        publishAlert(savedAlert);
        metrics.incrementAlertsGenerated();
        return Optional.of(savedAlert);
    }

    private AlertRecord alert(LogEvent event, Instant windowStart, Instant windowEnd, int count, Duration window) {
        Instant now = Instant.now();
        String severity = severity(event, count);
        return new AlertRecord(
                UUID.randomUUID(),
                HIGH_ERROR_RATE,
                severity,
                event.serviceName(),
                "%s produced %d ERROR/FATAL logs in %s".formatted(event.serviceName(), count, format(window)),
                windowStart,
                windowEnd,
                count,
                ACTIVE,
                event.correlationId(),
                event.transactionId(),
                now,
                now
        );
    }

    private boolean isErrorEvent(LogEvent event) {
        return event != null
                && event.serviceName() != null
                && event.eventTimestamp() != null
                && (event.level() == LogLevel.ERROR || event.level() == LogLevel.FATAL);
    }

    private String severity(LogEvent event, int count) {
        if (event.level() == LogLevel.FATAL) {
            return "CRITICAL";
        }
        int threshold = positiveThreshold();
        if (count >= threshold * 3) {
            return "CRITICAL";
        }
        if (count >= threshold * 2) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private void publishAlert(AlertRecord alert) {
        if (messagePublisher == null) {
            return;
        }
        try {
            messagePublisher.send(kafkaTopicsProperties.topics().alertsCreated(), alert.serviceName(), objectMapper.writeValueAsString(alert));
        } catch (JsonProcessingException | RuntimeException ignored) {
            // Alert persistence is authoritative; alert-topic publishing is best-effort in the monolith.
        }
    }

    private int positiveThreshold() {
        return Math.max(1, properties.errorThreshold());
    }

    private Duration safeWindow() {
        Duration window = properties.window();
        return window == null || window.isZero() || window.isNegative() ? Duration.ofMinutes(5) : window;
    }

    private String format(Duration window) {
        if (window.toSecondsPart() == 0 && window.toMinutes() > 0) {
            return window.toMinutes() + " minutes";
        }
        return window.toString();
    }

}
