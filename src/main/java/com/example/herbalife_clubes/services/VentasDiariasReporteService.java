package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.reporte.VentasDiariasReporteDTO;

import java.time.LocalDate;

public interface VentasDiariasReporteService {

    VentasDiariasReporteDTO generarReporte(Integer clubId, LocalDate fecha);

    byte[] generarExcel(VentasDiariasReporteDTO reporte);

    byte[] generarPdf(VentasDiariasReporteDTO reporte);
}
