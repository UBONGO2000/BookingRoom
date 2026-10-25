package com.springbootlearning.learningspringboot.bookingroom.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitFilterTest {

    @Test
    void formLogin_shouldRedirectOnceRateLimitIsReached() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(
                new LoginAttemptService(5, 15), new LoginRateLimiter(1, 60));

        MockHttpServletResponse first = post(filter, "/login", "10.0.0.1", "user", "wrong");
        MockHttpServletResponse second = post(filter, "/login", "10.0.0.1", "user", "wrong");

        assertThat(first.getRedirectedUrl()).isNull();
        assertThat(second.getStatus()).isEqualTo(302);
        assertThat(second.getRedirectedUrl()).isEqualTo("/login?ratelimited");
        assertThat(second.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void apiLogin_shouldReturn429OnceRateLimitIsReached() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(
                new LoginAttemptService(5, 15), new LoginRateLimiter(1, 30));

        post(filter, "/api/auth/login", "10.0.0.2", null, null);
        MockHttpServletResponse second = post(filter, "/api/auth/login", "10.0.0.2", null, null);

        assertThat(second.getStatus()).isEqualTo(429);
        assertThat(second.getHeader("Retry-After")).isEqualTo("30");
        assertThat(second.getContentAsString()).contains("Trop de tentatives");
    }

    @Test
    void formLogin_shouldRedirectToLocked_whenAccountIsLocked() throws Exception {
        LoginAttemptService attempts = new LoginAttemptService(2, 15);
        attempts.recordFailure("admin");
        attempts.recordFailure("admin");
        LoginRateLimitFilter filter = new LoginRateLimitFilter(attempts, new LoginRateLimiter(10, 60));

        MockHttpServletResponse response = post(filter, "/login", "10.0.0.3", "admin", "wrong");

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?locked");
    }

    @Test
    void otherPosts_shouldNotBeRateLimited() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(
                new LoginAttemptService(5, 15), new LoginRateLimiter(1, 60));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/booking");
        request.setServletPath("/booking");
        request.setRemoteAddr("10.0.0.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getRedirectedUrl()).isNull();
    }

    private MockHttpServletResponse post(LoginRateLimitFilter filter,
                                         String path,
                                         String remoteAddress,
                                         String username,
                                         String password) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddress);
        if (username != null) {
            request.setParameter("username", username);
        }
        if (password != null) {
            request.setParameter("password", password);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
