package com.springbootlearning.learningspringboot.bookingroom.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

public class RateLimitingAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public RateLimitingAuthenticationFailureHandler(LoginAttemptService loginAttemptService) {
        super("/login?error");
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        loginAttemptService.recordFailure(request.getParameter("username"));
        super.onAuthenticationFailure(request, response, exception);
    }
}
