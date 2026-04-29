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
 * Intercepts POST /api/auth/login and enforces rate limiting per client IP.
 * Returns HTTP 429 with a JSON body when the limit is exceeded.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String METHOD_POST = "POST";

    private final LoginRateLimiter rateLimiter;

    public LoginRateLimitFilter(LoginRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        if (METHOD_POST.equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI())) {

            String clientIp = resolveClientIp(request);

            if (!rateLimiter.isAllowed(clientIp)) {
                log.warn("Rate limit exceeded for login | ip={}", clientIp);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\","
                        + "\"message\":\"Too many login attempts. Please try again in 15 minutes.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Only apply to /api/** paths to keep the filter out of the web UI chain
        return !request.getRequestURI().startsWith("/api/");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first (leftmost) IP — that's the originating client
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
