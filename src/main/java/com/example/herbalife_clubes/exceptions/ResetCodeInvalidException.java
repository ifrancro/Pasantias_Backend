package com.example.herbalife_clubes.exceptions;

/**
 * OTP de recuperación inválido, expirado, usado o bloqueado por intentos.
 */
public class ResetCodeInvalidException extends RuntimeException {

    public static final String ERROR_CODE = "RESET_CODE_INVALID";
    public static final String MESSAGE = "Código inválido o expirado.";

    public ResetCodeInvalidException() {
        super(MESSAGE);
    }
}
