package com.example.herbalife_clubes.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Rechazo funcional de ubicación en POST /api/asistencias/registrar (MOB-ATT-002).
 */
@Getter
public class AttendanceLocationRejectedException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Double maxDistanceMeters;

    public AttendanceLocationRejectedException(String errorCode, String message, HttpStatus httpStatus) {
        this(errorCode, message, httpStatus, null);
    }

    public AttendanceLocationRejectedException(
            String errorCode, String message, HttpStatus httpStatus, Double maxDistanceMeters) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.maxDistanceMeters = maxDistanceMeters;
    }
}
