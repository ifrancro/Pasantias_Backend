package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.reporte.ResumenMensualVentasDTO;

public interface ResumenMensualVentasService {

    ResumenMensualVentasDTO generarReporte(Integer clubId, int anio, int mes);

    byte[] generarExcel(ResumenMensualVentasDTO reporte);

    byte[] generarPdf(ResumenMensualVentasDTO reporte);
}
