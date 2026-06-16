package com.smartlog.analytics.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlog.analytics.dto.TopErrorsResponse;
import com.smartlog.analytics.service.TopErrorAnalyticsService;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final TopErrorAnalyticsService analyticsService;

    public AnalyticsController(TopErrorAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/top-errors")
    public TopErrorsResponse topErrors(
            @RequestParam(defaultValue = "10m") String window,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return analyticsService.topErrors(window, limit);
    }
}
