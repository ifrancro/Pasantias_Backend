package com.example.herbalife_clubes.exceptions;

import com.example.herbalife_clubes.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.DisabledException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerGoogleAuthTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void googleTokenInvalidDevuelve401EstableSinDetalles() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGoogleTokenInvalid(new GoogleTokenInvalidException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(GoogleTokenInvalidException.ERROR_CODE, body.get("error"));
        assertEquals(GoogleTokenInvalidException.DEFAULT_MESSAGE, body.get("message"));
        assertBodySafe(body);
    }

    @Test
    void googleEmailNotVerifiedDevuelve400Estable() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGoogleEmailNotVerified(new GoogleEmailNotVerifiedException());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(GoogleEmailNotVerifiedException.ERROR_CODE, body.get("error"));
        assertEquals(GoogleEmailNotVerifiedException.DEFAULT_MESSAGE, body.get("message"));
        assertBodySafe(body);
    }

    @Test
    void usuarioDeshabilitadoDevuelve403() {
        ResponseEntity<ApiResponse<String>> response =
                handler.handleDisabledUser(new DisabledException("internal"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Usuario deshabilitado. Contacte al administrador.", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void errorInterno500SinDetallesNiIdToken() {
        RuntimeException boom = new RuntimeException(
                "Google JWT aud=812612197014... idToken=eyJhbGciOiJSUzI1NiJ9.secret");

        ResponseEntity<ApiResponse<String>> response =
                handler.handleGenericException(boom, new MockHttpServletRequest());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error interno del servidor", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        String serialized = String.valueOf(response.getBody()).toLowerCase();
        assertFalse(serialized.contains("eyj"));
        assertFalse(serialized.contains("idtoken"));
        assertFalse(serialized.contains("812612197014"));
        assertFalse(serialized.contains("aud="));
    }

    private static void assertBodySafe(Map<String, Object> body) {
        String serialized = String.valueOf(body).toLowerCase();
        assertFalse(serialized.contains("stack"));
        assertFalse(serialized.contains("googleidtoken"));
        assertFalse(serialized.contains("eyj"));
        assertFalse(serialized.contains("audience"));
        assertFalse(serialized.contains("client-id"));
    }
}
