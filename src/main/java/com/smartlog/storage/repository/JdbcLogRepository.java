package com.smartlog.storage.repository;

import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.smartlog.common.model.LogEvent;

@Repository
class JdbcLogRepository implements LogRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String insertSql;

    JdbcLogRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.insertSql = buildInsertSql(attributesExpression(dataSource));
    }

    @Override
    public void save(LogEvent event) {
        jdbcTemplate.update(insertSql, parameters(event));
    }

    @Override
    public void saveAll(List<LogEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        MapSqlParameterSource[] batch = events.stream()
                .map(this::parameters)
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(insertSql, batch);
    }

    private MapSqlParameterSource parameters(LogEvent event) {
        return new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("eventId", event.eventId())
                .addValue("serviceName", event.serviceName())
                .addValue("environment", event.environment())
                .addValue("level", event.level().name())
                .addValue("message", event.message())
                .addValue("correlationId", event.correlationId())
                .addValue("traceId", event.traceId())
                .addValue("spanId", event.spanId())
                .addValue("parentSpanId", event.parentSpanId())
                .addValue("userId", event.userId())
                .addValue("transactionId", event.transactionId())
                .addValue("module", event.module())
                .addValue("exceptionType", event.exceptionType())
                .addValue("stackTrace", event.stackTrace())
                .addValue("attributes", toJson(event.attributes()))
                .addValue("eventTimestamp", Timestamp.from(event.eventTimestamp()))
                .addValue("receivedAt", Timestamp.from(event.receivedAt()));
    }

    private String toJson(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes == null ? Map.of() : attributes);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("attributes must be valid JSON", exception);
        }
    }

    private String attributesExpression(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String productName = metadata.getDatabaseProductName();
            if (productName != null && productName.toLowerCase().contains("postgres")) {
                return "CAST(:attributes AS jsonb)";
            }
            return ":attributes";
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect database type", exception);
        }
    }

    private String buildInsertSql(String attributesExpression) {
        return """
                INSERT INTO logs (
                    id, event_id, service_name, environment, level, message,
                    correlation_id, trace_id, span_id, parent_span_id, user_id,
                    transaction_id, module, exception_type, stack_trace, attributes,
                    event_timestamp, received_at
                )
                VALUES (
                    :id, :eventId, :serviceName, :environment, :level, :message,
                    :correlationId, :traceId, :spanId, :parentSpanId, :userId,
                    :transactionId, :module, :exceptionType, :stackTrace, %s,
                    :eventTimestamp, :receivedAt
                )
                """.formatted(attributesExpression);
    }
}
