package com.example.herbalife_clubes.clubes;

import java.math.BigDecimal;

/**
 * Validación centralizada de coordenadas de club (CLUB-LOCATION-001).
 */
public final class ClubLocationValidator {

    private ClubLocationValidator() {
    }

    public static void validateRequired(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            ClubLocationRejections.throwLocationRequired();
        }
        validateRange(lat, lng);
    }

    public static void validateStoredForApproval(BigDecimal lat, BigDecimal lng) {
        if (!isValid(lat, lng)) {
            ClubLocationRejections.throwLocationUnavailable(
                    "El club debe tener una ubicación válida antes de ser aprobado.");
        }
    }

    public static void validateStoredForActivation(BigDecimal lat, BigDecimal lng) {
        if (!isValid(lat, lng)) {
            ClubLocationRejections.throwLocationUnavailable(
                    "El club debe tener una ubicación válida antes de ser activado.");
        }
    }

    public static boolean isValid(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return false;
        }
        Double latValue = toFiniteDouble(lat);
        Double lngValue = toFiniteDouble(lng);
        if (latValue == null || lngValue == null) {
            return false;
        }
        return isInRange(latValue, -90.0, 90.0) && isInRange(lngValue, -180.0, 180.0);
    }

    private static void validateRange(BigDecimal lat, BigDecimal lng) {
        Double latValue = toFiniteDouble(lat);
        Double lngValue = toFiniteDouble(lng);
        if (latValue == null || lngValue == null) {
            ClubLocationRejections.throwLocationInvalid();
        }
        if (!isInRange(latValue, -90.0, 90.0) || !isInRange(lngValue, -180.0, 180.0)) {
            ClubLocationRejections.throwLocationInvalid();
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
