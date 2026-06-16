CREATE TABLE incident_summaries (
    id UUID PRIMARY KEY,
    alert_id UUID REFERENCES alerts(id),
    correlation_id VARCHAR(150) NOT NULL,
    summary TEXT NOT NULL,
    probable_cause TEXT NOT NULL,
    impacted_services TEXT NOT NULL,
    suggested_actions TEXT NOT NULL,
    confidence VARCHAR(50) NOT NULL,
    summarizer_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incident_summaries_correlation_created
ON incident_summaries(correlation_id, created_at DESC);

CREATE INDEX idx_incident_summaries_alert_created
ON incident_summaries(alert_id, created_at DESC);
