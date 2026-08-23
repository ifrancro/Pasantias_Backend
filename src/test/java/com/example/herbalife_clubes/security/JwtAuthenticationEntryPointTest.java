package com.example.herbalife_clubes.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationEntryPointTest {

    @Test
    void commenceResponde401JsonSinDetalleInterno() throws Exception {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("Full authentication is required")
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertEquals("{\"success\":false,\"message\":\"No autenticado\"}", response.getContentAsString());
        assertFalse(response.getContentAsString().contains("InsufficientAuthentication"));
        assertFalse(response.getContentAsString().contains("stack"));
    }
}
