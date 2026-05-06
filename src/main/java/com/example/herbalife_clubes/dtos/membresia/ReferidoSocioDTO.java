package com.example.herbalife_clubes.dtos.membresia;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ReferidoSocioDTO {
    private Integer id;
    private Integer usuarioId;
    private String usuarioNombre;
    private Integer clubId;
    private String clubNombre;
    private Integer nivelId;
    private String nivelNombre;
    private String numeroSocio;
    private Integer puntosAcumulados;
    private LocalDate fechaRegistro;
    private String estado;
}
