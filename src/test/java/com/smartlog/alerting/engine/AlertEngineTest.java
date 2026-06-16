package com.smartlog.alerting.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlog.alerting.model.AlertRecord;
import com.smartlog.alerting.repository.AlertRepository;
import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;

class AlertEngineTest {

    @Test
    void triggersAlertWhenErrorCountExceedsThresholdInsideWindow() {
        RecordingAlertRepository repository = new RecordingAlertRepository();
        AlertEngine engine = new AlertEngine(repository, properties(2, Duration.ofMinutes(10)));

        engine.evaluate(event("evt-1", Instant.parse("2026-06-16T10:00:00Z")));
        engine.evaluate(event("evt-2", Instant.parse("2026-06-16T10:01:00Z")));
        Optional<AlertRecord> alert = engine.evaluate(event("evt-3", Instant.parse("2026-06-16T10:02:00Z")));

        assertThat(alert).isPresent();
        assertThat(repository.savedAlerts).hasSize(1);
        assertThat(repository.savedAlerts.getFirst().serviceName()).isEqualTo("limit-check-service");
        assertThat(repository.savedAlerts.getFirst().eventCount()).isEqualTo(3);
        assertThat(repository.savedAlerts.getFirst().alertType()).isEqualTo("HIGH_ERROR_RATE");
    }

    @Test
    void doesNotTriggerAlertBelowThreshold() {
        RecordingAlertRepository repository = new RecordingAlertRepository();
        AlertEngine engine = new AlertEngine(repository, properties(3, Duration.ofMinutes(10)));

        engine.evaluate(event("evt-1", Instant.parse("2026-06-16T10:00:00Z")));
        engine.evaluate(event("evt-2", Instant.parse("2026-06-16T10:01:00Z")));
        engine.evaluate(event("evt-3", Instant.parse("2026-06-16T10:02:00Z")));

        assertThat(repository.savedAlerts).isEmpty();
    }

    private AlertingProperties properties(int threshold, Duration window) {
        AlertingProperties properties = new AlertingProperties();
        properties.setErrorThreshold(threshold);
        properties.setWindow(window);
        return properties;
    }

    private LogEvent event(String eventId, Instant timestamp) {
        return new LogEvent(
                eventId,
                timestamp,
                timestamp.plusMillis(10),
                "limit-check-service",
                "dev",
                LogLevel.ERROR,
                "Customer limit validation failed",
                "corr-12345",
                "trace-abc",
                "span-" + eventId,
                null,
                "U1001",
                "TF-9081",
                "LIMIT_VALIDATION",
                "LimitExceededException",
                null,
                Map.of()
        );
    }

    private static final class RecordingAlertRepository implements AlertRepository {

        private final List<AlertRecord> savedAlerts = new ArrayList<>();

        @Override
        public AlertRecord save(AlertRecord alert) {
            savedAlerts.add(alert);
            return alert;
        }

        @Override
        public List<AlertRecord> findAll() {
            return List.copyOf(savedAlerts);
        }

        @Override
        public Optional<AlertRecord> findById(UUID alertId) {
            return savedAlerts.stream()
                    .filter(alert -> alert.alertId().equals(alertId))
                    .findFirst();
        }
    }
}
