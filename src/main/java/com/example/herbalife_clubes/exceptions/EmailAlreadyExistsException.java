package com.example.herbalife_clubes.exceptions;

/**
 * El correo ya existe en usuarios (UNIQUE). Distinto de otros conflictos 409.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public static final String ERROR_CODE = "EMAIL_ALREADY_EXISTS";
    public static final String DEFAULT_MESSAGE = "El correo ya está registrado.";

    public EmailAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }

    public EmailAlreadyExistsException(String message) {
        super(message);
    }

    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
