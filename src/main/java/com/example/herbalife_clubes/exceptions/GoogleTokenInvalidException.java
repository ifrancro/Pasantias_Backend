package com.example.herbalife_clubes.exceptions;

/**
 * idToken de Google inválido, expirado, con audience incorrecto o firma inválida.
 */
public class GoogleTokenInvalidException extends RuntimeException {

    public static final String ERROR_CODE = "GOOGLE_TOKEN_INVALID";
    public static final String DEFAULT_MESSAGE = "No se pudo validar la cuenta de Google.";

    public GoogleTokenInvalidException() {
        super(DEFAULT_MESSAGE);
    }

    public GoogleTokenInvalidException(String message) {
        super(message);
    }

    public GoogleTokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
