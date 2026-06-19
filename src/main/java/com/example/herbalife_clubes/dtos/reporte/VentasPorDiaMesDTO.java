package com.example.herbalife_clubes.dtos.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentasPorDiaMesDTO {
    private LocalDate fecha;
    private long totalVentas;
    @Builder.Default
    private BigDecimal totalIngresosBs = BigDecimal.ZERO;
}
