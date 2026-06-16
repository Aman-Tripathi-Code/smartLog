package com.smartlog.storage.repository;

import java.util.List;

import com.smartlog.analytics.topk.TopErrorEvent;
import com.smartlog.common.model.LogEvent;
import com.smartlog.query.dto.LogSearchCriteria;
import com.smartlog.query.dto.LogSearchPage;
import com.smartlog.query.dto.LogSearchResult;
import com.smartlog.trace.dto.TraceLogEvent;

public interface LogRepository {

    void save(LogEvent event);

    void saveAll(List<LogEvent> events);

    LogSearchPage<LogSearchResult> search(LogSearchCriteria criteria);

    List<TraceLogEvent> findByCorrelationId(String correlationId);

    default List<TraceLogEvent> findByTransactionId(String transactionId) {
        return List.of();
    }

    default List<String> findRecentCorrelationIdsByUserId(String userId, int limit) {
        return List.of();
    }

    List<TopErrorEvent> findErrorEventsSince(java.time.Instant from);

    default List<TopErrorEvent> findErrorEventsSince(java.time.Instant from, String serviceName) {
        return findErrorEventsSince(from);
    }
}
