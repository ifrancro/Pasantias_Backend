package com.example.herbalife_clubes.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerAdminAccessTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noAdminDevuelve403SinTokenNiDetallesInternos() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAdminAccessDenied(new AdminAccessDeniedException());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(AdminAccessDeniedException.ERROR_CODE, body.get("error"));
        assertEquals(AdminAccessDeniedException.DEFAULT_MESSAGE, body.get("message"));
        assertFalse(body.containsKey("token"));
        assertFalse(body.containsKey("password"));
        assertFalse(body.containsKey("passwordHash"));
        assertFalse(body.toString().toLowerCase().contains("exception"));
        assertFalse(body.toString().contains("eyJ"));
    }
}
