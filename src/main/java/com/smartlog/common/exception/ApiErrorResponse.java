package com.smartlog.common.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String error,
        List<String> details,
        Instant timestamp
) {
}
