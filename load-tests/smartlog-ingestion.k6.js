import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    batch_ingestion: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 5),
      duration: __ENV.DURATION || '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const now = new Date().toISOString();
  const runId = `${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    logs: [
      logEvent(`evt-k6-gateway-${runId}`, now, 'api-gateway-service', 'INFO', 'Request received', runId),
      logEvent(`evt-k6-auth-${runId}`, now, 'auth-service', 'INFO', 'User authenticated', runId),
      logEvent(`evt-k6-trade-${runId}`, now, 'trade-service', 'INFO', 'Trade transaction created', runId),
      logEvent(`evt-k6-limit-${runId}`, now, 'limit-check-service', 'ERROR', 'Customer limit validation failed', runId, 'LimitExceededException'),
      logEvent(`evt-k6-workflow-${runId}`, now, 'workflow-service', 'WARN', 'Workflow stopped due to validation failure', runId),
    ],
  });

  const response = http.post(`${baseUrl}/api/v1/logs/batch`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(response, {
    'accepted': (res) => res.status === 202,
  });
  sleep(1);
}

function logEvent(eventId, timestamp, serviceName, level, message, correlationId, exceptionType = null) {
  return {
    eventId,
    timestamp,
    serviceName,
    environment: 'load-test',
    level,
    message,
    correlationId: `corr-k6-${correlationId}`,
    traceId: `trace-k6-${correlationId}`,
    userId: `U${__VU}`,
    transactionId: `TF-K6-${correlationId}`,
    exceptionType,
    attributes: {
      generator: 'k6',
      iteration: __ITER,
    },
  };
}
