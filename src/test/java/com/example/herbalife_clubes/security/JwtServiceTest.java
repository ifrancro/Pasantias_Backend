package com.example.herbalife_clubes.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void secretDe63BytesFallaEnInicializacion() {
        JwtService service = new JwtService();
        String secret = base64OfLength(63);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.applySecret(secret));

        assertEquals(
                "JWT_SECRET debe contener al menos 64 bytes (512 bits) para HS512.",
                ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase().contains("base64"));
        assertFalse(ex.getMessage().contains(secret));
    }

    @Test
    void secretDe64BytesInicializaOk() {
        JwtService service = new JwtService();
        assertDoesNotThrow(() -> service.applySecret(base64OfLength(64)));
    }

    @Test
    void secretMayorA64BytesInicializaOk() {
        JwtService service = new JwtService();
        assertDoesNotThrow(() -> service.applySecret(base64OfLength(80)));
    }

    @Test
    void secretBase64InvalidoFallaDeFormaSegura() {
        JwtService service = new JwtService();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.applySecret("!!!not-valid-base64!!!"));

        assertEquals("JWT_SECRET debe ser Base64 válido.", ex.getMessage());
        assertFalse(ex.getMessage().contains("!!!"));
    }

    @Test
    void secretAusenteFalla() {
        JwtService service = new JwtService();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.applySecret(null));

        assertTrue(ex.getMessage().contains("JWT_SECRET no encontrada"));
    }

    @Test
    void secretEnBlancoFalla() {
        JwtService service = new JwtService();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.applySecret("   "));

        assertTrue(ex.getMessage().contains("JWT_SECRET no encontrada"));
    }

    @Test
    void generateTokenConSecretValidoFirmaHs512YSePuedeValidar() {
        JwtService service = new JwtService();
        service.applySecret(base64OfLength(64));

        UserDetails user = User.builder()
                .username("activo@test.com")
                .password("n/a")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USUARIO_BASICO")))
                .build();

        String token = service.generateToken(user);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
        assertEquals("activo@test.com", service.extractUsername(token));
        assertTrue(service.isTokenValid(token, user));

        // Header HS512 (alg) sin asumir secret en mensaje de error
        String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        assertTrue(headerJson.contains("HS512"));
    }

    /** Bytes ficticios de test; el contenido no es un secret de producción. */
    private static String base64OfLength(int byteLength) {
        byte[] bytes = new byte[byteLength];
        for (int i = 0; i < byteLength; i++) {
            bytes[i] = (byte) (i + 1);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}
