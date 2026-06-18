package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.entities.Usuario;

/**
 * Servicio para la verificación de correo electrónico mediante códigos OTP.
 */
public interface VerificationService {

    /**
     * Genera un código OTP, lo almacena en la BD y lo envía por correo.
     *
     * @param usuario el usuario que necesita verificar su correo
     */
    void generateAndSendCode(Usuario usuario);

    /**
     * Verifica un código OTP para un email dado.
     * Si es válido, actualiza el estado del usuario a ACTIVO.
     *
     * @param email el correo del usuario
     * @param code  el código OTP ingresado
     * @return true si la verificación fue exitosa
     */
    boolean verifyCode(String email, String code);

    /**
     * Reenvía un nuevo código de verificación al email dado.
     * Invalida cualquier código anterior.
     *
     * @param email el correo del usuario
     * @throws RuntimeException si se excede el límite de reenvíos
     */
    void resendCode(String email);
}
