package com.smartlog.trace.analyzer;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.trace.dto.RootCauseResponse;
import com.smartlog.trace.dto.TraceLogEvent;

@Component
public class BasicRootCauseAnalyzer {

    public static final String CONFIDENCE = "BASIC_RULE_BASED";

    public Optional<RootCauseResponse> analyze(String correlationId, List<TraceLogEvent> orderedEvents) {
        return orderedEvents.stream()
                .filter(this::isErrorOrFatal)
                .findFirst()
                .map(event -> new RootCauseResponse(
                        correlationId,
                        event.serviceName(),
                        event.message(),
                        event.exceptionType(),
                        event.timestamp(),
                        event.transactionId(),
                        event.userId(),
                        CONFIDENCE
                ));
    }

    private boolean isErrorOrFatal(TraceLogEvent event) {
        LogLevel level = LogLevel.from(event.level());
        return level == LogLevel.ERROR || level == LogLevel.FATAL;
    }
}
