package com.springbootlearning.learningspringboot.bookingroom.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockDuration;
    private final ConcurrentHashMap<String, Attempt> attemptsByUsername = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.login.max-attempts:5}") int maxAttempts,
            @Value("${app.login.lock-duration-minutes:15}") long lockDurationMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockDurationMinutes);
    }

    public boolean isBlocked(String username) {
        Attempt attempt = attemptsByUsername.get(normalize(username));
        if (attempt == null) {
            return false;
        }
        if (attempt.count.get() < maxAttempts) {
            return false;
        }
        if (Instant.now().isAfter(attempt.lockedUntil)) {
            attemptsByUsername.remove(normalize(username));
            return false;
        }
        return true;
    }

    public void recordFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        attemptsByUsername.compute(normalize(username), (key, existing) -> {
            Attempt attempt = existing == null ? new Attempt() : existing;
            int count = attempt.count.incrementAndGet();
            if (count >= maxAttempts) {
                attempt.lockedUntil = Instant.now().plus(lockDuration);
            }
            return attempt;
        });
    }

    public void recordSuccess(String username) {
        if (username == null) {
            return;
        }
        attemptsByUsername.remove(normalize(username));
    }

    private String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static class Attempt {
        private final AtomicInteger count = new AtomicInteger(0);
        private Instant lockedUntil = Instant.now();
    }
}
