package com.example.herbalife_clubes.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de POST /api/admin/auth/login.
 * Solo se emite tras autenticación exitosa con rol ADMIN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {
    private String token;
    private Integer userId;
    private String email;
    private String nombre;
    private String apellido;
    private String rolNombre;
}
