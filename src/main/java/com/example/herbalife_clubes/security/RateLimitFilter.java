package com.example.herbalife_clubes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 30;
    private static final long WINDOW_MS = 60_000;
    private static final long CLEANUP_INTERVAL_MS = 300_000;
    private static final String BUSCAR_PATH = "/api/membresias/buscar";

    private final ConcurrentHashMap<String, List<Long>> requestTimestamps = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        if (!BUSCAR_PATH.equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();
        boolean shouldCleanupAll = now - lastCleanup > CLEANUP_INTERVAL_MS;

        List<Long> timestamps = requestTimestamps.computeIfAbsent(clientIp, k -> new ArrayList<>());

        synchronized (timestamps) {
            timestamps.removeIf(t -> now - t > WINDOW_MS);

            if (timestamps.size() >= MAX_REQUESTS) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"Demasiadas solicitudes. Intente de nuevo en 1 minuto.\"}");
                return;
            }

            timestamps.add(now);
        }

        if (timestamps.isEmpty()) {
            requestTimestamps.remove(clientIp);
        }

        if (shouldCleanupAll) {
            lastCleanup = now;
            cleanupStaleEntries(now);
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupStaleEntries(long now) {
        Iterator<Map.Entry<String, List<Long>>> it = requestTimestamps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<Long>> entry = it.next();
            List<Long> stamps = entry.getValue();
            synchronized (stamps) {
                stamps.removeIf(t -> now - t > WINDOW_MS);
                if (stamps.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
