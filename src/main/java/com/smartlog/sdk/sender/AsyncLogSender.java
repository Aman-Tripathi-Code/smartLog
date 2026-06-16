package com.smartlog.sdk.sender;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncLogSender implements AutoCloseable {

    private final BlockingQueue<String> queue;
    private final HttpLogTransport transport;
    private final ExecutorService executorService;
    private final AtomicLong dropped = new AtomicLong();
    private volatile boolean running = true;

    public AsyncLogSender(HttpLogTransport transport, int capacity) {
        this.transport = transport;
        this.queue = new ArrayBlockingQueue<>(Math.max(1, capacity));
        this.executorService = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "smartlog-sdk-sender");
            thread.setDaemon(true);
            return thread;
        });
        this.executorService.submit(this::loop);
    }

    public void send(String jsonPayload) {
        if (!queue.offer(jsonPayload)) {
            dropped.incrementAndGet();
        }
    }

    public void flush() {
        while (!queue.isEmpty()) {
            Thread.onSpinWait();
        }
    }

    public long dropped() {
        return dropped.get();
    }

    @Override
    public void close() {
        running = false;
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void loop() {
        while (running || !queue.isEmpty()) {
            try {
                String payload = queue.poll(200, TimeUnit.MILLISECONDS);
                if (payload != null) {
                    postWithRetry(payload);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void postWithRetry(String payload) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (transport.post(payload)) {
                    return;
                }
            } catch (Exception ignored) {
                // SDK logging must not leak transport failures to business code.
            }
            try {
                Thread.sleep(50L * attempt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        dropped.incrementAndGet();
    }
}
