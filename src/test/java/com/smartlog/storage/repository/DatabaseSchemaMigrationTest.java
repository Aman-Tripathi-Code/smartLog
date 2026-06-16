package com.smartlog.storage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseSchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesRequiredTables() {
        assertTableExists("logs");
        assertTableExists("alerts");
    }

    @Test
    void flywayCreatesRequiredLogIndexes() {
        assertConstraintExists("ux_logs_event_id");
        assertIndexExists("idx_logs_correlation_time");
        assertIndexExists("idx_logs_trace_time");
        assertIndexExists("idx_logs_user_time");
        assertIndexExists("idx_logs_transaction_time");
        assertIndexExists("idx_logs_service_time");
        assertIndexExists("idx_logs_level_time");
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = ?",
                Integer.class,
                tableName
        );
        assertThat(count).isEqualTo(1);
    }

    private void assertIndexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE LOWER(index_name) = ?",
                Integer.class,
                indexName
        );
        assertThat(count).isEqualTo(1);
    }

    private void assertConstraintExists(String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE LOWER(constraint_name) = ?",
                Integer.class,
                constraintName
        );
        assertThat(count).isEqualTo(1);
    }
}
