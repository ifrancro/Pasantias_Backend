package com.example.herbalife_clubes.services;

/**
 * Recuperación de contraseña mediante OTP + token opaco de un solo uso.
 */
public interface PasswordResetService {

    String FORGOT_PASSWORD_PUBLIC_MESSAGE =
            "Si el correo está registrado, recibirás un código para restablecer tu contraseña.";

    /**
     * Solicita un código PASSWORD_RESET si el usuario está ACTIVO.
     * Siempre termina sin excepción (anti-enumeración); fallos de envío se registran
     * server-side e invalidan el OTP generado.
     */
    void requestPasswordReset(String email);

    /**
     * Valida OTP PASSWORD_RESET y emite un resetToken opaco (raw, single-use).
     */
    String verifyResetCode(String email, String code);

    /**
     * Actualiza password_hash usando resetToken válido.
     */
    void resetPassword(String resetToken, String newPassword);
}
