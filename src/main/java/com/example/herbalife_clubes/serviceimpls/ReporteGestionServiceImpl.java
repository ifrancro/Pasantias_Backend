package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.reporte.ReporteGestionSnapshot;
import com.example.herbalife_clubes.dtos.reporte.ReporteGestionSnapshot.ProductoVendidoRanking;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.AsistenciaRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.services.ReporteGestionService;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteGestionServiceImpl implements ReporteGestionService {

    private final ClubRepository clubRepository;
    private final PedidoRepository pedidoRepository;
    private final AsistenciaRepository asistenciaRepository;

    @Override
    public ReporteGestionSnapshot recopilarDatos(Integer clubId, LocalDate fechaInicio, LocalDate fechaFin) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));

        LocalDateTime desde = fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin.plusDays(1).atStartOfDay();

        BigDecimal totalIngresos = pedidoRepository.sumIngresosPuntosValorEntregados(clubId, desde, hasta);
        if (totalIngresos == null) {
            totalIngresos = BigDecimal.ZERO;
        }

        Map<LocalDate, Long> pedidosPorDia = new LinkedHashMap<>();
        for (Object[] row : pedidoRepository.countPedidosPorDia(clubId, desde, hasta)) {
            LocalDate dia = toLocalDate(row[0]);
            long cnt = toLong(row[1]);
            if (dia != null) {
                pedidosPorDia.put(dia, cnt);
            }
        }

        Map<LocalDate, Long> asistenciasPorDia = new LinkedHashMap<>();
        for (Object[] row : asistenciaRepository.countAsistenciasPorDiaEnClub(clubId, fechaInicio, fechaFin)) {
            LocalDate dia = (LocalDate) row[0];
            long cnt = toLong(row[1]);
            if (dia != null) {
                asistenciasPorDia.put(dia, cnt);
            }
        }

        List<ProductoVendidoRanking> ranking = new ArrayList<>();
        for (Object[] row : pedidoRepository.rankingProductosVendidos(clubId, desde, hasta)) {
            Integer pid = row[0] != null ? ((Number) row[0]).intValue() : null;
            String nombre = row[1] != null ? row[1].toString() : "";
            long cant = toLong(row[2]);
            ranking.add(ProductoVendidoRanking.builder()
                    .productoId(pid)
                    .nombreProducto(nombre)
                    .cantidadVendida(cant)
                    .build());
        }

        return ReporteGestionSnapshot.builder()
                .clubId(clubId)
                .nombreClub(club.getNombreClub() != null ? club.getNombreClub() : "Club " + clubId)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .totalIngresosPuntosValor(totalIngresos)
                .asistenciasPorDia(asistenciasPorDia)
                .pedidosPorDia(pedidosPorDia)
                .rankingProductos(ranking)
                .build();
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
            return new java.sql.Date(d.getTime()).toLocalDate();
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

    @Override
    public byte[] generarExcel(ReporteGestionSnapshot d) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Reporte");
            int r = 0;
            Row row0 = sh.createRow(r++);
            row0.createCell(0).setCellValue("Reporte de gestión");
            Row row1 = sh.createRow(r++);
            row1.createCell(0).setCellValue("Club");
            row1.createCell(1).setCellValue(d.getNombreClub());
            Row row2 = sh.createRow(r++);
            row2.createCell(0).setCellValue("Periodo");
            row2.createCell(1).setCellValue(d.getFechaInicio() + " - " + d.getFechaFin());
            Row row3 = sh.createRow(r++);
            row3.createCell(0).setCellValue("Total ingresos (Bs., precio histórico en pedidos ENTREGADOS)");
            row3.createCell(1).setCellValue(d.getTotalIngresosPuntosValor().doubleValue());
            r++;

            Row hAs = sh.createRow(r++);
            hAs.createCell(0).setCellValue("Asistencias por día");
            Row hAs2 = sh.createRow(r++);
            hAs2.createCell(0).setCellValue("Fecha");
            hAs2.createCell(1).setCellValue("Cantidad");
            for (Map.Entry<LocalDate, Long> e : d.getAsistenciasPorDia().entrySet()) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(e.getKey().toString());
                row.createCell(1).setCellValue(e.getValue());
            }
            r++;

            Row hPe = sh.createRow(r++);
            hPe.createCell(0).setCellValue("Pedidos por día (excluye CANCELADO)");
            Row hPe2 = sh.createRow(r++);
            hPe2.createCell(0).setCellValue("Fecha");
            hPe2.createCell(1).setCellValue("Cantidad");
            for (Map.Entry<LocalDate, Long> e : d.getPedidosPorDia().entrySet()) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(e.getKey().toString());
                row.createCell(1).setCellValue(e.getValue());
            }
            r++;

            Row hRk = sh.createRow(r++);
            hRk.createCell(0).setCellValue("Ranking productos vendidos (ENTREGADO)");
            Row hRk2 = sh.createRow(r++);
            hRk2.createCell(0).setCellValue("Producto");
            hRk2.createCell(1).setCellValue("Cantidad");
            for (ProductoVendidoRanking p : d.getRankingProductos()) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(p.getNombreProducto());
                row.createCell(1).setCellValue(p.getCantidadVendida());
            }

            sh.setColumnWidth(0, 8000);
            sh.setColumnWidth(1, 5000);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar Excel", e);
        }
    }

    @Override
    public byte[] generarPdf(ReporteGestionSnapshot d) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("Reporte de gestión del club", title));
            doc.add(new Paragraph("Club: " + d.getNombreClub(), normal));
            doc.add(new Paragraph("Periodo: " + d.getFechaInicio() + " — " + d.getFechaFin(), normal));
            doc.add(new Paragraph("Total ingresos (Bs., precio histórico en pedidos ENTREGADOS): " + d.getTotalIngresosPuntosValor(), normal));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Asistencias por día", title));
            doc.add(tablaDosColumnas("Fecha", "Cantidad", d.getAsistenciasPorDia()));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Pedidos por día (excluye CANCELADO)", title));
            doc.add(tablaDosColumnas("Fecha", "Cantidad", d.getPedidosPorDia()));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Ranking productos vendidos (ENTREGADO)", title));
            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(100);
            t.addCell(headerCell("Producto"));
            t.addCell(headerCell("Cantidad"));
            for (ProductoVendidoRanking p : d.getRankingProductos()) {
                t.addCell(new PdfPCell(new Phrase(p.getNombreProducto(), normal)));
                t.addCell(new PdfPCell(new Phrase(String.valueOf(p.getCantidadVendida()), normal)));
            }
            doc.add(t);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar PDF", e);
        }
    }

    private static PdfPCell headerCell(String text) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        return new PdfPCell(new Phrase(text, f));
    }

    private static PdfPTable tablaDosColumnas(String h1, String h2, Map<LocalDate, Long> mapa) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        t.addCell(headerCell(h1));
        t.addCell(headerCell(h2));
        if (mapa.isEmpty()) {
            t.addCell(new PdfPCell(new Phrase("Sin datos", normal)));
            t.addCell(new PdfPCell(new Phrase("-", normal)));
        } else {
            for (Map.Entry<LocalDate, Long> e : mapa.entrySet()) {
                t.addCell(new PdfPCell(new Phrase(e.getKey().toString(), normal)));
                t.addCell(new PdfPCell(new Phrase(String.valueOf(e.getValue()), normal)));
            }
        }
        return t;
    }
}
