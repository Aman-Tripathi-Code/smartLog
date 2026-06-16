package com.smartlog.analytics.dto;

public record TopExceptionItem(
        String exceptionType,
        long count
) {
}
