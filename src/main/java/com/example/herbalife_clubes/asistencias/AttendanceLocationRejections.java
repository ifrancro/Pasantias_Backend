package com.example.herbalife_clubes.asistencias;

import com.example.herbalife_clubes.exceptions.AttendanceLocationRejectedException;
import org.springframework.http.HttpStatus;

/**
 * Códigos estables MOB-ATT-002 para validación de ubicación en registro de asistencia.
 */
public final class AttendanceLocationRejections {

    public static final String ATTENDANCE_LOCATION_REQUIRED = "ATTENDANCE_LOCATION_REQUIRED";
    public static final String ATTENDANCE_LOCATION_INVALID = "ATTENDANCE_LOCATION_INVALID";
    public static final String ATTENDANCE_CLUB_LOCATION_UNAVAILABLE = "ATTENDANCE_CLUB_LOCATION_UNAVAILABLE";
    public static final String ATTENDANCE_OUT_OF_RANGE = "ATTENDANCE_OUT_OF_RANGE";

    private AttendanceLocationRejections() {
    }

    public static AttendanceLocationRejectedException locationRequired(String message) {
        return new AttendanceLocationRejectedException(
                ATTENDANCE_LOCATION_REQUIRED, message, HttpStatus.BAD_REQUEST);
    }

    public static AttendanceLocationRejectedException locationInvalid(String message) {
        return new AttendanceLocationRejectedException(
                ATTENDANCE_LOCATION_INVALID, message, HttpStatus.BAD_REQUEST);
    }

    public static AttendanceLocationRejectedException clubLocationUnavailable(String message) {
        return new AttendanceLocationRejectedException(
                ATTENDANCE_CLUB_LOCATION_UNAVAILABLE, message, HttpStatus.CONFLICT);
    }

    public static AttendanceLocationRejectedException outOfRange(String message, double maxDistanceMeters) {
        return new AttendanceLocationRejectedException(
                ATTENDANCE_OUT_OF_RANGE, message, HttpStatus.BAD_REQUEST, maxDistanceMeters);
    }

    public static void throwLocationRequired(String message) {
        throw locationRequired(message);
    }

    public static void throwLocationInvalid(String message) {
        throw locationInvalid(message);
    }

    public static void throwClubLocationUnavailable(String message) {
        throw clubLocationUnavailable(message);
    }

    public static void throwOutOfRange(String message, double maxDistanceMeters) {
        throw outOfRange(message, maxDistanceMeters);
    }
}
