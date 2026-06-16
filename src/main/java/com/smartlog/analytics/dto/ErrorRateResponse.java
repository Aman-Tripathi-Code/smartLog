package com.smartlog.analytics.dto;

public record ErrorRateResponse(
        String serviceName,
        String window,
        long errorCount,
        double errorRatePerMinute
) {
}
