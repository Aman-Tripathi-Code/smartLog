package com.smartlog.trace.analyzer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.trace.dto.RootCauseResponse;
import com.smartlog.trace.dto.TraceLogEvent;

@Component
public class BasicRootCauseAnalyzer {

    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String LOW = "LOW";
    public static final String CONFIDENCE = HIGH;
    private static final Set<String> FAILURE_KEYWORDS = Set.of(
            "failed",
            "failure",
            "timeout",
            "exception",
            "rejected",
            "declined",
            "invalid",
            "unavailable",
            "connection refused",
            "limit exceeded"
    );

    public Optional<RootCauseResponse> analyze(String correlationId, List<TraceLogEvent> orderedEvents) {
        return selectedIndex(orderedEvents)
                .map(index -> response(correlationId, orderedEvents, index));
    }

    private Optional<Integer> selectedIndex(List<TraceLogEvent> orderedEvents) {
        return firstBySignal(orderedEvents, event -> level(event) == LogLevel.FATAL && hasException(event))
                .or(() -> firstBySignal(orderedEvents, event -> level(event) == LogLevel.FATAL))
                .or(() -> firstBySignal(orderedEvents, event -> level(event) == LogLevel.ERROR && hasException(event)))
                .or(() -> firstBySignal(orderedEvents, event -> level(event) == LogLevel.ERROR))
                .or(() -> firstBySignal(orderedEvents, this::isFailureWarn));
    }

    private RootCauseResponse response(String correlationId, List<TraceLogEvent> orderedEvents, int index) {
        TraceLogEvent event = orderedEvents.get(index);
        return new RootCauseResponse(
                        correlationId,
                        event.serviceName(),
                        event.message(),
                        event.exceptionType(),
                        event.timestamp(),
                        event.transactionId(),
                        event.userId(),
                confidence(event),
                reason(event),
                supportingEvents(orderedEvents, index)
        );
    }

    private Optional<Integer> firstBySignal(List<TraceLogEvent> orderedEvents, java.util.function.Predicate<TraceLogEvent> predicate) {
        return IntStream.range(0, orderedEvents.size())
                .filter(index -> predicate.test(orderedEvents.get(index)))
                .boxed()
                .findFirst();
    }

    private String confidence(TraceLogEvent event) {
        LogLevel level = level(event);
        if ((level == LogLevel.FATAL || level == LogLevel.ERROR) && hasException(event)) {
            return HIGH;
        }
        if (level == LogLevel.FATAL || level == LogLevel.ERROR) {
            return MEDIUM;
        }
        return LOW;
    }

    private String reason(TraceLogEvent event) {
        LogLevel level = level(event);
        if (level == LogLevel.WARN) {
            return "WARN event contains a failure keyword and no ERROR/FATAL signal was present";
        }
        if (hasException(event)) {
            return "First prioritized " + level.name() + " event with exception type in the distributed request timeline";
        }
        return "First prioritized " + level.name() + " event in the distributed request timeline";
    }

    private List<TraceLogEvent> supportingEvents(List<TraceLogEvent> orderedEvents, int index) {
        int from = Math.max(0, index - 1);
        int to = Math.min(orderedEvents.size(), index + 2);
        return IntStream.range(from, to)
                .filter(candidate -> candidate != index)
                .mapToObj(orderedEvents::get)
                .toList();
    }

    private boolean isFailureWarn(TraceLogEvent event) {
        if (level(event) != LogLevel.WARN || event.message() == null) {
            return false;
        }
        String message = event.message().toLowerCase();
        return FAILURE_KEYWORDS.stream().anyMatch(message::contains);
    }

    private LogLevel level(TraceLogEvent event) {
        return LogLevel.from(event.level());
    }

    private boolean hasException(TraceLogEvent event) {
        return event.exceptionType() != null && !event.exceptionType().isBlank();
    }
}
