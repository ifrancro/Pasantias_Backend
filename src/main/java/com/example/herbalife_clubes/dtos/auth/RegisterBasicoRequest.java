package com.example.herbalife_clubes.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el registro de usuarios básicos.
 * Endpoint: POST /api/auth/register-basico
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterBasicoRequest {
    private String nombre;
    private String apellido;
    private String email;
    private String password;
    private String telefono;
    private String fechaNacimiento; // Formato ISO: YYYY-MM-DD
    private String redesSociales;
}
