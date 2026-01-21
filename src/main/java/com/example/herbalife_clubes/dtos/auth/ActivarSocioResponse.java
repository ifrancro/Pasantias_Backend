package com.example.herbalife_clubes.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de la activación de socio.
 * Incluye datos de la membresía creada y el QR definitivo del socio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivarSocioResponse {
    private Integer membresiaId;
    private String numeroSocio;
    private Integer clubId;
    private String clubNombre;
    private Integer usuarioId;
    private String usuarioNombre;
    private String usuarioApellido;
    private String qrSocioPayload; // Formato: "SOCIO:{numeroSocio}"
}

