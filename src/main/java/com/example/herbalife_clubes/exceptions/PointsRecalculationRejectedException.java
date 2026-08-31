package com.example.herbalife_clubes.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Rechazo funcional del recálculo de puntos por asistencias (POINTS-ORDER-001).
 */
@Getter
public class PointsRecalculationRejectedException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public PointsRecalculationRejectedException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
