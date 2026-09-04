package com.akhil.social.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter suitable for a single Render instance.
 * For horizontal scaling, replace with Redis-backed limiting.
 */
@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.login:10}")
    private int loginLimit;
    @Value("${app.rate-limit.ai:20}")
    private int aiLimit;
    @Value("${app.rate-limit.write:60}")
    private int writeLimit;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();
        String method = req.getMethod();

        int limit = 0;
        if ("POST".equals(method)) {
            if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
                limit = loginLimit;
            } else if (path.startsWith("/api/ai/")) {
                limit = aiLimit;
            } else if (path.startsWith("/api/messages") || path.startsWith("/api/posts")
                    || path.startsWith("/api/comments")) {
                limit = writeLimit;
            }
        }

        if (limit > 0) {
            String key = clientKey(req) + ":" + path;
            Window w = windows.computeIfAbsent(key, k -> new Window());
            long now = Instant.now().getEpochSecond();
            synchronized (w) {
                if (now - w.windowStart >= 60) {
                    w.windowStart = now;
                    w.count.set(0);
                }
                if (w.count.incrementAndGet() > limit) {
                    res.setStatus(429);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Try again later.\",\"timestamp\":\"" + Instant.now() + "\",\"path\":\"" + path + "\"}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
    }

    static class Window {
        long windowStart = Instant.now().getEpochSecond();
        AtomicInteger count = new AtomicInteger(0);
    }
}
