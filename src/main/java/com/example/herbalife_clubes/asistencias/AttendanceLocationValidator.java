package com.example.herbalife_clubes.asistencias;

import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.util.GeoDistance;

import java.math.BigDecimal;

/**
 * Validación autoritativa de coordenadas y distancia para registro de asistencia.
 */
public final class AttendanceLocationValidator {

    private AttendanceLocationValidator() {
    }

    public static void validateRequestCoordinates(Double latitud, Double longitud, Double precisionMetros) {
        if (latitud == null || longitud == null) {
            AttendanceLocationRejections.throwLocationRequired(
                    "latitud y longitud son obligatorias para registrar asistencia");
        }
        validateCoordinate(latitud, -90.0, 90.0, "latitud");
        validateCoordinate(longitud, -180.0, 180.0, "longitud");
        if (precisionMetros != null) {
            validateOptionalPrecision(precisionMetros);
        }
    }

    public static void validateClubCoordinates(Club club) {
        Double clubLat = toFiniteDouble(club != null ? club.getLat() : null);
        Double clubLng = toFiniteDouble(club != null ? club.getLng() : null);
        if (clubLat == null || clubLng == null) {
            AttendanceLocationRejections.throwClubLocationUnavailable(
                    "El club no tiene ubicación configurada para validar asistencia");
        }
        if (!isInRange(clubLat, -90.0, 90.0) || !isInRange(clubLng, -180.0, 180.0)) {
            AttendanceLocationRejections.throwClubLocationUnavailable(
                    "El club no tiene ubicación configurada para validar asistencia");
        }
    }

    public static void validateWithinRange(
            double userLat, double userLng, Club club, double maxDistanceMeters) {
        double clubLat = toFiniteDouble(club.getLat());
        double clubLng = toFiniteDouble(club.getLng());
        double distance = GeoDistance.distanceMeters(userLat, userLng, clubLat, clubLng);
        if (distance > maxDistanceMeters) {
            AttendanceLocationRejections.throwOutOfRange(
                    "Debes estar cerca del club para registrar tu asistencia.",
                    maxDistanceMeters);
        }
    }

    private static void validateCoordinate(double value, double min, double max, String label) {
        if (!Double.isFinite(value)) {
            AttendanceLocationRejections.throwLocationInvalid(label + " inválida");
        }
        if (!isInRange(value, min, max)) {
            AttendanceLocationRejections.throwLocationInvalid(label + " fuera de rango permitido");
        }
    }

    private static void validateOptionalPrecision(double precisionMetros) {
        if (!Double.isFinite(precisionMetros) || precisionMetros < 0) {
            AttendanceLocationRejections.throwLocationInvalid("precisionMetros inválida");
        }
    }

    private static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    private static Double toFiniteDouble(BigDecimal value) {
        if (value == null) {
            return null;
        }
        double parsed = value.doubleValue();
        return Double.isFinite(parsed) ? parsed : null;
    }
}
