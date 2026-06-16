package com.smartlog.analytics.topk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartlog.analytics.dto.TopErrorItem;

class TopErrorRankerTest {

    @Test
    void ranksTopErrorsUsingFrequencyAndLimit() {
        TopErrorRanker ranker = new TopErrorRanker();

        List<TopErrorItem> topErrors = ranker.rank(List.of(
                event("Customer limit validation failed", "LimitExceededException"),
                event("Customer limit validation failed", "LimitExceededException"),
                event("Customer limit validation failed", "LimitExceededException"),
                event("Database connection timed out", "TimeoutException"),
                event("Database connection timed out", "TimeoutException"),
                event("Document missing", "ValidationException")
        ), 2);

        assertThat(topErrors)
                .extracting(TopErrorItem::errorFingerprint)
                .containsExactly(
                        "LimitExceededException:Customer limit validation failed",
                        "TimeoutException:Database connection timed out"
                );
        assertThat(topErrors)
                .extracting(TopErrorItem::count)
                .containsExactly(3L, 2L);
    }

    private TopErrorEvent event(String message, String exceptionType) {
        return new TopErrorEvent(message, exceptionType);
    }
}
