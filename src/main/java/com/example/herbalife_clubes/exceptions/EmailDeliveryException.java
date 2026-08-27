package com.example.herbalife_clubes.exceptions;

/**
 * Fallo al entregar el correo OTP (SMTP/proveedor).
 * Mensaje público fijo; la causa técnica queda solo en logs del servidor.
 */
public class EmailDeliveryException extends RuntimeException {

    public static final String ERROR_CODE = "EMAIL_DELIVERY_FAILED";
    public static final String DEFAULT_MESSAGE =
            "No pudimos enviar el código de verificación. Inténtalo nuevamente.";

    public EmailDeliveryException() {
        super(DEFAULT_MESSAGE);
    }

    public EmailDeliveryException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }
}
