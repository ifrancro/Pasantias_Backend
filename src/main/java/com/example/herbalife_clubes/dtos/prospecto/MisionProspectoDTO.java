package com.example.herbalife_clubes.dtos.prospecto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MisionProspectoDTO {
    private Integer id;
    private Integer prospectoId;
    private String nombre;
    private String descripcion;
    private Integer metaCantidad;
    private Integer progresoActual;
    private LocalDate fechaLimite;
    private Boolean completada;
}
