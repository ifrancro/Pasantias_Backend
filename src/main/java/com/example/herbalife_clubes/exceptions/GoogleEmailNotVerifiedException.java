package com.example.herbalife_clubes.exceptions;

/**
 * La cuenta de Google no tiene el correo verificado (email_verified != true).
 */
public class GoogleEmailNotVerifiedException extends RuntimeException {

    public static final String ERROR_CODE = "GOOGLE_EMAIL_NOT_VERIFIED";
    public static final String DEFAULT_MESSAGE =
            "El correo de la cuenta de Google no está verificado.";

    public GoogleEmailNotVerifiedException() {
        super(DEFAULT_MESSAGE);
    }

    public GoogleEmailNotVerifiedException(String message) {
        super(message);
    }
}
