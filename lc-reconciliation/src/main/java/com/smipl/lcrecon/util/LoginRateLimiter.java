package com.smipl.lcrecon.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection for the authentication endpoint (VAPT WEB_VUL_04 -
 * missing rate limiting on login). After {@link #MAX_ATTEMPTS} failed attempts within a
 * rolling window for a given key (client IP + username), further attempts are locked out
 * for {@link #LOCKOUT_MS}. A successful login clears the counter.
 */
public final class LoginRateLimiter {

    private LoginRateLimiter() {}

    public static final int MAX_ATTEMPTS = 5;
    public static final long WINDOW_MS = 15 * 60 * 1000L;   // count failures within 15 min
    public static final long LOCKOUT_MS = 15 * 60 * 1000L;  // lock for 15 min once tripped

    private static final Map<String, Attempt> ATTEMPTS = new ConcurrentHashMap<>();

    private static final class Attempt {
        int count;
        long windowStart;
        long lockedUntil;
    }

    /** True if the key is currently locked out. */
    public static boolean isLocked(String key, long now) {
        Attempt a = ATTEMPTS.get(key);
        return a != null && a.lockedUntil > now;
    }

    /** Seconds until the key is unlocked (0 if not locked). */
    public static long retryAfterSeconds(String key, long now) {
        Attempt a = ATTEMPTS.get(key);
        if (a == null || a.lockedUntil <= now) return 0;
        return (a.lockedUntil - now + 999) / 1000;
    }

    /** Record a failed attempt; trips a lockout once the threshold is reached in-window. */
    public static void recordFailure(String key, long now) {
        Attempt a = ATTEMPTS.computeIfAbsent(key, k -> new Attempt());
        synchronized (a) {
            if (a.lockedUntil > now) return; // already locked
            if (a.windowStart == 0 || now - a.windowStart > WINDOW_MS) {
                a.windowStart = now;
                a.count = 0;
            }
            a.count++;
            if (a.count >= MAX_ATTEMPTS) {
                a.lockedUntil = now + LOCKOUT_MS;
            }
        }
    }

    /** Clear the counter for a key (called after a successful login). */
    public static void reset(String key) {
        ATTEMPTS.remove(key);
    }
}
