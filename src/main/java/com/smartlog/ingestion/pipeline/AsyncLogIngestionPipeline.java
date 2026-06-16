package com.smartlog.ingestion.pipeline;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.smartlog.common.model.LogEvent;
import com.smartlog.storage.repository.LogRepository;

@Component
@ConditionalOnProperty(name = "smartlog.ingestion.mode", havingValue = "in-memory")
public class AsyncLogIngestionPipeline implements LogEventPublisher, SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncLogIngestionPipeline.class);

    private final LogRepository repository;
    private final LogPipelineMetrics metrics;
    private final BlockingQueue<LogEvent> queue;
    private final int batchSize;
    private final int workerThreads;
    private final Duration pollTimeout;
    private final Duration shutdownTimeout;
    private final ReentrantLock enqueueLock = new ReentrantLock();
    private final AtomicBoolean accepting = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;

    public AsyncLogIngestionPipeline(
            LogRepository repository,
            LogPipelineProperties properties,
            LogPipelineMetrics metrics
    ) {
        this.repository = repository;
        this.metrics = metrics;
        this.queue = new ArrayBlockingQueue<>(positive(properties.queueCapacity(), "queueCapacity"));
        this.batchSize = positive(properties.batchSize(), "batchSize");
        this.workerThreads = positive(properties.workerThreads(), "workerThreads");
        this.pollTimeout = properties.pollTimeout() == null ? Duration.ofMillis(200) : properties.pollTimeout();
        this.shutdownTimeout = properties.shutdownTimeout() == null ? Duration.ofSeconds(10) : properties.shutdownTimeout();
        this.metrics.bindQueueDepth(queue::size);
    }

    @Override
    public void publish(LogEvent event) {
        enqueueLock.lock();
        try {
            if (!accepting.get() || !queue.offer(event)) {
                reject(1);
            }
            metrics.incrementAccepted(1);
        } finally {
            enqueueLock.unlock();
        }
    }

    @Override
    public void publishAll(List<LogEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        enqueueLock.lock();
        try {
            if (!accepting.get() || queue.remainingCapacity() < events.size()) {
                reject(events.size());
            }
            events.forEach(queue::offer);
            metrics.incrementAccepted(events.size());
        } finally {
            enqueueLock.unlock();
        }
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        accepting.set(true);
        executorService = Executors.newFixedThreadPool(workerThreads, workerThreadFactory());
        for (int index = 0; index < workerThreads; index++) {
            executorService.submit(this::workerLoop);
        }
    }

    @Override
    public void stop() {
        accepting.set(false);
        running.set(false);
        if (executorService == null) {
            return;
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void workerLoop() {
        List<LogEvent> batch = new ArrayList<>(batchSize);
        while (running.get() || !queue.isEmpty()) {
            try {
                LogEvent event = queue.poll(pollTimeout.toMillis(), TimeUnit.MILLISECONDS);
                if (event != null) {
                    batch.add(event);
                    queue.drainTo(batch, batchSize - batch.size());
                }

                if (batch.size() >= batchSize || (event == null && !batch.isEmpty())) {
                    flush(batch);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        flush(batch);
    }

    private void flush(List<LogEvent> batch) {
        if (batch.isEmpty()) {
            return;
        }

        List<LogEvent> toPersist = List.copyOf(batch);
        batch.clear();
        try {
            repository.saveAll(toPersist);
            metrics.incrementPersisted(toPersist.size());
            metrics.incrementBatchesPersisted();
        } catch (RuntimeException exception) {
            metrics.incrementFailed(toPersist.size());
            LOGGER.error("Failed to persist {} queued log event(s)", toPersist.size(), exception);
        }
    }

    private void reject(int count) {
        metrics.incrementRejected(count);
        throw new LogQueueFullException("Log ingestion queue is full");
    }

    private ThreadFactory workerThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("smartlog-storage-writer-" + thread.threadId());
            thread.setDaemon(false);
            return thread;
        };
    }

    private int positive(int value, String propertyName) {
        if (value < 1) {
            throw new IllegalArgumentException(propertyName + " must be greater than 0");
        }
        return value;
    }
}
