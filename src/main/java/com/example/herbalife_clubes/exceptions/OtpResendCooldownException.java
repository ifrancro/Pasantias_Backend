package com.example.herbalife_clubes.exceptions;

/**
 * Reenvío de OTP EMAIL_VERIFICATION solicitado antes de completar el cooldown.
 */
public class OtpResendCooldownException extends RuntimeException {

    public static final String ERROR_CODE = "OTP_RESEND_COOLDOWN";
    public static final String MESSAGE = "Espera unos segundos antes de solicitar otro código.";

    private final int retryAfterSeconds;

    public OtpResendCooldownException(int retryAfterSeconds) {
        super(MESSAGE);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
