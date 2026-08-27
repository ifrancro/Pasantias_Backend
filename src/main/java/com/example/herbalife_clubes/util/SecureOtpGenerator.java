package com.example.herbalife_clubes.util;

import java.security.SecureRandom;

/**
 * Generación de códigos OTP numéricos.
 */
public final class SecureOtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecureOtpGenerator() {
    }

    public static String generateNumericCode(int codeLength) {
        int max = (int) Math.pow(10, codeLength);
        int code = SECURE_RANDOM.nextInt(max);
        return String.format("%0" + codeLength + "d", code);
    }

    public static String generateUrlSafeToken(int numBytes) {
        byte[] raw = new byte[numBytes];
        SECURE_RANDOM.nextBytes(raw);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
