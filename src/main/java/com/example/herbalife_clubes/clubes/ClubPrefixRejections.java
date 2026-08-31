package com.example.herbalife_clubes.clubes;

import com.example.herbalife_clubes.exceptions.ClubPrefixRejectedException;
import org.springframework.http.HttpStatus;

/**
 * Códigos estables CLUB-PREFIX-001 para prefijo/iniciales de clubes.
 */
public final class ClubPrefixRejections {

    public static final String CLUB_PREFIX_REQUIRED = "CLUB_PREFIX_REQUIRED";
    public static final String CLUB_PREFIX_INVALID = "CLUB_PREFIX_INVALID";
    public static final String CLUB_PREFIX_CONFLICT = "CLUB_PREFIX_CONFLICT";
    public static final String CLUB_PREFIX_UNAVAILABLE = "CLUB_PREFIX_UNAVAILABLE";

    private static final String MSG_REQUIRED = "Debes indicar las iniciales del club.";
    private static final String MSG_INVALID = "Las iniciales del club deben tener exactamente 2 letras.";
    private static final String MSG_CONFLICT = "Estas iniciales ya están siendo utilizadas por otro club.";

    private ClubPrefixRejections() {
    }

    public static ClubPrefixRejectedException prefixRequired() {
        return new ClubPrefixRejectedException(CLUB_PREFIX_REQUIRED, MSG_REQUIRED, HttpStatus.BAD_REQUEST);
    }

    public static ClubPrefixRejectedException prefixInvalid() {
        return new ClubPrefixRejectedException(CLUB_PREFIX_INVALID, MSG_INVALID, HttpStatus.BAD_REQUEST);
    }

    public static ClubPrefixRejectedException prefixConflict() {
        return new ClubPrefixRejectedException(CLUB_PREFIX_CONFLICT, MSG_CONFLICT, HttpStatus.CONFLICT);
    }

    public static ClubPrefixRejectedException prefixUnavailable(String message) {
        return new ClubPrefixRejectedException(CLUB_PREFIX_UNAVAILABLE, message, HttpStatus.CONFLICT);
    }

    public static void throwPrefixRequired() {
        throw prefixRequired();
    }

    public static void throwPrefixInvalid() {
        throw prefixInvalid();
    }

    public static void throwPrefixConflict() {
        throw prefixConflict();
    }

    public static void throwPrefixUnavailable(String message) {
        throw prefixUnavailable(message);
    }
}
