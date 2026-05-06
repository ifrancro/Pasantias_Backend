package com.example.herbalife_clubes.dtos.prospecto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProspectoDTO {
    private Integer id;
    private Integer clubId;
    private String nombre;
    private String telefono;
    private Integer referidoPorMembresiaId;
    private String referidoPorNombre;
    private LocalDate fechaCreacion;
    private String estado;
    private List<MisionProspectoDTO> misiones;
}
