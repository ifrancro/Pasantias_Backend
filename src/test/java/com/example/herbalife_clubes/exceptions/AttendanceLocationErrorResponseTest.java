package com.example.herbalife_clubes.exceptions;

import com.example.herbalife_clubes.asistencias.AttendanceLocationRejections;
import com.example.herbalife_clubes.asistencias.AttendanceLocationValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceLocationErrorResponseTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlerLocationRequired400() {
        assertHandler(
                AttendanceLocationRejections.locationRequired("latitud y longitud son obligatorias"),
                HttpStatus.BAD_REQUEST,
                AttendanceLocationRejections.ATTENDANCE_LOCATION_REQUIRED);
    }

    @Test
    void handlerLocationInvalid400() {
        assertHandler(
                AttendanceLocationRejections.locationInvalid("latitud fuera de rango"),
                HttpStatus.BAD_REQUEST,
                AttendanceLocationRejections.ATTENDANCE_LOCATION_INVALID);
    }

    @Test
    void handlerClubLocationUnavailable409() {
        assertHandler(
                AttendanceLocationRejections.clubLocationUnavailable("sin ubicación"),
                HttpStatus.CONFLICT,
                AttendanceLocationRejections.ATTENDANCE_CLUB_LOCATION_UNAVAILABLE);
    }

    @Test
    void handlerOutOfRangeIncluyeMaxDistance() {
        AttendanceLocationRejectedException ex =
                AttendanceLocationRejections.outOfRange("lejos", 100.0);
        ResponseEntity<Map<String, Object>> response = handler.handleAttendanceLocationRejected(ex);
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(AttendanceLocationRejections.ATTENDANCE_OUT_OF_RANGE, body.get("error"));
        assertEquals(100.0, body.get("maxDistanceMeters"));
    }

    @Test
    void latitudMayor90Invalida() {
        assertValidatorRejects(
                () -> AttendanceLocationValidator.validateRequestCoordinates(91.0, -63.0, null),
                AttendanceLocationRejections.ATTENDANCE_LOCATION_INVALID);
    }

    @Test
    void longitudMayor180Invalida() {
        assertValidatorRejects(
                () -> AttendanceLocationValidator.validateRequestCoordinates(-17.0, 181.0, null),
                AttendanceLocationRejections.ATTENDANCE_LOCATION_INVALID);
    }

    @Test
    void nanInvalido() {
        assertValidatorRejects(
                () -> AttendanceLocationValidator.validateRequestCoordinates(Double.NaN, -63.0, null),
                AttendanceLocationRejections.ATTENDANCE_LOCATION_INVALID);
    }

    private void assertHandler(AttendanceLocationRejectedException ex, HttpStatus status, String code) {
        ResponseEntity<Map<String, Object>> response = handler.handleAttendanceLocationRejected(ex);
        assertEquals(status, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(code, body.get("error"));
        assertEquals(ex.getMessage(), body.get("message"));
    }

    private void assertValidatorRejects(org.junit.jupiter.api.function.Executable exec, String code) {
        AttendanceLocationRejectedException ex =
                assertThrows(AttendanceLocationRejectedException.class, exec);
        assertEquals(code, ex.getErrorCode());
    }
}
