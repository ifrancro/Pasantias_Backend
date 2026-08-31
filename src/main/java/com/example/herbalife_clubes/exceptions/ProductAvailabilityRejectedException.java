package com.example.herbalife_clubes.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Rechazo funcional de disponibilidad/precio de producto en club (PROD-AVAIL-002).
 */
@Getter
public class ProductAvailabilityRejectedException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public ProductAvailabilityRejectedException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
