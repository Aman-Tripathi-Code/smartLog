package com.smartlog.analytics.window;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WindowDurationParser {

    private static final Pattern SIMPLE_WINDOW = Pattern.compile("^(\\d+)([smhd])$");

    private WindowDurationParser() {
    }

    public static Duration parse(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ofMinutes(10);
        }

        Matcher matcher = SIMPLE_WINDOW.matcher(value.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("window must use a value like 30s, 10m, 2h, or 1d");
        }

        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("unsupported window unit");
        };
    }
}
