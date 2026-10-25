package com.springbootlearning.learningspringboot.bookingroom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String FORM_LOGIN_PATH = "/login";
    private static final String API_LOGIN_PATH = "/api/auth/login";

    private final LoginAttemptService loginAttemptService;
    private final LoginRateLimiter loginRateLimiter;

    public LoginRateLimitFilter(LoginAttemptService loginAttemptService,
                                LoginRateLimiter loginRateLimiter) {
        this.loginAttemptService = loginAttemptService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        boolean formLogin = FORM_LOGIN_PATH.equals(path);
        boolean apiLogin = API_LOGIN_PATH.equals(path);
        if (!formLogin && !apiLogin) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!loginRateLimiter.tryAcquire(request.getRemoteAddr())) {
            rejectRateLimited(request, response, apiLogin);
            return;
        }

        if (formLogin) {
            String username = request.getParameter("username");
            if (username != null && loginAttemptService.isBlocked(username)) {
                response.sendRedirect(request.getContextPath() + "/login?locked");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void rejectRateLimited(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean apiLogin) throws IOException {
        response.setHeader("Retry-After", Long.toString(loginRateLimiter.windowSeconds()));

        if (apiLogin) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Trop de tentatives. Reessayez plus tard.\"}");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login?ratelimited");
    }
}
