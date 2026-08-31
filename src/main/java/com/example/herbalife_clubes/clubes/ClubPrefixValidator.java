package com.example.herbalife_clubes.clubes;

import java.util.Locale;

/**
 * Validación centralizada de prefijo/iniciales de club (CLUB-PREFIX-001).
 */
public final class ClubPrefixValidator {

    private ClubPrefixValidator() {
    }

    public static String requireValidNormalized(String rawPrefijo) {
        validateRequired(rawPrefijo);
        return normalize(rawPrefijo);
    }

    public static void validateRequired(String rawPrefijo) {
        String normalized = normalize(rawPrefijo);
        if (normalized == null) {
            ClubPrefixRejections.throwPrefixRequired();
        }
        if (!isValidFormat(normalized)) {
            ClubPrefixRejections.throwPrefixInvalid();
        }
    }

    public static void validateStoredForApproval(String storedPrefijo) {
        if (!isValid(storedPrefijo)) {
            ClubPrefixRejections.throwPrefixUnavailable(
                    "El club debe tener iniciales válidas antes de ser aprobado.");
        }
    }

    public static void validateStoredForActivation(String storedPrefijo) {
        if (!isValid(storedPrefijo)) {
            ClubPrefixRejections.throwPrefixUnavailable(
                    "El club debe tener iniciales válidas antes de ser activado.");
        }
    }

    public static boolean isValid(String prefijo) {
        String normalized = normalize(prefijo);
        return normalized != null && isValidFormat(normalized);
    }

    public static String normalize(String rawPrefijo) {
        if (rawPrefijo == null) {
            return null;
        }
        String normalized = rawPrefijo.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static boolean isValidFormat(String normalized) {
        return normalized.matches("^[A-Z]{2}$");
    }
}
