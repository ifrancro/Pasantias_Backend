package com.example.herbalife_clubes.exceptions;

/**
 * Credenciales correctas, pero el correo aún no fue verificado (OTP pendiente).
 * Distinto de {@link org.springframework.security.authentication.DisabledException}
 * (cuenta deshabilitada) y de credenciales inválidas.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public static final String ERROR_CODE = "EMAIL_NOT_VERIFIED";

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
