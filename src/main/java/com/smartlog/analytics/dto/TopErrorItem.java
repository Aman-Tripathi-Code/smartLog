package com.smartlog.analytics.dto;

public record TopErrorItem(
        String errorFingerprint,
        String message,
        String exceptionType,
        long count
) {
}
