package com.smartlog.ingestion.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.smartlog.testsupport.AsyncAssertions.awaitAsserted;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.smartlog.alerting.engine.AlertEngine;
import com.smartlog.alerting.engine.AlertingProperties;
import com.smartlog.alerting.model.AlertRecord;
import com.smartlog.alerting.repository.AlertRepository;
import com.smartlog.analytics.topk.TopErrorEvent;
import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;
import com.smartlog.query.dto.LogSearchCriteria;
import com.smartlog.query.dto.LogSearchPage;
import com.smartlog.query.dto.LogSearchResult;
import com.smartlog.storage.repository.LogRepository;
import com.smartlog.trace.dto.TraceLogEvent;

class AsyncLogIngestionPipelineTest {

    @Test
    void rejectsWhenQueueIsFullWithoutDroppingSilently() throws InterruptedException {
        BlockingRepository repository = new BlockingRepository(true);
        LogPipelineMetrics metrics = new LogPipelineMetrics();
        AsyncLogIngestionPipeline pipeline = new AsyncLogIngestionPipeline(
                repository,
                alertEngine(),
                properties(1, 1, 1),
                metrics
        );

        pipeline.start();
        try {
            pipeline.publish(event("evt-1", LogLevel.INFO));
            assertThat(repository.firstWriteStarted.await(1, TimeUnit.SECONDS)).isTrue();

            pipeline.publish(event("evt-2", LogLevel.ERROR));

            assertThatThrownBy(() -> pipeline.publish(event("evt-3", LogLevel.ERROR)))
                    .isInstanceOf(LogQueueFullException.class)
                    .hasMessageContaining("queue is full");
            assertThat(metrics.rejected()).isEqualTo(1);
        } finally {
            repository.releaseWrites();
            pipeline.stop();
        }

        assertThat(metrics.accepted()).isEqualTo(2);
        assertThat(metrics.persisted()).isEqualTo(2);
    }

    @Test
    void workerPersistsLogsInBatches() {
        BlockingRepository repository = new BlockingRepository(false);
        LogPipelineMetrics metrics = new LogPipelineMetrics();
        AsyncLogIngestionPipeline pipeline = new AsyncLogIngestionPipeline(
                repository,
                alertEngine(),
                properties(10, 3, 1),
                metrics
        );

        pipeline.start();
        try {
            pipeline.publishAll(List.of(
                    event("evt-1", LogLevel.INFO),
                    event("evt-2", LogLevel.WARN),
                    event("evt-3", LogLevel.ERROR)
            ));

            awaitAsserted(() -> assertThat(repository.savedBatches).hasSize(1));
            assertThat(repository.savedBatches.getFirst())
                    .extracting(LogEvent::eventId)
                    .containsExactly("evt-1", "evt-2", "evt-3");
            assertThat(metrics.persisted()).isEqualTo(3);
            assertThat(metrics.batchesPersisted()).isEqualTo(1);
        } finally {
            pipeline.stop();
        }
    }

    private LogPipelineProperties properties(int queueCapacity, int batchSize, int workerThreads) {
        LogPipelineProperties properties = new LogPipelineProperties();
        properties.setQueueCapacity(queueCapacity);
        properties.setBatchSize(batchSize);
        properties.setWorkerThreads(workerThreads);
        properties.setPollTimeout(Duration.ofMillis(25));
        properties.setShutdownTimeout(Duration.ofSeconds(2));
        return properties;
    }

    private AlertEngine alertEngine() {
        AlertingProperties properties = new AlertingProperties();
        properties.setErrorThreshold(100);
        return new AlertEngine(new RecordingAlertRepository(), properties, new LogPipelineMetrics());
    }

    private LogEvent event(String eventId, LogLevel level) {
        Instant timestamp = Instant.parse("2026-06-16T10:30:00Z");
        return new LogEvent(
                eventId,
                timestamp,
                timestamp.plusMillis(10),
                "limit-check-service",
                "dev",
                level,
                "message " + eventId,
                "corr-async",
                "trace-async",
                "span-" + eventId,
                null,
                "U1001",
                "TF-9081",
                "ASYNC_TEST",
                level == LogLevel.ERROR ? "AsyncException" : null,
                null,
                java.util.Map.of()
        );
    }

    private static final class BlockingRepository implements LogRepository {

        private final boolean blockWrites;
        private final CountDownLatch releaseWrites = new CountDownLatch(1);
        private final CountDownLatch firstWriteStarted = new CountDownLatch(1);
        private final CopyOnWriteArrayList<List<LogEvent>> savedBatches = new CopyOnWriteArrayList<>();

        private BlockingRepository(boolean blockWrites) {
            this.blockWrites = blockWrites;
        }

        @Override
        public void save(LogEvent event) {
            saveAll(List.of(event));
        }

        @Override
        public void saveAll(List<LogEvent> events) {
            firstWriteStarted.countDown();
            if (blockWrites) {
                try {
                    releaseWrites.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            savedBatches.add(List.copyOf(events));
        }

        @Override
        public LogSearchPage<LogSearchResult> search(LogSearchCriteria criteria) {
            return new LogSearchPage<>(0, 0, 0, List.of());
        }

        @Override
        public List<TraceLogEvent> findByCorrelationId(String correlationId) {
            return List.of();
        }

        @Override
        public List<TopErrorEvent> findErrorEventsSince(Instant from) {
            return List.of();
        }

        private void releaseWrites() {
            releaseWrites.countDown();
        }
    }

    private static final class RecordingAlertRepository implements AlertRepository {

        @Override
        public AlertRecord save(AlertRecord alert) {
            return alert;
        }

        @Override
        public List<AlertRecord> findAll() {
            return List.of();
        }

        @Override
        public java.util.Optional<AlertRecord> findById(java.util.UUID alertId) {
            return java.util.Optional.empty();
        }
    }
}
