package com.ecoaula.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    @Test
    @DisplayName("Debe permitir solicitudes dentro del límite configurado")
    void debePermitirSolicitudesDentroDelLimite() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter(5);
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/certificados/EA-1234/verificacion");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Debe responder 429 Too Many Requests cuando se excede el límite por IP")
    void debeBloquearCuandoExcedeLimite() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter(2);
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/certificados/EA-1234/verificacion");
        request.setRemoteAddr("192.168.1.99");

        // 1ª petición (permitida)
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(request, res1, filterChain);
        assertEquals(200, res1.getStatus());

        // 2ª petición (permitida)
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(request, res2, filterChain);
        assertEquals(200, res2.getStatus());

        // 3ª petición (bloqueada - 429)
        MockHttpServletResponse res3 = new MockHttpServletResponse();
        filter.doFilter(request, res3, filterChain);
        assertEquals(429, res3.getStatus());
    }
}
