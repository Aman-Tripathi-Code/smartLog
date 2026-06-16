CREATE TABLE dead_letter_logs (
    id UUID PRIMARY KEY,
    event_id VARCHAR(100),
    source_topic VARCHAR(150) NOT NULL,
    raw_payload CLOB NOT NULL,
    reason CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dead_letter_logs_created
ON dead_letter_logs(created_at DESC);

CREATE INDEX idx_dead_letter_logs_event_created
ON dead_letter_logs(event_id, created_at DESC);

CREATE INDEX idx_dead_letter_logs_topic_created
ON dead_letter_logs(source_topic, created_at DESC);

CREATE TABLE service_registry (
    id UUID PRIMARY KEY,
    service_name VARCHAR(150) NOT NULL UNIQUE,
    owner_team VARCHAR(150),
    environment VARCHAR(50),
    rate_limit_per_minute INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_registry_environment
ON service_registry(environment, service_name);

CREATE INDEX idx_service_registry_name
ON service_registry(service_name);

CREATE INDEX idx_logs_exception_fingerprint_time
ON logs(exception_fingerprint, event_timestamp DESC);

CREATE INDEX idx_logs_severity_time
ON logs(severity_score, event_timestamp DESC);
