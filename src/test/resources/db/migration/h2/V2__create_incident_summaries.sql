CREATE TABLE incident_summaries (
    id UUID PRIMARY KEY,
    alert_id UUID,
    correlation_id VARCHAR(150) NOT NULL,
    summary CLOB NOT NULL,
    probable_cause CLOB NOT NULL,
    impacted_services CLOB NOT NULL,
    suggested_actions CLOB NOT NULL,
    confidence VARCHAR(50) NOT NULL,
    summarizer_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_incident_summaries_alert
        FOREIGN KEY (alert_id) REFERENCES alerts(id)
);

CREATE INDEX idx_incident_summaries_correlation_created
ON incident_summaries(correlation_id, created_at DESC);

CREATE INDEX idx_incident_summaries_alert_created
ON incident_summaries(alert_id, created_at DESC);
