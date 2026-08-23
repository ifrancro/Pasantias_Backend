package com.example.herbalife_clubes.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerEmailConflictTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void emailAlreadyExistsDevuelve409Estable() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleEmailAlreadyExists(new EmailAlreadyExistsException());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(EmailAlreadyExistsException.ERROR_CODE, body.get("error"));
        assertEquals(EmailAlreadyExistsException.DEFAULT_MESSAGE, body.get("message"));
        assertBodyDoesNotLeakInternals(body);
    }

    @Test
    void dataIntegrityEmailUniqueFallbackSeguroSinSql() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "could not execute statement [insert into usuarios ...]; "
                        + "ERROR: duplicate key value violates unique constraint \"usuarios_email_key\" "
                        + "Detail: Key (email)=(dup@test.com) already exists.; "
                        + "SQL [n/a]; constraint [usuarios_email_key]; "
                        + "nested exception is org.hibernate.exception.ConstraintViolationException");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrity(dive);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(EmailAlreadyExistsException.ERROR_CODE, body.get("error"));
        assertEquals(EmailAlreadyExistsException.DEFAULT_MESSAGE, body.get("message"));
        assertBodyDoesNotLeakInternals(body);

        String serialized = body.toString().toLowerCase();
        assertFalse(serialized.contains("usuarios_email_key"));
        assertFalse(serialized.contains("duplicate key"));
        assertFalse(serialized.contains("hibernate"));
        assertFalse(serialized.contains("sql ["));
        assertFalse(serialized.contains("insert into"));
    }

    @Test
    void dataIntegrityOtroConflictoNoFiltraDetallesInternos() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"otra_tabla_uk\"");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrity(dive);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("CONFLICT", body.get("error"));
        assertBodyDoesNotLeakInternals(body);
        assertFalse(body.toString().toLowerCase().contains("otra_tabla_uk"));
    }

    private static void assertBodyDoesNotLeakInternals(Map<String, Object> body) {
        String serialized = String.valueOf(body).toLowerCase();
        assertFalse(serialized.contains("constraint"));
        assertFalse(serialized.contains("postgresql"));
        assertFalse(serialized.contains("org.hibernate"));
        assertFalse(serialized.contains("stack"));
        assertFalse(serialized.contains("psql"));
    }
}
