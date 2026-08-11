package com.springbootlearning.learningspringboot.bookingroom.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void isBlocked_shouldReturnFalse_whenNoFailedAttempts() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        assertThat(service.isBlocked("admin")).isFalse();
    }

    @Test
    void isBlocked_shouldReturnFalse_whileUnderMaxAttempts() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        service.recordFailure("admin");
        service.recordFailure("admin");

        assertThat(service.isBlocked("admin")).isFalse();
    }

    @Test
    void isBlocked_shouldReturnTrue_onceMaxAttemptsReached() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        service.recordFailure("admin");
        service.recordFailure("admin");
        service.recordFailure("admin");

        assertThat(service.isBlocked("admin")).isTrue();
    }

    @Test
    void isBlocked_shouldBeCaseInsensitive_onUsername() {
        LoginAttemptService service = new LoginAttemptService(1, 15);

        service.recordFailure("Admin");

        assertThat(service.isBlocked("admin")).isTrue();
        assertThat(service.isBlocked("ADMIN")).isTrue();
    }

    @Test
    void recordSuccess_shouldClearFailedAttempts() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        service.recordFailure("admin");
        service.recordFailure("admin");
        service.recordFailure("admin");
        assertThat(service.isBlocked("admin")).isTrue();

        service.recordSuccess("admin");

        assertThat(service.isBlocked("admin")).isFalse();
    }

    @Test
    void isBlocked_shouldReturnFalse_afterLockDurationExpires() throws InterruptedException {
        LoginAttemptService service = new LoginAttemptService(1, 0);

        service.recordFailure("admin");
        Thread.sleep(20);

        assertThat(service.isBlocked("admin")).isFalse();
    }
}
