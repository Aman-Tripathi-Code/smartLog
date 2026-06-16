package com.smartlog.storage.repository;

import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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
import com.smartlog.query.dto.LogSearchCriteria;
import com.smartlog.query.dto.LogSearchPage;
import com.smartlog.query.dto.LogSearchResult;

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

    @Override
    public LogSearchPage<LogSearchResult> search(LogSearchCriteria criteria) {
        QueryParts queryParts = queryParts(criteria);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logs" + queryParts.whereClause(),
                queryParts.parameters(),
                Long.class
        );

        String searchSql = """
                SELECT event_id, event_timestamp, service_name, environment, level, message,
                       correlation_id, trace_id, user_id, transaction_id, exception_type
                FROM logs
                %s
                ORDER BY event_timestamp DESC, received_at DESC
                LIMIT :size OFFSET :offset
                """.formatted(queryParts.whereClause());

        MapSqlParameterSource parameters = queryParts.parameters()
                .addValue("size", criteria.size())
                .addValue("offset", criteria.offset());

        List<LogSearchResult> items = jdbcTemplate.query(searchSql, parameters, this::mapSearchResult);
        return new LogSearchPage<>(total == null ? 0 : total, criteria.page(), criteria.size(), items);
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

    private QueryParts queryParts(LogSearchCriteria criteria) {
        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        addEquals(conditions, parameters, "service_name", "serviceName", criteria.serviceName());
        addEquals(conditions, parameters, "level", "level", criteria.level());
        addEquals(conditions, parameters, "correlation_id", "correlationId", criteria.correlationId());
        addEquals(conditions, parameters, "trace_id", "traceId", criteria.traceId());
        addEquals(conditions, parameters, "user_id", "userId", criteria.userId());
        addEquals(conditions, parameters, "transaction_id", "transactionId", criteria.transactionId());

        if (criteria.from() != null) {
            conditions.add("event_timestamp >= :from");
            parameters.addValue("from", Timestamp.from(criteria.from()));
        }
        if (criteria.to() != null) {
            conditions.add("event_timestamp <= :to");
            parameters.addValue("to", Timestamp.from(criteria.to()));
        }
        if (criteria.keyword() != null) {
            conditions.add("LOWER(message) LIKE :keyword");
            parameters.addValue("keyword", "%" + criteria.keyword().toLowerCase() + "%");
        }

        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new QueryParts(whereClause, parameters);
    }

    private void addEquals(
            List<String> conditions,
            MapSqlParameterSource parameters,
            String columnName,
            String parameterName,
            String value
    ) {
        if (value != null) {
            conditions.add(columnName + " = :" + parameterName);
            parameters.addValue(parameterName, value);
        }
    }

    private LogSearchResult mapSearchResult(ResultSet resultSet, int rowNumber) throws SQLException {
        return new LogSearchResult(
                resultSet.getString("event_id"),
                toInstant(resultSet.getTimestamp("event_timestamp")),
                resultSet.getString("service_name"),
                resultSet.getString("environment"),
                resultSet.getString("level"),
                resultSet.getString("message"),
                resultSet.getString("correlation_id"),
                resultSet.getString("trace_id"),
                resultSet.getString("user_id"),
                resultSet.getString("transaction_id"),
                resultSet.getString("exception_type")
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record QueryParts(String whereClause, MapSqlParameterSource parameters) {
    }
}
