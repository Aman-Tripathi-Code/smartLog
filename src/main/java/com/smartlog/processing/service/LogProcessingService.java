package com.smartlog.processing.service;

import org.springframework.stereotype.Service;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.common.model.LogEvent;
import com.smartlog.processing.fingerprint.FingerprintGenerator;
import com.smartlog.processing.masker.PiiMasker;

@Service
public class LogProcessingService {

    private final PiiMasker masker;
    private final FingerprintGenerator fingerprintGenerator;

    public LogProcessingService(PiiMasker masker, FingerprintGenerator fingerprintGenerator) {
        this.masker = masker;
        this.fingerprintGenerator = fingerprintGenerator;
    }

    public LogEvent process(LogEvent event) {
        LogEvent masked = masker.mask(event);
        LogLevel level = masked.level();
        long ingestionDelayMs = masked.eventTimestamp() == null || masked.receivedAt() == null
                ? 0
                : Math.max(0, masked.receivedAt().toEpochMilli() - masked.eventTimestamp().toEpochMilli());

        return new LogEvent(
                masked.eventId(),
                masked.eventTimestamp(),
                masked.receivedAt(),
                masked.serviceName(),
                masked.environment(),
                level,
                masked.message(),
                masked.correlationId(),
                masked.traceId(),
                masked.spanId(),
                masked.parentSpanId(),
                masked.userId(),
                masked.transactionId(),
                masked.module(),
                masked.exceptionType(),
                masked.stackTrace(),
                masked.attributes(),
                fingerprintGenerator.messageHash(masked.message()),
                fingerprintGenerator.exceptionFingerprint(masked),
                level == null ? 0 : level.severityScore(),
                ingestionDelayMs,
                level == LogLevel.ERROR || level == LogLevel.FATAL
        );
    }
}
