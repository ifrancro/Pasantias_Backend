package com.example.herbalife_clubes.dtos.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVentaDiariaDTO {
    private Integer productoId;
    private String nombre;
    private Integer cantidad;
    private Boolean esCombo;
    private BigDecimal subtotal;
}
