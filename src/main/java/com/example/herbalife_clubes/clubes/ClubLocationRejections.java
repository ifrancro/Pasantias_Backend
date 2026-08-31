package com.example.herbalife_clubes.clubes;

import com.example.herbalife_clubes.exceptions.ClubLocationRejectedException;
import org.springframework.http.HttpStatus;

/**
 * Códigos estables CLUB-LOCATION-001 para ubicación de clubes.
 */
public final class ClubLocationRejections {

    public static final String CLUB_LOCATION_REQUIRED = "CLUB_LOCATION_REQUIRED";
    public static final String CLUB_LOCATION_INVALID = "CLUB_LOCATION_INVALID";
    public static final String CLUB_LOCATION_UNAVAILABLE = "CLUB_LOCATION_UNAVAILABLE";

    private static final String MSG_REQUIRED = "Debes indicar la ubicación del club.";
    private static final String MSG_INVALID = "La ubicación del club no es válida.";

    private ClubLocationRejections() {
    }

    public static ClubLocationRejectedException locationRequired() {
        return locationRequired(MSG_REQUIRED);
    }

    public static ClubLocationRejectedException locationRequired(String message) {
        return new ClubLocationRejectedException(CLUB_LOCATION_REQUIRED, message, HttpStatus.BAD_REQUEST);
    }

    public static ClubLocationRejectedException locationInvalid() {
        return locationInvalid(MSG_INVALID);
    }

    public static ClubLocationRejectedException locationInvalid(String message) {
        return new ClubLocationRejectedException(CLUB_LOCATION_INVALID, message, HttpStatus.BAD_REQUEST);
    }

    public static ClubLocationRejectedException locationUnavailable(String message) {
        return new ClubLocationRejectedException(CLUB_LOCATION_UNAVAILABLE, message, HttpStatus.CONFLICT);
    }

    public static void throwLocationRequired() {
        throw locationRequired();
    }

    public static void throwLocationInvalid() {
        throw locationInvalid();
    }

    public static void throwLocationUnavailable(String message) {
        throw locationUnavailable(message);
    }
}
