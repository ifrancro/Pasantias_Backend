package com.example.herbalife_clubes.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    // Un token criptográficamente válido no alcanza: el usuario tiene
                    // que estar habilitado y no bloqueado. Sin esto, el JWT emitido a
                    // una cuenta sin verificar abre toda la API.
                    if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                        logger.warn("Token de usuario deshabilitado o bloqueado; se continúa sin autenticar");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (UsernameNotFoundException ex) {
                    // Token válido de un usuario que ya no existe (p. ej. borrado de la BD).
                    // Se continúa sin autenticar: la cadena de seguridad responderá 401
                    // en vez de propagar la excepción y devolver un 500.
                    logger.warn("Token con usuario inexistente; se continúa sin autenticar");
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // ExpiredJwtException, MalformedJwtException, SignatureException, etc.
            // No autenticar y dejar que Spring Security (AuthenticationEntryPoint)
            // responda 401 en endpoints protegidos. Nunca propagar → evita HTTP 500.
            logger.warn("JWT inválido o expirado (" + ex.getClass().getSimpleName() + ")");
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
