package com.example.herbalife_clubes.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.mail.MessagingException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerEmailDeliveryTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void emailDeliveryExceptionDevuelve503ConCodigoYMensajeSeguro() {
        EmailDeliveryException ex = new EmailDeliveryException(
                new MessagingException("Authentication failed smtp-relay.brevo.com secret=XYZ"));

        ResponseEntity<Map<String, Object>> response = handler.handleEmailDelivery(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(EmailDeliveryException.ERROR_CODE, body.get("error"));
        assertEquals(EmailDeliveryException.DEFAULT_MESSAGE, body.get("message"));

        String serialized = String.valueOf(body).toLowerCase();
        assertFalse(serialized.contains("brevo"));
        assertFalse(serialized.contains("smtp-relay"));
        assertFalse(serialized.contains("secret=xyz"));
        assertFalse(serialized.contains("authentication failed"));
    }
}
