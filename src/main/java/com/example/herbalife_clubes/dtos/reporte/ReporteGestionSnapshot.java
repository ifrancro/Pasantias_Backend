package com.example.herbalife_clubes.dtos.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos agregados en memoria para armar el reporte de gestión del club (sin persistencia).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteGestionSnapshot {
    private Integer clubId;
    private String nombreClub;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    /** Suma monetaria histórica de subtotales en pedidos ENTREGADOS del rango. */
    @Builder.Default
    private BigDecimal totalIngresosPuntosValor = BigDecimal.ZERO;
    /** Día → cantidad de asistencias. */
    @Builder.Default
    private Map<LocalDate, Long> asistenciasPorDia = new LinkedHashMap<>();
    /** Día → cantidad de pedidos (no cancelados). */
    @Builder.Default
    private Map<LocalDate, Long> pedidosPorDia = new LinkedHashMap<>();
    private List<ProductoVendidoRanking> rankingProductos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoVendidoRanking {
        private Integer productoId;
        private String nombreProducto;
        private long cantidadVendida;
    }
}
