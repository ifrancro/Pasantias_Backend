package com.example.herbalife_clubes.dtos.nivelsocio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NivelSocioDTO {
    private Integer id;
    private String nombre;
    private Integer visitasRequeridas;
    private String descripcionBeneficios;
}

