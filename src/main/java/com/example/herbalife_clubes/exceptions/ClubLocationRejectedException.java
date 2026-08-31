package com.example.herbalife_clubes.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Rechazo funcional de ubicación de club (CLUB-LOCATION-001).
 */
@Getter
public class ClubLocationRejectedException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public ClubLocationRejectedException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
