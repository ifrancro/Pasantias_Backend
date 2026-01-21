package com.example.herbalife_clubes.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta para endpoints de QR.
 * Puede contener QR de activación o QR definitivo de socio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrResponse {
    /**
     * Tipo de QR: "ACTIVACION" o "SOCIO"
     */
    private String tipo;
    
    /**
     * Payload del QR (formato: "ACTIVATE:{userId}" o "SOCIO:{numeroSocio}")
     */
    private String qrPayload;
    
    /**
     * Datos adicionales según el tipo
     */
    private Integer numeroSocio;
    private Integer clubId;
    private String clubNombre;
    private Integer hubId;
}

