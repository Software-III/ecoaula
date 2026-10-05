package com.ecoaula.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de limitación de tasa (Rate Limiting) por dirección IP para endpoints públicos (ADR-06).
 * Previene ataques de fuerza bruta o adivinación sobre los códigos de verificación de certificados.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final int maxPermitsPerMinute;
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${ecoaula.seguridad.rate-limit-permits:30}") int maxPermitsPerMinute) {
        this.maxPermitsPerMinute = maxPermitsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Solo aplica a endpoints de verificación pública de certificados
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/certificados");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = obtenerIpCliente(request);
        long currentMinute = System.currentTimeMillis() / 60000;

        RequestCounter counter = requestCounts.compute(clientIp, (ip, existingCounter) -> {
            if (existingCounter == null || existingCounter.minuteTimestamp != currentMinute) {
                return new RequestCounter(currentMinute, new AtomicInteger(1));
            }
            existingCounter.count.incrementAndGet();
            return existingCounter;
        });

        if (counter.count.get() > maxPermitsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Límite de solicitudes superado. Intente de nuevo más tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        final long minuteTimestamp;
        final AtomicInteger count;

        RequestCounter(long minuteTimestamp, AtomicInteger count) {
            this.minuteTimestamp = minuteTimestamp;
            this.count = count;
        }
    }
}
