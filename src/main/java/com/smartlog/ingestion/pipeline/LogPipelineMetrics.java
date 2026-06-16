package com.smartlog.ingestion.pipeline;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

import org.springframework.stereotype.Component;

@Component
public class LogPipelineMetrics {

    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong persisted = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong batchesPersisted = new AtomicLong();
    private final AtomicLong alertsGenerated = new AtomicLong();
    private volatile IntSupplier queueDepthSupplier = () -> 0;

    public void bindQueueDepth(IntSupplier queueDepthSupplier) {
        this.queueDepthSupplier = queueDepthSupplier;
    }

    public void incrementAccepted(long count) {
        accepted.addAndGet(count);
    }

    public void incrementRejected(long count) {
        rejected.addAndGet(count);
    }

    public void incrementPersisted(long count) {
        persisted.addAndGet(count);
    }

    public void incrementFailed(long count) {
        failed.addAndGet(count);
    }

    public void incrementBatchesPersisted() {
        batchesPersisted.incrementAndGet();
    }

    public void incrementAlertsGenerated() {
        alertsGenerated.incrementAndGet();
    }

    public long accepted() {
        return accepted.get();
    }

    public long rejected() {
        return rejected.get();
    }

    public long persisted() {
        return persisted.get();
    }

    public long failed() {
        return failed.get();
    }

    public long batchesPersisted() {
        return batchesPersisted.get();
    }

    public long alertsGenerated() {
        return alertsGenerated.get();
    }

    public int queueDepth() {
        return queueDepthSupplier.getAsInt();
    }
}
