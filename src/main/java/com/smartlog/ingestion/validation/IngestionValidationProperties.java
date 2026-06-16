package com.smartlog.ingestion.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartlog.ingestion.validation")
public class IngestionValidationProperties {

    private int maxPayloadBytes = 262144;
    private int maxStackTraceChars = 12000;

    public int maxPayloadBytes() {
        return maxPayloadBytes;
    }

    public int getMaxPayloadBytes() {
        return maxPayloadBytes;
    }

    public void setMaxPayloadBytes(int maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public int maxStackTraceChars() {
        return maxStackTraceChars;
    }

    public int getMaxStackTraceChars() {
        return maxStackTraceChars;
    }

    public void setMaxStackTraceChars(int maxStackTraceChars) {
        this.maxStackTraceChars = maxStackTraceChars;
    }
}
