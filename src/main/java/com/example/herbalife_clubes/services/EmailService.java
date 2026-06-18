package com.example.herbalife_clubes.services;

/**
 * Servicio para el envío de correos electrónicos.
 */
public interface EmailService {

    /**
     * Envía un correo con el código de verificación OTP.
     *
     * @param to    dirección de correo del destinatario
     * @param name  nombre del usuario para personalizar el correo
     * @param code  código OTP de 6 dígitos
     */
    void sendVerificationCode(String to, String name, String code);
}
