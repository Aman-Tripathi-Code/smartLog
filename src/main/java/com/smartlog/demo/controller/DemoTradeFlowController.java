package com.smartlog.demo.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlog.demo.service.DemoTradeFlowService;

@RestController
@RequestMapping("/api/v1/demo/trade-transactions")
public class DemoTradeFlowController {

    private final DemoTradeFlowService demoTradeFlowService;

    public DemoTradeFlowController(DemoTradeFlowService demoTradeFlowService) {
        this.demoTradeFlowService = demoTradeFlowService;
    }

    @PostMapping("/success")
    public Map<String, Object> success() {
        return demoTradeFlowService.success();
    }

    @PostMapping("/fail-limit")
    public Map<String, Object> failLimit() {
        return demoTradeFlowService.failLimit();
    }
}
