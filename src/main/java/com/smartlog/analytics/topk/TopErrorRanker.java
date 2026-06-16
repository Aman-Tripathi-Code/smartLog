package com.smartlog.analytics.topk;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.springframework.stereotype.Component;

import com.smartlog.analytics.dto.TopErrorItem;

@Component
public class TopErrorRanker {

    public List<TopErrorItem> rank(List<TopErrorEvent> events, int limit) {
        if (limit < 1 || events.isEmpty()) {
            return List.of();
        }

        Map<String, ErrorAccumulator> counts = new HashMap<>();
        for (TopErrorEvent event : events) {
            String message = normalize(event.message());
            String exceptionType = normalizeException(event.exceptionType());
            String fingerprint = exceptionType + ":" + message;
            counts.computeIfAbsent(fingerprint, ignored -> new ErrorAccumulator(fingerprint, message, exceptionType))
                    .increment();
        }

        PriorityQueue<ErrorAccumulator> heap = new PriorityQueue<>(this::lowestPriorityFirst);
        for (ErrorAccumulator accumulator : counts.values()) {
            heap.offer(accumulator);
            if (heap.size() > limit) {
                heap.poll();
            }
        }

        return heap.stream()
                .map(ErrorAccumulator::toItem)
                .sorted(Comparator.comparingLong(TopErrorItem::count).reversed()
                        .thenComparing(TopErrorItem::errorFingerprint))
                .toList();
    }

    private int lowestPriorityFirst(ErrorAccumulator first, ErrorAccumulator second) {
        int countComparison = Long.compare(first.count, second.count);
        if (countComparison != 0) {
            return countComparison;
        }
        return second.fingerprint.compareTo(first.fingerprint);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        return value.trim();
    }

    private String normalizeException(String exceptionType) {
        return normalize(exceptionType);
    }

    private static final class ErrorAccumulator {

        private final String fingerprint;
        private final String message;
        private final String exceptionType;
        private long count;

        private ErrorAccumulator(String fingerprint, String message, String exceptionType) {
            this.fingerprint = fingerprint;
            this.message = message;
            this.exceptionType = exceptionType;
        }

        private void increment() {
            count++;
        }

        private TopErrorItem toItem() {
            return new TopErrorItem(fingerprint, message, exceptionType, count);
        }
    }
}
