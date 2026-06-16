package com.smartlog.alerting.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.smartlog.alerting.model.AlertRecord;

@Repository
class JdbcAlertRepository implements AlertRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcAlertRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AlertRecord save(AlertRecord alert) {
        jdbcTemplate.update("""
                INSERT INTO alerts (
                    id, alert_type, severity, service_name, message, window_start, window_end,
                    event_count, status, sample_correlation_id, sample_transaction_id, created_at, updated_at
                )
                VALUES (
                    :id, :alertType, :severity, :serviceName, :message, :windowStart, :windowEnd,
                    :eventCount, :status, :sampleCorrelationId, :sampleTransactionId, :createdAt, :updatedAt
                )
                """, parameters(alert));
        return alert;
    }

    @Override
    public List<AlertRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT id, alert_type, severity, service_name, message, window_start, window_end,
                       event_count, status, sample_correlation_id, sample_transaction_id, created_at, updated_at
                FROM alerts
                ORDER BY created_at DESC
                """, this::mapAlert);
    }

    @Override
    public Optional<AlertRecord> findById(UUID alertId) {
        List<AlertRecord> alerts = jdbcTemplate.query("""
                SELECT id, alert_type, severity, service_name, message, window_start, window_end,
                       event_count, status, sample_correlation_id, sample_transaction_id, created_at, updated_at
                FROM alerts
                WHERE id = :id
                """, new MapSqlParameterSource("id", alertId), this::mapAlert);
        return alerts.stream().findFirst();
    }

    @Override
    public Optional<AlertRecord> findRecentDuplicate(String alertType, String serviceName, Instant since) {
        List<AlertRecord> alerts = jdbcTemplate.query("""
                SELECT id, alert_type, severity, service_name, message, window_start, window_end,
                       event_count, status, sample_correlation_id, sample_transaction_id, created_at, updated_at
                FROM alerts
                WHERE alert_type = :alertType
                  AND service_name = :serviceName
                  AND status = 'ACTIVE'
                  AND created_at >= :since
                ORDER BY created_at DESC
                LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("alertType", alertType)
                .addValue("serviceName", serviceName)
                .addValue("since", Timestamp.from(since)), this::mapAlert);
        return alerts.stream().findFirst();
    }

    private MapSqlParameterSource parameters(AlertRecord alert) {
        return new MapSqlParameterSource()
                .addValue("id", alert.alertId())
                .addValue("alertType", alert.alertType())
                .addValue("severity", alert.severity())
                .addValue("serviceName", alert.serviceName())
                .addValue("message", alert.message())
                .addValue("windowStart", Timestamp.from(alert.windowStart()))
                .addValue("windowEnd", Timestamp.from(alert.windowEnd()))
                .addValue("eventCount", alert.eventCount())
                .addValue("status", alert.status())
                .addValue("sampleCorrelationId", alert.sampleCorrelationId())
                .addValue("sampleTransactionId", alert.sampleTransactionId())
                .addValue("createdAt", Timestamp.from(alert.createdAt()))
                .addValue("updatedAt", Timestamp.from(alert.updatedAt()));
    }

    private AlertRecord mapAlert(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AlertRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("alert_type"),
                resultSet.getString("severity"),
                resultSet.getString("service_name"),
                resultSet.getString("message"),
                toInstant(resultSet.getTimestamp("window_start")),
                toInstant(resultSet.getTimestamp("window_end")),
                resultSet.getInt("event_count"),
                resultSet.getString("status"),
                resultSet.getString("sample_correlation_id"),
                resultSet.getString("sample_transaction_id"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
