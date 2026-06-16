CREATE TABLE logs (
    id UUID PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    environment VARCHAR(50),
    level VARCHAR(20) NOT NULL,
    message CLOB NOT NULL,
    correlation_id VARCHAR(150),
    trace_id VARCHAR(150),
    span_id VARCHAR(150),
    parent_span_id VARCHAR(150),
    user_id VARCHAR(150),
    transaction_id VARCHAR(150),
    module VARCHAR(150),
    exception_type VARCHAR(255),
    stack_trace CLOB,
    attributes CLOB,
    message_hash VARCHAR(128),
    exception_fingerprint VARCHAR(255),
    severity_score INT NOT NULL DEFAULT 0,
    event_timestamp TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_logs_event_id UNIQUE (event_id)
);

CREATE INDEX idx_logs_service_time
ON logs(service_name, event_timestamp DESC);

CREATE INDEX idx_logs_correlation_time
ON logs(correlation_id, event_timestamp ASC);

CREATE INDEX idx_logs_transaction_time
ON logs(transaction_id, event_timestamp ASC);

CREATE INDEX idx_logs_user_time
ON logs(user_id, event_timestamp DESC);

CREATE INDEX idx_logs_level_time
ON logs(level, event_timestamp DESC);

CREATE INDEX idx_logs_exception_time
ON logs(exception_type, event_timestamp DESC);

CREATE INDEX idx_logs_trace_time
ON logs(trace_id, event_timestamp ASC);

CREATE INDEX idx_logs_message_hash_time
ON logs(message_hash, event_timestamp DESC);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    alert_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    message CLOB NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    event_count INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    sample_correlation_id VARCHAR(150),
    sample_transaction_id VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_service_created
ON alerts(service_name, created_at DESC);

CREATE INDEX idx_alerts_status_created
ON alerts(status, created_at DESC);

CREATE INDEX idx_alerts_type_created
ON alerts(alert_type, created_at DESC);
