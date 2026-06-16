package com.smartlog.query.dto;

import java.util.List;

public record LogSearchPage<T>(
        long total,
        int page,
        int size,
        List<T> items
) {
}
