package com.smartlog.alerting.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.smartlog.alerting.model.AlertRecord;

public interface AlertRepository {

    AlertRecord save(AlertRecord alert);

    List<AlertRecord> findAll();

    Optional<AlertRecord> findById(UUID alertId);
}
