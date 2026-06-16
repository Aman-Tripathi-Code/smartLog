package com.smartlog.processing.masker;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.smartlog.common.model.LogEvent;

@Component
public class PiiMasker {

    private static final String MASK = "***MASKED***";
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "token",
            "secret",
            "authorization",
            "accountnumber",
            "cardnumber",
            "ssn",
            "pin"
    );

    public LogEvent mask(LogEvent event) {
        return new LogEvent(
                event.eventId(),
                event.eventTimestamp(),
                event.receivedAt(),
                event.serviceName(),
                event.environment(),
                event.level(),
                event.message(),
                event.correlationId(),
                event.traceId(),
                event.spanId(),
                event.parentSpanId(),
                event.userId(),
                event.transactionId(),
                event.module(),
                event.exceptionType(),
                maskText(event.stackTrace()),
                maskAttributes(event.attributes()),
                event.messageHash(),
                event.exceptionFingerprint(),
                event.severityScore(),
                event.ingestionDelayMs(),
                event.errorEvent()
        );
    }

    private Map<String, Object> maskAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        attributes.forEach((key, value) -> {
            if (isSensitive(key)) {
                masked.put(key, MASK);
            } else if (value instanceof Map<?, ?> nested) {
                masked.put(key, maskNested(nested));
            } else {
                masked.put(key, value);
            }
        });
        return Map.copyOf(masked);
    }

    private Map<String, Object> maskNested(Map<?, ?> nested) {
        Map<String, Object> masked = new LinkedHashMap<>();
        nested.forEach((key, value) -> {
            String stringKey = String.valueOf(key);
            masked.put(stringKey, isSensitive(stringKey) ? MASK : value);
        });
        return Map.copyOf(masked);
    }

    private String maskText(String value) {
        if (value == null) {
            return null;
        }
        String masked = value;
        for (String key : SENSITIVE_KEYS) {
            masked = masked.replaceAll("(?i)(" + key + "\\s*[=:]\\s*)\\S+", "$1" + MASK);
        }
        return masked;
    }

    private boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized);
    }
}
