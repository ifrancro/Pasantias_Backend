package com.example.herbalife_clubes.dtos.qr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QRValidacionResponse {
    private Integer membresiaId;
    private String numeroSocio;
    private String nombreCompleto;
    private String estado;
    private String nivelNombre;
    private Integer rachaActual;
    private Integer rachaMaxima;
    private String mensaje;
    private Boolean valido;
}

