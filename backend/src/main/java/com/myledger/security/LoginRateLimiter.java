package com.myledger.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP login throttle: a simple token bucket (capacity 5, refilling 5 tokens per
 * minute). Backs off brute-force attempts on {@code /api/auth/login} without an external
 * dependency or distributed store — appropriate for a single-node home server. Buckets
 * are in memory and reset on restart.
 */
@Component
public class LoginRateLimiter {

    private static final int CAPACITY = 5;
    private static final long REFILL_INTERVAL_NANOS = 60_000_000_000L / CAPACITY; // 1 token / 12s

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String clientIp) {
        String key = clientIp == null ? "unknown" : clientIp;
        return buckets.computeIfAbsent(key, k -> new Bucket()).tryConsume();
    }

    /** Lazily-refilled token bucket. Guarded by its own monitor. */
    private static final class Bucket {
        private double tokens = CAPACITY;
        private long lastRefillNanos = System.nanoTime();

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0) {
                return;
            }
            double refilled = (double) elapsed / REFILL_INTERVAL_NANOS;
            if (refilled > 0) {
                tokens = Math.min(CAPACITY, tokens + refilled);
                lastRefillNanos = now;
            }
        }
    }
}
