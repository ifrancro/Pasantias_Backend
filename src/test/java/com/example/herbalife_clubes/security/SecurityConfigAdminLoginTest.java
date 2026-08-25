package com.example.herbalife_clubes.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que solo /api/admin/auth/login sea público entre rutas admin,
 * y que el resto de /api/admin/** siga cayendo en authenticated (anyRequest).
 */
class SecurityConfigAdminLoginTest {

    @Test
    void securityConfigDeclaraSoloAdminLoginComoPublico() throws Exception {
        Path config = Path.of("src/main/java/com/example/herbalife_clubes/security/SecurityConfig.java");
        String source = Files.readString(config);

        assertTrue(source.contains("\"/api/admin/auth/login\""),
                "debe permitAll solo el login administrativo");
        assertFalse(source.contains("\"/api/admin/**\""),
                "no debe abrir /api/admin/** completo");
        assertTrue(source.contains(".anyRequest().authenticated()"),
                "el resto de rutas (incl. /api/admin/**) debe exigir autenticación");
    }
}
