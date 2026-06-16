package com.smartlog.query.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlog.query.dto.LogSearchPage;
import com.smartlog.query.dto.LogSearchResult;
import com.smartlog.query.service.LogQueryService;

@RestController
@RequestMapping("/api/v1/logs")
public class LogSearchController {

    private final LogQueryService queryService;

    public LogSearchController(LogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/search")
    public LogSearchPage<LogSearchResult> search(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return queryService.search(
                serviceName,
                level,
                from,
                to,
                keyword,
                correlationId,
                traceId,
                userId,
                transactionId,
                page,
                size
        );
    }
}
