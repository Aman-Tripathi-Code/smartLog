package com.smartlog.alerting.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import com.smartlog.alerting.model.AlertRecord;

public interface AlertRepository {

    AlertRecord save(AlertRecord alert);

    List<AlertRecord> findAll();

    Optional<AlertRecord> findById(UUID alertId);

    default Optional<AlertRecord> findRecentDuplicate(String alertType, String serviceName, Instant since) {
        return Optional.empty();
    }
}
