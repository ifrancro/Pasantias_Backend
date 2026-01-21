package com.example.herbalife_clubes.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta del registro de usuario básico.
 * Incluye el payload del QR de activación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterBasicoResponse {
    private String token;
    private Integer userId;
    private String email;
    private String nombre;
    private String apellido;
    private String rolNombre;
    private String qrActivacionPayload; // Formato: "ACTIVATE:{userId}"
}

