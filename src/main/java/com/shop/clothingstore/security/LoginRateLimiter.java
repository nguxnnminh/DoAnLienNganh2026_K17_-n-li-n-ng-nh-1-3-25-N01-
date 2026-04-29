package com.shop.clothingstore.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sliding-window rate limiter for login attempts.
 * Keyed by client IP. Max 10 attempts per 15-minute window.
 * No external dependencies — uses JDK concurrency primitives.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1_000L; // 15 minutes

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> attempts =
            new ConcurrentHashMap<>();

    /**
     * Returns true if the request from this key is within limits.
     * Records the attempt timestamp regardless.
     */
    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> timestamps =
                attempts.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            timestamps.removeIf(t -> now - t > WINDOW_MS);
            if (timestamps.size() >= MAX_ATTEMPTS) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    /** Purge expired entries hourly to prevent memory accumulation. */
    @Scheduled(fixedDelay = 3_600_000L)
    public void cleanup() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                entry.getValue().removeIf(t -> now - t > WINDOW_MS);
                return entry.getValue().isEmpty();
            }
        });
    }
}
