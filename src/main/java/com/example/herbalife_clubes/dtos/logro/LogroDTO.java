package com.example.herbalife_clubes.dtos.logro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogroDTO {
    private Integer id;
    private Integer clubCreadorId;
    private String clubCreadorNombre;
    private String nombre;
    private String descripcion;
    private String iconoUrl;
    private Integer puntosRecompensa;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estadoAprobacion;
    private List<RequisitoLogroDTO> requisitos = new ArrayList<>();
}
