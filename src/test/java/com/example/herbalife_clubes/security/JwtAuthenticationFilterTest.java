package com.example.herbalife_clubes.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AUTH-001: JWT expirado/inválido no debe producir HTTP 500.
 * El filtro captura JwtException y deja la petición sin autenticar;
 * Spring Security + JwtAuthenticationEntryPoint responden 401.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;
    private JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        entryPoint = new JwtAuthenticationEntryPoint();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authMeConJwtValidoAutentica() throws Exception {
        UserDetails user = User.withUsername("activo@test.com")
                .password("x")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USUARIO_BASICO")))
                .build();
        when(jwtService.extractUsername("valid.jwt")).thenReturn("activo@test.com");
        when(userDetailsService.loadUserByUsername("activo@test.com")).thenReturn(user);
        when(jwtService.isTokenValid("valid.jwt", user)).thenReturn(true);

        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("activo@test.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
        assertNotEquals(500, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    @Test
    void authMeConJwtExpiradoResponde401No500() throws Exception {
        when(jwtService.extractUsername(anyString()))
                .thenThrow(new ExpiredJwtException(null, null, "JWT expired"));

        assertEquals(401, statusAfterFilterAndAuthz("expired.jwt.token"));
    }

    @Test
    void authMeConJwtMalformadoResponde401() throws Exception {
        when(jwtService.extractUsername(anyString()))
                .thenThrow(new MalformedJwtException("Malformed JWT"));

        assertEquals(401, statusAfterFilterAndAuthz("not-a-jwt"));
    }

    @Test
    void authMeConFirmaInvalidaResponde401() throws Exception {
        when(jwtService.extractUsername(anyString()))
                .thenThrow(new SignatureException("Invalid signature"));

        assertEquals(401, statusAfterFilterAndAuthz("bad.signature.jwt"));
    }

    @Test
    void authMeSinTokenResponde401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                entryPoint.commence(
                        (HttpServletRequest) req,
                        (HttpServletResponse) res,
                        new InsufficientAuthenticationException("Full authentication is required")
                );
            }
        });

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("No autenticado"));
        assertFalse(response.getContentAsString().toLowerCase().contains("exception"));
        assertFalse(response.getContentAsString().contains("eyJ"));
        verifyNoInteractions(jwtService);
    }

    @Test
    void jwtExpiradoNoPropagaExcepcionNiAutentica() throws Exception {
        when(jwtService.extractUsername(anyString()))
                .thenThrow(new ExpiredJwtException(null, null, "JWT expired"));

        MockHttpServletRequest request = requestWithBearer("expired.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertNotEquals(500, response.getStatus());
    }

    private int statusAfterFilterAndAuthz(String jwt) throws Exception {
        MockHttpServletRequest request = requestWithBearer(jwt);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simula AuthorizationFilter + ExceptionTranslationFilter en endpoint protegido.
        filter.doFilter(request, response, (req, res) -> {
            HttpServletResponse httpRes = (HttpServletResponse) res;
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                entryPoint.commence(
                        (HttpServletRequest) req,
                        httpRes,
                        new InsufficientAuthenticationException("Full authentication is required")
                );
            } else {
                httpRes.setStatus(HttpServletResponse.SC_OK);
                httpRes.getWriter().write("{\"ok\":true}");
            }
        });

        String body = response.getContentAsString();
        assertFalse(body.toLowerCase().contains("exception"), "body no debe filtrar stack/excepción");
        assertFalse(body.contains("eyJ"), "body no debe incluir JWT");
        return response.getStatus();
    }

    private static MockHttpServletRequest requestWithBearer(String jwt) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + jwt);
        return request;
    }
}
