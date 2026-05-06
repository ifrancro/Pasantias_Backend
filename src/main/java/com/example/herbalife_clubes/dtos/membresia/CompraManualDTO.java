package com.example.herbalife_clubes.dtos.membresia;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CompraManualDTO {
    private Integer id;
    private Integer membresiaId;
    private Integer clubId;
    private String descripcion;
    private BigDecimal monto;
    private LocalDate fecha;
    private Integer registradaPorHostId;
}
