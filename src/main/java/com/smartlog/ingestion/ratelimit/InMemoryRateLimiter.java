package com.smartlog.ingestion.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter {

    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void check(String serviceName, int eventCount) {
        if (!properties.enabled()) {
            return;
        }
        String key = serviceName == null || serviceName.isBlank() ? "UNKNOWN" : serviceName.trim();
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(Instant.now(clock), 0));
        synchronized (counter) {
            Instant now = Instant.now(clock);
            Duration window = safeWindow();
            if (!counter.windowStart.plus(window).isAfter(now)) {
                counter.windowStart = now;
                counter.count = 0;
            }
            counter.count += eventCount;
            if (counter.count > Math.max(1, properties.defaultLimitPerWindow())) {
                throw new RateLimitExceededException("Rate limit exceeded for serviceName " + key);
            }
        }
    }

    private Duration safeWindow() {
        Duration window = properties.window();
        return window == null || window.isZero() || window.isNegative() ? Duration.ofMinutes(1) : window;
    }

    private static final class WindowCounter {
        private Instant windowStart;
        private int count;

        private WindowCounter(Instant windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
