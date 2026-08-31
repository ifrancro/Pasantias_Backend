package com.example.herbalife_clubes.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Rechazo funcional de prefijo/iniciales de club (CLUB-PREFIX-001).
 */
@Getter
public class ClubPrefixRejectedException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public ClubPrefixRejectedException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
