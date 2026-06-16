# SmartLog Benchmark Report

Use this template to record real benchmark results. Do not fill in numbers until the run has completed.

## Environment

| Item | Value |
|---|---|
| Date | TBD |
| Machine | TBD |
| CPU | TBD |
| Memory | TBD |
| Java version | TBD |
| SmartLog commit | TBD |
| Ingestion mode | kafka / in-memory |
| PostgreSQL version | TBD |
| Kafka version | TBD |

## Commands

```bash
docker compose up -d postgres kafka kafka-init prometheus grafana
mvn spring-boot:run
k6 run load-tests/smartlog-ingestion.k6.js
```

Optional parameters:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e VUS=10 -e DURATION=5m load-tests/smartlog-ingestion.k6.js
```

## Batch Ingestion

| Metric | Result |
|---|---|
| Total logs sent | TBD |
| Accepted requests | TBD |
| Failed requests | TBD |
| Average latency | TBD |
| p95 latency | TBD |
| p99 latency | TBD |
| Logs persisted/sec | TBD |
| Queue size max | TBD |
| Kafka consumer lag max | TBD |

## Trace Lookup

| Metric | Result |
|---|---|
| Trace size | TBD |
| Average latency | TBD |
| p95 latency | TBD |
| p99 latency | TBD |

## Top-K Analytics

| Metric | Result |
|---|---|
| Window | TBD |
| Limit | TBD |
| Average latency | TBD |
| p95 latency | TBD |

## Observations

- TBD

## Follow-Up Optimizations

- TBD
