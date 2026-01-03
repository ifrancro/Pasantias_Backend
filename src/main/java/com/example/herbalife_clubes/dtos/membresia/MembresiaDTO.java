package com.example.herbalife_clubes.dtos.membresia;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembresiaDTO {
    private Integer id;
    private Integer usuarioId;
    private String usuarioNombre;
    private Integer clubId;
    private String clubNombre;
    private Integer nivelId;
    private String nivelNombre;
    private String numeroSocio;
    private Integer puntosAcumulados;
    private String referidoPor;
    private String comoConocio;
    private LocalDateTime fechaRegistro;
    private String estado;
}

