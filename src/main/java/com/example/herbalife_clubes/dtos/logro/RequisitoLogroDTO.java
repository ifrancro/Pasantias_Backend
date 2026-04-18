package com.example.herbalife_clubes.dtos.logro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequisitoLogroDTO {
    private Integer id;
    private String tipoMetrica;
    private Integer cantidadEsperada;
}
