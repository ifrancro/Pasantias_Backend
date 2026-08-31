package com.example.herbalife_clubes.membresias;

import java.util.Locale;

/**
 * Generación centralizada de códigos de socio legibles (MEMBER-CODE-001).
 * Formato: {PREFIJO_CLUB}-{MEMBRESIA_ID_PADDED_8}
 */
public final class MemberCodeGenerator {

    private MemberCodeGenerator() {
    }

    public static String generate(String clubPrefix, Integer membershipId) {
        String prefix = normalizePrefix(clubPrefix);
        validatePrefix(prefix);
        validateMembershipId(membershipId);
        return prefix + "-" + formatMembershipId(membershipId);
    }

    static String normalizePrefix(String clubPrefix) {
        if (clubPrefix == null) {
            return null;
        }
        String normalized = clubPrefix.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    static String formatMembershipId(Integer membershipId) {
        return String.format("%08d", membershipId);
    }

    private static void validatePrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException(
                    "El club debe tener un prefijo de socio configurado para generar el código de membresía.");
        }
        if (!prefix.matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException(
                    "El prefijo de socio debe ser exactamente 2 letras (A-Z). Recibido: '" + prefix + "'");
        }
    }

    private static void validateMembershipId(Integer membershipId) {
        if (membershipId == null || membershipId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de membresía debe ser un entero positivo.");
        }
    }
}
