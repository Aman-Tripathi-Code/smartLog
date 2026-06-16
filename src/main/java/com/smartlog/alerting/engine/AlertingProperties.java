package com.smartlog.alerting.engine;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartlog.alerting")
public class AlertingProperties {

    private int errorThreshold = 100;
    private Duration window = Duration.ofMinutes(5);

    public int errorThreshold() {
        return errorThreshold;
    }

    public void setErrorThreshold(int errorThreshold) {
        this.errorThreshold = errorThreshold;
    }

    public Duration window() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}
