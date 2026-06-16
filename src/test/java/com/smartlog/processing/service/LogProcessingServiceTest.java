package com.smartlog.processing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;
import com.smartlog.processing.fingerprint.FingerprintGenerator;
import com.smartlog.processing.masker.PiiMasker;

class LogProcessingServiceTest {

    private final LogProcessingService service = new LogProcessingService(new PiiMasker(), new FingerprintGenerator());

    @Test
    void masksPiiAndAddsSearchableEnrichmentFields() {
        LogEvent processed = service.process(new LogEvent(
                "evt-processing",
                Instant.parse("2026-06-16T10:30:00Z"),
                Instant.parse("2026-06-16T10:30:03Z"),
                "trade-service",
                "dev",
                LogLevel.ERROR,
                "Payment token validation failed",
                "corr-processing",
                "trace-processing",
                "span-processing",
                null,
                "U1001",
                "TF-9081",
                "PAYMENT",
                "PaymentException",
                "PaymentException: token=secret-token",
                Map.of("token", "secret-token", "customerId", "C1001")
        ));

        assertThat(processed.attributes()).containsEntry("token", "***MASKED***");
        assertThat(processed.attributes()).containsEntry("customerId", "C1001");
        assertThat(processed.stackTrace()).contains("token=***MASKED***").doesNotContain("secret-token");
        assertThat(processed.messageHash()).isNotBlank();
        assertThat(processed.exceptionFingerprint()).startsWith("PaymentException:");
        assertThat(processed.severityScore()).isEqualTo(8);
        assertThat(processed.ingestionDelayMs()).isEqualTo(3000);
        assertThat(processed.errorEvent()).isTrue();
    }
}
