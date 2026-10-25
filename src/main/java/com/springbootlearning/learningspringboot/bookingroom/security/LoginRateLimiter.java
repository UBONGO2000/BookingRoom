package com.springbootlearning.learningspringboot.bookingroom.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de tentatives de connexion par client (adresse IP) sur une
 * fenetre glissante simple. Complementaire du verrouillage par compte assure
 * par {@link LoginAttemptService}.
 */
@Service
public class LoginRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${app.login.rate-limit.max-requests:20}") int maxRequests,
            @Value("${app.login.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("app.login.rate-limit.max-requests doit etre >= 1");
        }
        if (windowSeconds < 1) {
            throw new IllegalArgumentException("app.login.rate-limit.window-seconds doit etre >= 1");
        }
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public boolean tryAcquire(String clientKey) {
        Instant now = Instant.now();
        Window current = windows.compute(normalize(clientKey), (key, existing) -> {
            if (existing == null || !now.isBefore(existing.resetAt)) {
                return new Window(now.plus(window), 1);
            }
            existing.count++;
            return existing;
        });

        // Evite que la map grossisse indefiniment avec des IP ephemeres.
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().resetAt));
        }

        return current.count <= maxRequests;
    }

    public long windowSeconds() {
        return window.getSeconds();
    }

    private String normalize(String clientKey) {
        return clientKey == null ? "unknown" : clientKey.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Window {
        private final Instant resetAt;
        private int count;

        private Window(Instant resetAt, int count) {
            this.resetAt = resetAt;
            this.count = count;
        }
    }
}
