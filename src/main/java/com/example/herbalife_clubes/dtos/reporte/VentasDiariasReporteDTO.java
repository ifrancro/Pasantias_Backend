package com.example.herbalife_clubes.dtos.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentasDiariasReporteDTO {
    private Integer clubId;
    private String nombreClub;
    private LocalDate fecha;
    private ResumenDiaVentasDTO resumen;
    @Builder.Default
    private List<RegistroVentaDiariaDTO> filas = new ArrayList<>();
}
