package com.example.herbalife_clubes.dtos.consumo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoDTO {
    private Integer id;
    private Integer membresiaId;
    private String membresiaNumeroSocio;
    private Integer clubId;
    private String clubNombre;
    private Integer asistenciaId;
    private Integer productoId;
    private String productoNombre;
    private String descripcion;
    private Integer cantidad;
    private BigDecimal precioRegistrado;
    private LocalDateTime fechaHora;
    private LocalDateTime createdAt;
}

