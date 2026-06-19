package com.example.herbalife_clubes.dtos.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMensualVentasDTO {
    private Integer clubId;
    private String nombreClub;
    private int anio;
    private int mes;
    private String nombreMes;
    private ResumenMesKpiDTO resumen;
    @Builder.Default
    private List<VentasPorDiaMesDTO> ventasPorDia = new ArrayList<>();
    @Builder.Default
    private List<TopProductoMesDTO> topProductos = new ArrayList<>();
}
