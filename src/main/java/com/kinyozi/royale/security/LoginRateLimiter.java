package com.kinyozi.royale.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory login rate limiter (per-identifier — usually the email).
 * After {@code maxAttempts} consecutive failures the identifier is locked
 * out for {@code lockoutMinutes}. Successful logins reset the counter.
 *
 * Suitable for a single-node deployment. For horizontally scaled
 * deployments this should be replaced with a shared store (e.g. Redis).
 */
@Component
public class LoginRateLimiter {

    private static final class Entry {
        int failures;
        Instant lockedUntil;
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockout;

    public LoginRateLimiter(
            @Value("${royale.security.login.maxAttempts:5}") int maxAttempts,
            @Value("${royale.security.login.lockoutMinutes:15}") int lockoutMinutes) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockout = Duration.ofMinutes(Math.max(1, lockoutMinutes));
    }

    /** @return seconds remaining in the lockout, or 0 if the caller may proceed. */
    public long checkAllowed(String key) {
        if (key == null || key.isBlank()) return 0;
        Entry e = entries.get(key.toLowerCase());
        if (e == null || e.lockedUntil == null) return 0;
        long remaining = Duration.between(Instant.now(), e.lockedUntil).getSeconds();
        return remaining > 0 ? remaining : 0;
    }

    public void recordFailure(String key) {
        if (key == null || key.isBlank()) return;
        entries.compute(key.toLowerCase(), (k, cur) -> {
            Entry e = cur == null ? new Entry() : cur;
            if (e.lockedUntil != null && Instant.now().isAfter(e.lockedUntil)) {
                e.failures = 0;
                e.lockedUntil = null;
            }
            e.failures++;
            if (e.failures >= maxAttempts) {
                e.lockedUntil = Instant.now().plus(lockout);
            }
            return e;
        });
    }

    public void recordSuccess(String key) {
        if (key == null || key.isBlank()) return;
        entries.remove(key.toLowerCase());
    }
}
