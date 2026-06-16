package com.smartlog.alerting.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smartlog.alerting.model.AlertRecord;
import com.smartlog.alerting.repository.AlertRepository;
import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;
import com.smartlog.ingestion.pipeline.LogPipelineMetrics;

@Service
public class AlertEngine {

    private static final String HIGH_ERROR_RATE = "HIGH_ERROR_RATE";
    private static final String ACTIVE = "ACTIVE";

    private final AlertRepository repository;
    private final AlertingProperties properties;
    private final LogPipelineMetrics metrics;
    private final Map<String, Deque<Instant>> serviceErrorWindows = new HashMap<>();
    private final Map<String, Instant> lastAlertByService = new HashMap<>();

    public AlertEngine(AlertRepository repository, AlertingProperties properties, LogPipelineMetrics metrics) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
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

        AlertRecord alert = alert(event, windowStart, eventTime, count, window);
        lastAlertByService.put(serviceName, eventTime);
        AlertRecord savedAlert = repository.save(alert);
        metrics.incrementAlertsGenerated();
        return Optional.of(savedAlert);
    }

    private AlertRecord alert(LogEvent event, Instant windowStart, Instant windowEnd, int count, Duration window) {
        Instant now = Instant.now();
        String severity = event.level() == LogLevel.FATAL ? "CRITICAL" : "HIGH";
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
