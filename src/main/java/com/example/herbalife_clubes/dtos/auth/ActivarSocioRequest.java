package com.example.herbalife_clubes.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para activar un socio desde el QR escaneado.
 * Endpoint: POST /api/clubes/{clubId}/socios/activar
 * Usado por: ANFITRION
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivarSocioRequest {
    /**
     * Payload del QR escaneado. Formato esperado: "ACTIVATE:{userId}"
     * También puede venir directamente el userId como Integer
     */
    private String activationPayload;
    
    /**
     * Referido por (opcional)
     */
    private String referidoPor;
    
    /**
     * Cómo conoció (opcional)
     */
    private String comoConocio;

    /**
     * Declaración legal obligatoria en activación.
     * true = SÍ (no puede activarse), false = NO (puede continuar).
     * null o ausente no es válido.
     */
    private Boolean esClientePreferenteODistribuidor;
}

