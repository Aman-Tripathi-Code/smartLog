package com.smartlog.testsupport;

public final class AsyncAssertions {

    private static final long TIMEOUT_MILLIS = 5_000;
    private static final long SLEEP_MILLIS = 25;

    private AsyncAssertions() {
    }

    public static void awaitAsserted(Runnable assertion) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        AssertionError lastError = null;

        while (System.currentTimeMillis() < deadline) {
            try {
                assertion.run();
                return;
            } catch (AssertionError error) {
                lastError = error;
                sleep();
            }
        }

        if (lastError != null) {
            throw lastError;
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(SLEEP_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for async assertion", exception);
        }
    }
}
