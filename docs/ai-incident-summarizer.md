# AI Incident Summarizer

Checkpoint 10 adds a provider-neutral summarizer extension for failed traces. It does not call a paid model provider directly; production integrations should provide their own `LlmClient` bean behind configuration.

## Runtime Flow

```text
POST /api/v1/traces/{correlationId}/incident-summary
  -> fetch trace timeline
  -> fetch basic root-cause result
  -> build IncidentContext
  -> call LlmClient.summarizeIncident(context)
  -> store incident_summaries row
  -> return generated incident summary
```

The endpoint returns:

```json
{
  "incidentSummaryId": "4a933ed0-2376-4be2-9273-8fd8ecf07554",
  "correlationId": "corr-12345",
  "summary": "Trace corr-12345 failed at limit-check-service.",
  "probableCause": "LimitExceededException: Customer limit validation failed",
  "impactedServices": [
    "auth-service",
    "trade-service",
    "limit-check-service",
    "workflow-service"
  ],
  "suggestedActions": [
    "Inspect logs for correlationId corr-12345 around 2026-06-16T10:30:05Z",
    "Review recent changes and dependencies for limit-check-service"
  ],
  "confidence": "MOCK_RULE_BASED",
  "summarizerType": "MOCK_LLM",
  "createdAt": "2026-06-17T10:00:00Z"
}
```

## Extension Point

The abstraction is:

```java
public interface LlmClient {
    IncidentSummaryDraft summarizeIncident(IncidentContext context);
}
```

`MockLlmClient` is the default local/dev implementation:

```yaml
smartlog:
  ai:
    llm-client: mock
```

To add a real provider later, create a new `LlmClient` implementation with a different conditional property value. Keep provider credentials in environment variables or secret storage, not in source control.

## Persistence

Generated summaries are stored in `incident_summaries` with:

- `id`
- `alert_id`
- `correlation_id`
- `summary`
- `probable_cause`
- `impacted_services`
- `suggested_actions`
- `confidence`
- `summarizer_type`
- `created_at`

`alert_id` is nullable for trace-driven summaries. Alert-driven incident workflows can attach this relationship in a later checkpoint.

## Local Example

After ingesting a failed trace:

```bash
curl -X POST "http://localhost:8080/api/v1/traces/corr-12345/incident-summary"
```

The endpoint requires at least one ERROR or FATAL event in the trace because it reuses the existing root-cause detector.
