package com.example.herbalife_clubes.exceptions;

/**
 * Credenciales válidas, pero el rol no es ADMIN.
 * Usado por POST /api/admin/auth/login para denegar sesión administrativa.
 */
public class AdminAccessDeniedException extends RuntimeException {

    public static final String ERROR_CODE = "ADMIN_ACCESS_DENIED";
    public static final String DEFAULT_MESSAGE = "Acceso administrativo no autorizado.";

    public AdminAccessDeniedException() {
        super(DEFAULT_MESSAGE);
    }

    public AdminAccessDeniedException(String message) {
        super(message);
    }
}
