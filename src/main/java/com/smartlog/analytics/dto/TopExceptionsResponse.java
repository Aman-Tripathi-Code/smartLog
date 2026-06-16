package com.smartlog.analytics.dto;

import java.util.List;

public record TopExceptionsResponse(
        String window,
        List<TopExceptionItem> items
) {
}
