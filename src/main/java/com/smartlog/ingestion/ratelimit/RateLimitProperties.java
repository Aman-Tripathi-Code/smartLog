package com.smartlog.ingestion.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartlog.ingestion.rate-limit")
public class RateLimitProperties {

    private boolean enabled = false;
    private int defaultLimitPerWindow = 1000;
    private Duration window = Duration.ofMinutes(1);

    public boolean enabled() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int defaultLimitPerWindow() {
        return defaultLimitPerWindow;
    }

    public int getDefaultLimitPerWindow() {
        return defaultLimitPerWindow;
    }

    public void setDefaultLimitPerWindow(int defaultLimitPerWindow) {
        this.defaultLimitPerWindow = defaultLimitPerWindow;
    }

    public Duration window() {
        return window;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}
