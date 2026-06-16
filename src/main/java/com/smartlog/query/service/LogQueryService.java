package com.smartlog.query.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.ingestion.validation.InvalidLogRequestException;
import com.smartlog.query.dto.LogSearchCriteria;
import com.smartlog.query.dto.LogSearchPage;
import com.smartlog.query.dto.LogSearchResult;
import com.smartlog.storage.repository.LogRepository;

@Service
public class LogQueryService {

    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    private final LogRepository repository;

    public LogQueryService(LogRepository repository) {
        this.repository = repository;
    }

    public LogSearchPage<LogSearchResult> search(
            String serviceName,
            String level,
            Instant from,
            Instant to,
            String keyword,
            String correlationId,
            String traceId,
            String userId,
            String transactionId,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidLogRequestException("page must be greater than or equal to 0");
        }
        if (size < 1) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidLogRequestException("from must be before or equal to to");
        }

        LogSearchCriteria criteria = new LogSearchCriteria(
                trimToNull(serviceName),
                normalizeLevel(level),
                from,
                to,
                trimToNull(keyword),
                trimToNull(correlationId),
                trimToNull(traceId),
                trimToNull(userId),
                trimToNull(transactionId),
                page,
                size
        );

        return repository.search(criteria);
    }

    private String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        return LogLevel.from(level).name();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
