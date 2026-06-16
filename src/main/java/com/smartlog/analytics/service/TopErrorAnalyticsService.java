package com.smartlog.analytics.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartlog.analytics.dto.TopErrorsResponse;
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

    private Duration parseWindow(String windowValue) {
        try {
            return WindowDurationParser.parse(windowValue);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
