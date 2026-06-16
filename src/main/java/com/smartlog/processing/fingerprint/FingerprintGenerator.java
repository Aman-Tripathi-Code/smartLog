package com.smartlog.processing.fingerprint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.smartlog.common.model.LogEvent;

@Component
public class FingerprintGenerator {

    public String messageHash(String message) {
        return sha256(normalize(message));
    }

    public String exceptionFingerprint(LogEvent event) {
        String exceptionType = normalize(event.exceptionType());
        String message = normalize(event.message());
        return exceptionType + ":" + messageHash(message).substring(0, 16);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
