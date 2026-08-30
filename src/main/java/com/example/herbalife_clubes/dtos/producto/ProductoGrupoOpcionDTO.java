package com.example.herbalife_clubes.dtos.producto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoGrupoOpcionDTO {
    private Integer id;
    private String nombre;
    private Integer orden;
    private Integer minSelecciones;
    private Integer maxSelecciones;
    private Boolean permiteRepetir;
    private List<ProductoOpcionDTO> opciones = new ArrayList<>();
}
