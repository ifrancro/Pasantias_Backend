package com.example.herbalife_clubes.dtos.logro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogroDTO {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String iconoUrl;
    private String tipoRequisito;
}

