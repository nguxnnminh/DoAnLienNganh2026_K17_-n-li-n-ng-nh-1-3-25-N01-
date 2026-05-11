package com.shop.clothingstore.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate-limits POST /api/auth/login and POST /api/auth/register per client IP.
 * Login: 10 attempts / 15-min window. Register: 5 attempts / 15-min window.
 * Returns HTTP 429 with a JSON body when the limit is exceeded.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final String LOGIN_PATH    = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String METHOD_POST   = "POST";

    // Separate key prefixes so login and register have independent buckets.
    private static final String LOGIN_KEY_PREFIX    = "login:";
    private static final String REGISTER_KEY_PREFIX = "register:";

    private final LoginRateLimiter rateLimiter;

    public LoginRateLimitFilter(LoginRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        if (METHOD_POST.equalsIgnoreCase(request.getMethod())) {
            String uri = request.getRequestURI();
            String clientIp = request.getRemoteAddr();

            if (LOGIN_PATH.equals(uri)) {
                if (!rateLimiter.isAllowed(LOGIN_KEY_PREFIX + clientIp)) {
                    log.warn("Rate limit exceeded for login | ip={}", clientIp);
                    rejectWithTooManyRequests(response, "Too many login attempts. Please try again in 15 minutes.");
                    return;
                }
            } else if (REGISTER_PATH.equals(uri)) {
                if (!rateLimiter.isAllowed(REGISTER_KEY_PREFIX + clientIp)) {
                    log.warn("Rate limit exceeded for register | ip={}", clientIp);
                    rejectWithTooManyRequests(response, "Too many registration attempts. Please try again in 15 minutes.");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private void rejectWithTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Only apply to /api/** paths to keep the filter out of the web UI chain
        return !request.getRequestURI().startsWith("/api/");
    }
}
