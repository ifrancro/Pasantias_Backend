package com.example.herbalife_clubes.exceptions;

/**
 * Token de reset inválido, expirado o ya utilizado.
 */
public class ResetTokenInvalidException extends RuntimeException {

    public static final String ERROR_CODE = "RESET_TOKEN_INVALID";
    public static final String MESSAGE =
            "El enlace de recuperación es inválido o expiró. Solicita un nuevo código.";

    public ResetTokenInvalidException() {
        super(MESSAGE);
    }
}
