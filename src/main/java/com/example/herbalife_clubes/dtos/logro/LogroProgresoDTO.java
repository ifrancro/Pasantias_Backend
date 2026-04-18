package com.example.herbalife_clubes.dtos.logro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogroProgresoDTO {
    private Integer logroId;
    private String nombre;
    private List<RequisitoProgresoDTO> requisitos = new ArrayList<>();
    private boolean completado;
    private Integer puntosRecompensa;
}
