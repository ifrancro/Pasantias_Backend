package com.example.herbalife_clubes.dtos.logro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequisitoProgresoDTO {
    private String tipoMetrica;
    private Integer cantidadEsperada;
    private Integer cantidadActual;
}
