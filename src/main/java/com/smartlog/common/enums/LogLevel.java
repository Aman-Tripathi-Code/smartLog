package com.smartlog.common.enums;

import java.util.Arrays;

public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL;

    public static LogLevel from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("level is required");
        }

        return Arrays.stream(values())
                .filter(level -> level.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("level must be one of TRACE, DEBUG, INFO, WARN, ERROR, FATAL"));
    }

    public int severityScore() {
        return switch (this) {
            case TRACE -> 1;
            case DEBUG -> 2;
            case INFO -> 3;
            case WARN -> 5;
            case ERROR -> 8;
            case FATAL -> 10;
        };
    }
}
