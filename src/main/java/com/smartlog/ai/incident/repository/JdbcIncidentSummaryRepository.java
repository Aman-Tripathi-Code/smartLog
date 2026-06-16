package com.smartlog.ai.incident.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.smartlog.ai.incident.model.IncidentSummaryRecord;

@Repository
class JdbcIncidentSummaryRepository implements IncidentSummaryRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcIncidentSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public IncidentSummaryRecord save(IncidentSummaryRecord summary) {
        jdbcTemplate.update("""
                INSERT INTO incident_summaries (
                    id, alert_id, correlation_id, summary, probable_cause, impacted_services,
                    suggested_actions, confidence, summarizer_type, created_at
                )
                VALUES (
                    :id, :alertId, :correlationId, :summary, :probableCause, :impactedServices,
                    :suggestedActions, :confidence, :summarizerType, :createdAt
                )
                """, parameters(summary));
        return findSaved(summary);
    }

    @Override
    public Optional<IncidentSummaryRecord> findByAlertId(UUID alertId) {
        List<IncidentSummaryRecord> summaries = jdbcTemplate.query("""
                SELECT id, alert_id, correlation_id, summary, probable_cause, impacted_services,
                       suggested_actions, confidence, summarizer_type, created_at
                FROM incident_summaries
                WHERE alert_id = :alertId
                ORDER BY created_at DESC
                LIMIT 1
                """, new MapSqlParameterSource("alertId", alertId), this::mapSummary);
        return summaries.stream().findFirst();
    }

    private IncidentSummaryRecord findSaved(IncidentSummaryRecord summary) {
        return jdbcTemplate.queryForObject("""
                SELECT id, alert_id, correlation_id, summary, probable_cause, impacted_services,
                       suggested_actions, confidence, summarizer_type, created_at
                FROM incident_summaries
                WHERE id = :id
                """, new MapSqlParameterSource("id", summary.incidentSummaryId()), this::mapSummary);
    }

    private MapSqlParameterSource parameters(IncidentSummaryRecord summary) {
        return new MapSqlParameterSource()
                .addValue("id", summary.incidentSummaryId())
                .addValue("alertId", summary.alertId())
                .addValue("correlationId", summary.correlationId())
                .addValue("summary", summary.summary())
                .addValue("probableCause", summary.probableCause())
                .addValue("impactedServices", toJson(summary.impactedServices()))
                .addValue("suggestedActions", toJson(summary.suggestedActions()))
                .addValue("confidence", summary.confidence())
                .addValue("summarizerType", summary.summarizerType())
                .addValue("createdAt", Timestamp.from(summary.createdAt()));
    }

    private IncidentSummaryRecord mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new IncidentSummaryRecord(
                resultSet.getObject("id", UUID.class),
                nullableUuid(resultSet, "alert_id"),
                resultSet.getString("correlation_id"),
                resultSet.getString("summary"),
                resultSet.getString("probable_cause"),
                fromJson(resultSet.getString("impacted_services")),
                fromJson(resultSet.getString("suggested_actions")),
                resultSet.getString("confidence"),
                resultSet.getString("summarizer_type"),
                toInstant(resultSet.getTimestamp("created_at"))
        );
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("summary list fields must be JSON serializable", exception);
        }
    }

    private List<String> fromJson(String value) {
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read stored incident summary JSON", exception);
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private UUID nullableUuid(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        return value == null ? null : resultSet.getObject(columnName, UUID.class);
    }
}
