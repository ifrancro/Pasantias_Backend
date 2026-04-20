package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.reporte.ReporteGestionSnapshot;

import java.time.LocalDate;

public interface ReporteGestionService {

    ReporteGestionSnapshot recopilarDatos(Integer clubId, LocalDate fechaInicio, LocalDate fechaFin);

    byte[] generarExcel(ReporteGestionSnapshot datos);

    byte[] generarPdf(ReporteGestionSnapshot datos);
}
