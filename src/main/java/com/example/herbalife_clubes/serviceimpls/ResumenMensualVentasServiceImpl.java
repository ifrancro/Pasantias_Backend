package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.reporte.ResumenMensualVentasDTO;
import com.example.herbalife_clubes.dtos.reporte.ResumenMesKpiDTO;
import com.example.herbalife_clubes.dtos.reporte.TopProductoMesDTO;
import com.example.herbalife_clubes.dtos.reporte.VentasPorDiaMesDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.services.ResumenMensualVentasService;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResumenMensualVentasServiceImpl implements ResumenMensualVentasService {

    private static final int TOP_EXPORT = 20;

    private final ClubRepository clubRepository;
    private final PedidoRepository pedidoRepository;

    @Override
    public ResumenMensualVentasDTO generarReporte(Integer clubId, int anio, int mes) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("mes debe estar entre 1 y 12");
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));

        LocalDate fechaInicio = LocalDate.of(anio, mes, 1);
        LocalDate fechaFin = fechaInicio.withDayOfMonth(fechaInicio.lengthOfMonth());
        LocalDateTime desde = fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> ventasPorDiaMap = new HashMap<>();
        for (Object[] row : pedidoRepository.countEntregadosPorDia(clubId, desde, hasta)) {
            LocalDate dia = toLocalDate(row[0]);
            if (dia != null) {
                ventasPorDiaMap.put(dia, toLong(row[1]));
            }
        }

        Map<LocalDate, BigDecimal> ingresosPorDiaMap = new HashMap<>();
        for (Object[] row : pedidoRepository.ingresosEntregadosPorDia(clubId, desde, hasta)) {
            LocalDate dia = toLocalDate(row[0]);
            if (dia != null) {
                ingresosPorDiaMap.put(dia, toBigDecimal(row[1]));
            }
        }

        List<VentasPorDiaMesDTO> ventasPorDia = new ArrayList<>();
        long totalVentas = 0;
        BigDecimal totalIngresos = BigDecimal.ZERO;

        int diasEnMes = fechaInicio.lengthOfMonth();
        for (int d = 1; d <= diasEnMes; d++) {
            LocalDate dia = LocalDate.of(anio, mes, d);
            long ventas = ventasPorDiaMap.getOrDefault(dia, 0L);
            BigDecimal ingresos = ingresosPorDiaMap.getOrDefault(dia, BigDecimal.ZERO);
            ventasPorDia.add(VentasPorDiaMesDTO.builder()
                    .fecha(dia)
                    .totalVentas(ventas)
                    .totalIngresosBs(ingresos)
                    .build());
            totalVentas += ventas;
            totalIngresos = totalIngresos.add(ingresos);
        }

        List<TopProductoMesDTO> topProductos = new ArrayList<>();
        for (Object[] row : pedidoRepository.rankingProductosVendidos(clubId, desde, hasta)) {
            Integer pid = row[0] != null ? ((Number) row[0]).intValue() : null;
            String nombre = row[1] != null ? row[1].toString() : "";
            long cant = toLong(row[2]);
            topProductos.add(TopProductoMesDTO.builder()
                    .productoId(pid)
                    .nombre(nombre)
                    .cantidadVendida(cant)
                    .build());
        }

        String nombreMes = Month.of(mes).getDisplayName(TextStyle.FULL, new Locale("es"));

        return ResumenMensualVentasDTO.builder()
                .clubId(clubId)
                .nombreClub(club.getNombreClub() != null ? club.getNombreClub() : "Club " + clubId)
                .anio(anio)
                .mes(mes)
                .nombreMes(capitalizar(nombreMes))
                .resumen(ResumenMesKpiDTO.builder()
                        .totalVentas(totalVentas)
                        .totalIngresosBs(totalIngresos)
                        .build())
                .ventasPorDia(ventasPorDia)
                .topProductos(topProductos)
                .build();
    }

    @Override
    public byte[] generarExcel(ResumenMensualVentasDTO reporte) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ResumenMesKpiDTO kpi = reporte.getResumen();

            Sheet resumen = wb.createSheet("Resumen");
            int r = 0;
            resumen.createRow(r++).createCell(0).setCellValue("Resumen mensual de ventas");
            Row rClub = resumen.createRow(r++);
            rClub.createCell(0).setCellValue("Club");
            rClub.createCell(1).setCellValue(reporte.getNombreClub());
            Row rPeriodo = resumen.createRow(r++);
            rPeriodo.createCell(0).setCellValue("Periodo");
            rPeriodo.createCell(1).setCellValue(reporte.getNombreMes() + " " + reporte.getAnio());
            Row rVentas = resumen.createRow(r++);
            rVentas.createCell(0).setCellValue("Total ventas");
            rVentas.createCell(1).setCellValue(kpi != null ? kpi.getTotalVentas() : 0);
            Row rIng = resumen.createRow(r++);
            rIng.createCell(0).setCellValue("Total ingresos (Bs.)");
            rIng.createCell(1).setCellValue(kpi != null ? kpi.getTotalIngresosBs().doubleValue() : 0);

            Sheet porDia = wb.createSheet("Por día");
            int rd = 0;
            Row hDia = porDia.createRow(rd++);
            hDia.createCell(0).setCellValue("Fecha");
            hDia.createCell(1).setCellValue("Ventas");
            hDia.createCell(2).setCellValue("Ingresos (Bs.)");
            for (VentasPorDiaMesDTO v : reporte.getVentasPorDia()) {
                Row row = porDia.createRow(rd++);
                row.createCell(0).setCellValue(v.getFecha().toString());
                row.createCell(1).setCellValue(v.getTotalVentas());
                row.createCell(2).setCellValue(v.getTotalIngresosBs().doubleValue());
            }

            Sheet top = wb.createSheet("Top productos");
            int rt = 0;
            Row hTop = top.createRow(rt++);
            hTop.createCell(0).setCellValue("#");
            hTop.createCell(1).setCellValue("Producto");
            hTop.createCell(2).setCellValue("Cantidad vendida");
            int pos = 1;
            for (TopProductoMesDTO p : reporte.getTopProductos().stream().limit(TOP_EXPORT).toList()) {
                Row row = top.createRow(rt++);
                row.createCell(0).setCellValue(pos++);
                row.createCell(1).setCellValue(p.getNombre());
                row.createCell(2).setCellValue(p.getCantidadVendida());
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar Excel mensual", e);
        }
    }

    @Override
    public byte[] generarPdf(ResumenMensualVentasDTO reporte) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            ResumenMesKpiDTO kpi = reporte.getResumen();
            doc.add(new Paragraph("Resumen mensual de ventas", title));
            doc.add(new Paragraph("Club: " + reporte.getNombreClub(), normal));
            doc.add(new Paragraph("Periodo: " + reporte.getNombreMes() + " " + reporte.getAnio(), normal));
            if (kpi != null) {
                doc.add(new Paragraph(
                        "Total ventas: " + kpi.getTotalVentas()
                                + " | Total ingresos: Bs. " + kpi.getTotalIngresosBs(),
                        normal));
            }
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Ventas por día", title));
            PdfPTable tDia = new PdfPTable(3);
            tDia.setWidthPercentage(100);
            tDia.addCell(headerCell("Fecha", header));
            tDia.addCell(headerCell("Ventas", header));
            tDia.addCell(headerCell("Ingresos (Bs.)", header));
            for (VentasPorDiaMesDTO v : reporte.getVentasPorDia()) {
                tDia.addCell(cell(v.getFecha().toString(), normal));
                tDia.addCell(cell(String.valueOf(v.getTotalVentas()), normal));
                tDia.addCell(cell(v.getTotalIngresosBs().toPlainString(), normal));
            }
            doc.add(tDia);
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Top productos del mes", title));
            PdfPTable tTop = new PdfPTable(3);
            tTop.setWidthPercentage(100);
            tTop.addCell(headerCell("#", header));
            tTop.addCell(headerCell("Producto", header));
            tTop.addCell(headerCell("Cantidad", header));
            int pos = 1;
            for (TopProductoMesDTO p : reporte.getTopProductos().stream().limit(TOP_EXPORT).toList()) {
                tTop.addCell(cell(String.valueOf(pos++), normal));
                tTop.addCell(cell(p.getNombre(), normal));
                tTop.addCell(cell(String.valueOf(p.getCantidadVendida()), normal));
            }
            doc.add(tTop);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar PDF mensual", e);
        }
    }

    private static PdfPCell headerCell(String text, Font font) {
        return new PdfPCell(new Phrase(text, font));
    }

    private static PdfPCell cell(String text, Font font) {
        return new PdfPCell(new Phrase(text, font));
    }

    private static String capitalizar(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static LocalDate toLocalDate(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDate ld) {
            return ld;
        }
        if (o instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (o instanceof java.util.Date d) {
            return new Date(d.getTime()).toLocalDate();
        }
        return LocalDate.parse(o.toString());
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(o.toString());
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(o.toString());
    }
}
