package com.example.herbalife_clubes.dtos.producto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoOpcionDTO {
    private Integer id;
    private String nombre;
    private Integer orden;
    private Boolean activo;
}
