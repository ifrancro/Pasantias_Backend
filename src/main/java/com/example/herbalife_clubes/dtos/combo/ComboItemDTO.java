package com.example.herbalife_clubes.dtos.combo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboItemDTO {
    private Integer id;
    private Integer productoId;
    private String productoNombre;
    private String productoImagenUrl;
    private Integer puntosValorProducto;
    private Integer saborId;
    private String saborNombre;
    private Integer cantidad;
}
