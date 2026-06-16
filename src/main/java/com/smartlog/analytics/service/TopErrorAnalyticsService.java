package com.smartlog.analytics.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartlog.analytics.dto.ErrorRateResponse;
import com.smartlog.analytics.dto.TopExceptionItem;
import com.smartlog.analytics.dto.TopExceptionsResponse;
import com.smartlog.analytics.dto.TopErrorsResponse;
import com.smartlog.analytics.topk.TopErrorEvent;
import com.smartlog.analytics.topk.TopErrorRanker;
import com.smartlog.analytics.window.WindowDurationParser;
import com.smartlog.storage.repository.LogRepository;

@Service
public class TopErrorAnalyticsService {

    private static final int MAX_LIMIT = 100;

    private final LogRepository repository;
    private final TopErrorRanker ranker;
    private final Clock clock;

    public TopErrorAnalyticsService(LogRepository repository, TopErrorRanker ranker, Clock clock) {
        this.repository = repository;
        this.ranker = ranker;
        this.clock = clock;
    }

    public TopErrorsResponse topErrors(String windowValue, int limit) {
        Duration window = parseWindow(windowValue);
        int boundedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        Instant from = Instant.now(clock).minus(window);
        return new TopErrorsResponse(
                windowValue,
                ranker.rank(repository.findErrorEventsSince(from), boundedLimit)
        );
    }

    public ErrorRateResponse errorRate(String serviceName, String windowValue) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceName is required");
        }
        Duration window = parseWindow(windowValue);
        Instant from = Instant.now(clock).minus(window);
        long count = repository.findErrorEventsSince(from, serviceName.trim()).size();
        double minutes = Math.max(1.0d / 60.0d, window.toMillis() / 60000.0d);
        return new ErrorRateResponse(serviceName.trim(), windowValue, count, count / minutes);
    }

    public TopExceptionsResponse topExceptions(String windowValue, int limit) {
        Duration window = parseWindow(windowValue);
        int boundedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        Instant from = Instant.now(clock).minus(window);
        return new TopExceptionsResponse(windowValue, topExceptions(repository.findErrorEventsSince(from), boundedLimit));
    }

    private java.util.List<TopExceptionItem> topExceptions(java.util.List<TopErrorEvent> events, int limit) {
        Map<String, Long> counts = new HashMap<>();
        for (TopErrorEvent event : events) {
            String exceptionType = event.exceptionType() == null || event.exceptionType().isBlank()
                    ? "UNKNOWN"
                    : event.exceptionType().trim();
            counts.merge(exceptionType, 1L, Long::sum);
        }

        PriorityQueue<TopExceptionItem> heap = new PriorityQueue<>(
                Comparator.comparingLong(TopExceptionItem::count)
                        .thenComparing(TopExceptionItem::exceptionType, Comparator.reverseOrder())
        );
        counts.forEach((exceptionType, count) -> {
            heap.offer(new TopExceptionItem(exceptionType, count));
            if (heap.size() > limit) {
                heap.poll();
            }
        });

        return heap.stream()
                .sorted(Comparator.comparingLong(TopExceptionItem::count).reversed()
                        .thenComparing(TopExceptionItem::exceptionType))
                .toList();
    }

    private Duration parseWindow(String windowValue) {
        try {
            return WindowDurationParser.parse(windowValue);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
