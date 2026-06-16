package com.smartlog.processing.consumer;

import java.sql.Timestamp;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcDeadLetterRepository implements DeadLetterRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcDeadLetterRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DeadLetterRecord save(DeadLetterRecord record) {
        jdbcTemplate.update("""
                INSERT INTO dead_letter_logs (id, event_id, source_topic, raw_payload, reason, created_at)
                VALUES (:id, :eventId, :sourceTopic, :rawPayload, :reason, :createdAt)
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("eventId", record.eventId())
                .addValue("sourceTopic", record.sourceTopic())
                .addValue("rawPayload", record.rawPayload())
                .addValue("reason", record.reason())
                .addValue("createdAt", Timestamp.from(record.createdAt())));
        return record;
    }
}
