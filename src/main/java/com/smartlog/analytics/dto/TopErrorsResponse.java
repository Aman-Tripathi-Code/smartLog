package com.smartlog.analytics.dto;

import java.util.List;

public record TopErrorsResponse(
        String window,
        List<TopErrorItem> items
) {
}
