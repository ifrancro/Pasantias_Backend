package com.example.herbalife_clubes.dtos.producto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private Integer id;
    private Integer clubId;
    private String clubNombre;
    private String nombre;
    private String descripcion;
    private BigDecimal precioReferencial;
    private Boolean activo;
    private LocalDateTime createdAt;
}

