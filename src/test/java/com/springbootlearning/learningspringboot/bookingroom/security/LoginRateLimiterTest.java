package com.springbootlearning.learningspringboot.bookingroom.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    @Test
    void tryAcquire_shouldAllowRequestsUpToLimit() {
        LoginRateLimiter limiter = new LoginRateLimiter(2, 60);

        assertThat(limiter.tryAcquire("127.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1")).isFalse();
    }

    @Test
    void tryAcquire_shouldTrackClientsIndependently() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, 60);

        assertThat(limiter.tryAcquire("127.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1")).isFalse();
        assertThat(limiter.tryAcquire("192.168.1.10")).isTrue();
    }

    @Test
    void tryAcquire_shouldResetAfterWindow() throws InterruptedException {
        LoginRateLimiter limiter = new LoginRateLimiter(1, 1);

        assertThat(limiter.tryAcquire("127.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1")).isFalse();

        Thread.sleep(1_100);

        assertThat(limiter.tryAcquire("127.0.0.1")).isTrue();
    }
}
