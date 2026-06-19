package com.example.herbalife_clubes.dtos.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenDiaVentasDTO {
    private LocalDate fecha;
    private long totalVentas;
    @Builder.Default
    private BigDecimal totalIngresosBs = BigDecimal.ZERO;
    @Builder.Default
    private Map<String, BigDecimal> ingresosPorTipoPago = new LinkedHashMap<>();
    private long conteoNuevos;
    private long conteoReferidos;
    @Builder.Default
    private List<RankingProductoDiaDTO> rankingProductos = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankingProductoDiaDTO {
        private String nombre;
        private long cantidad;
    }
}
