package com.smartlog.ai.incident.llm;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.smartlog.ai.incident.context.IncidentContext;
import com.smartlog.ai.incident.dto.IncidentSummaryDraft;

@Component
@ConditionalOnProperty(name = "smartlog.ai.llm-client", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    @Override
    public IncidentSummaryDraft summarizeIncident(IncidentContext context) {
        String serviceName = context.rootCause().serviceName();
        String message = context.rootCause().message();
        String exceptionType = context.rootCause().exceptionType() == null
                ? "an error signal"
                : context.rootCause().exceptionType();

        return new IncidentSummaryDraft(
                "Trace %s failed at %s after %d correlated log event(s).".formatted(
                        context.correlationId(),
                        serviceName,
                        context.traceEvents().size()
                ),
                "%s reported %s with message: %s".formatted(serviceName, exceptionType, message),
                context.impactedServices(),
                suggestedActions(serviceName, exceptionType, context.transactionId()),
                "MOCK_RULE_BASED",
                "MOCK_LLM"
        );
    }

    private List<String> suggestedActions(String serviceName, String exceptionType, String transactionId) {
        List<String> actions = new ArrayList<>();
        actions.add("Inspect recent logs and deployment changes for " + serviceName + ".");
        actions.add("Validate the failing path around " + exceptionType + ".");
        if (transactionId != null && !transactionId.isBlank()) {
            actions.add("Replay or inspect transaction " + transactionId + " in a lower environment.");
        }
        actions.add("Check downstream dependency health before retrying affected traffic.");
        return actions;
    }
}
