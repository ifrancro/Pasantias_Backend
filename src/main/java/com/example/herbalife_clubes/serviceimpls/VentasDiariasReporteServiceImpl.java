package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.reporte.ProductoVentaDiariaDTO;
import com.example.herbalife_clubes.dtos.reporte.RegistroVentaDiariaDTO;
import com.example.herbalife_clubes.dtos.reporte.ResumenDiaVentasDTO;
import com.example.herbalife_clubes.dtos.reporte.ResumenDiaVentasDTO.RankingProductoDiaDTO;
import com.example.herbalife_clubes.dtos.reporte.VentasDiariasReporteDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.services.VentasDiariasReporteService;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentasDiariasReporteServiceImpl implements VentasDiariasReporteService {

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ClubRepository clubRepository;
    private final PedidoRepository pedidoRepository;

    @Override
    @Transactional(readOnly = true)
    public VentasDiariasReporteDTO generarReporte(Integer clubId, LocalDate fecha) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();

        List<Integer> pedidoIds = pedidoRepository.findEntregadoIdsByClubAndRango(
                clubId, desde, hasta, EstadoPedido.ENTREGADO);

        List<Pedido> pedidos = pedidoIds.isEmpty()
                ? List.of()
                : ordenarPedidos(
                        deduplicarPedidos(pedidoRepository.findEntregadosDetalleByIds(pedidoIds)),
                        pedidoIds);

        List<RegistroVentaDiariaDTO> filas = new ArrayList<>();
        int numeroFila = 1;
        BigDecimal totalIngresos = BigDecimal.ZERO;
        long conteoN = 0;
        long conteoR = 0;
        Map<String, Long> rankingMap = new LinkedHashMap<>();

        for (Pedido pedido : pedidos) {
            RegistroVentaDiariaDTO fila = construirFila(pedido, fecha, numeroFila++);
            filas.add(fila);
            totalIngresos = totalIngresos.add(fila.getTotalBs() != null ? fila.getTotalBs() : BigDecimal.ZERO);

            if ("N".equals(fila.getEstatusVisita())) {
                conteoN++;
            } else if ("R".equals(fila.getEstatusVisita())) {
                conteoR++;
            }

            for (ProductoVentaDiariaDTO prod : fila.getProductos()) {
                String nombre = prod.getNombre() != null ? prod.getNombre() : "";
                long cant = prod.getCantidad() != null ? prod.getCantidad() : 0;
                rankingMap.merge(nombre, cant, Long::sum);
            }
        }

        List<RankingProductoDiaDTO> ranking = rankingMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> RankingProductoDiaDTO.builder()
                        .nombre(e.getKey())
                        .cantidad(e.getValue())
                        .build())
                .collect(Collectors.toList());

        ResumenDiaVentasDTO resumen = ResumenDiaVentasDTO.builder()
                .fecha(fecha)
                .totalVentas(filas.size())
                .totalIngresosBs(totalIngresos)
                .ingresosPorTipoPago(new LinkedHashMap<>())
                .conteoNuevos(conteoN)
                .conteoReferidos(conteoR)
                .rankingProductos(ranking)
                .build();

        return VentasDiariasReporteDTO.builder()
                .clubId(clubId)
                .nombreClub(club.getNombreClub() != null ? club.getNombreClub() : "Club " + clubId)
                .fecha(fecha)
                .resumen(resumen)
                .filas(filas)
                .build();
    }

    private static List<Pedido> deduplicarPedidos(List<Pedido> candidatos) {
        Map<Integer, Pedido> mejorPorId = new LinkedHashMap<>();
        for (Pedido candidato : candidatos) {
            if (candidato.getId() == null) {
                continue;
            }
            Pedido actual = mejorPorId.get(candidato.getId());
            if (actual == null || contarItems(candidato) > contarItems(actual)) {
                mejorPorId.put(candidato.getId(), candidato);
            }
        }
        return new ArrayList<>(mejorPorId.values());
    }

    private static int contarItems(Pedido pedido) {
        return pedido.getItems() != null ? pedido.getItems().size() : 0;
    }

    private static List<Pedido> ordenarPedidos(List<Pedido> pedidos, List<Integer> pedidoIds) {
        Map<Integer, Integer> orden = new LinkedHashMap<>();
        for (int i = 0; i < pedidoIds.size(); i++) {
            orden.put(pedidoIds.get(i), i);
        }
        List<Pedido> ordenados = new ArrayList<>(pedidos);
        ordenados.sort((a, b) -> Integer.compare(
                orden.getOrDefault(a.getId(), Integer.MAX_VALUE),
                orden.getOrDefault(b.getId(), Integer.MAX_VALUE)));
        return ordenados;
    }

    private RegistroVentaDiariaDTO construirFila(Pedido pedido, LocalDate fecha, int numeroFila) {
        List<ProductoVentaDiariaDTO> productos = new ArrayList<>();
        BigDecimal totalBs = BigDecimal.ZERO;

        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            for (PedidoItem item : pedido.getItems()) {
                BigDecimal subtotal = item.getSubtotal();
                if (subtotal == null && item.getPrecioUnitario() != null && item.getCantidad() != null) {
                    subtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
                }
                if (subtotal == null) {
                    subtotal = BigDecimal.ZERO;
                }
                totalBs = totalBs.add(subtotal);

                var producto = item.getProducto();
                productos.add(ProductoVentaDiariaDTO.builder()
                        .productoId(producto != null ? producto.getId() : null)
                        .nombre(producto != null ? producto.getNombre() : "")
                        .cantidad(item.getCantidad())
                        .esCombo(producto != null && Boolean.TRUE.equals(producto.getEsCombo()))
                        .subtotal(subtotal)
                        .build());
            }
        } else if (pedido.getProducto() != null) {
            int cantidad = pedido.getCantidad() != null ? pedido.getCantidad() : 1;
            var producto = pedido.getProducto();
            BigDecimal precio = producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO;
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
            totalBs = totalBs.add(subtotal);
            productos.add(ProductoVentaDiariaDTO.builder()
                    .productoId(producto.getId())
                    .nombre(producto.getNombre() != null ? producto.getNombre() : "")
                    .cantidad(cantidad)
                    .esCombo(Boolean.TRUE.equals(producto.getEsCombo()))
                    .subtotal(subtotal)
                    .build());
        }

        Membresia membresia = pedido.getMembresia();
        String nombre = resolverNombre(membresia);
        String numeroSocio = membresia != null ? membresia.getNumeroSocio() : null;
        String estatusVisita = resolverEstatusVisita(membresia, pedido, fecha);
        String origen = membresia == null ? "MOSTRADOR" : "SOCIO";

        LocalDateTime fp = pedido.getFechaPedido();
        String hora = fp != null ? fp.format(HORA_FMT) : "";

        return RegistroVentaDiariaDTO.builder()
                .numeroFila(numeroFila)
                .fecha(fecha)
                .hora(hora)
                .nombre(nombre)
                .estatusVisita(estatusVisita)
                .numeroSocio(numeroSocio)
                .productos(productos)
                .tipoPago(null)
                .totalBs(totalBs)
                .origen(origen)
                .pedidoId(pedido.getId())
                .build();
    }

    private static String resolverNombre(Membresia membresia) {
        if (membresia == null || membresia.getUsuario() == null) {
            return "";
        }
        Usuario u = membresia.getUsuario();
        String n = u.getNombre() != null ? u.getNombre().trim() : "";
        String a = u.getApellido() != null ? u.getApellido().trim() : "";
        return (n + " " + a).trim();
    }

    private String resolverEstatusVisita(Membresia membresia, Pedido pedido, LocalDate fecha) {
        if (membresia == null) {
            return "";
        }
        if (membresia.getReferidoPorMembresia() != null) {
            return "R";
        }
        boolean esNuevo = false;
        if (membresia.getFechaRegistro() != null
                && membresia.getFechaRegistro().toLocalDate().equals(fecha)) {
            esNuevo = true;
        } else if (pedido.getFechaPedido() != null && pedido.getClub() != null) {
            long anteriores = pedidoRepository.countEntregadosAntesDe(
                    membresia.getId(),
                    pedido.getClub().getId(),
                    pedido.getFechaPedido(),
                    EstadoPedido.ENTREGADO);
            esNuevo = anteriores == 0;
        }
        return esNuevo ? "N" : "";
    }

    @Override
    public byte[] generarExcel(VentasDiariasReporteDTO reporte) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = "Registro " + reporte.getFecha();
            Sheet sh = wb.createSheet(sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName);
            int r = 0;

            Row titleRow = sh.createRow(r++);
            titleRow.createCell(0).setCellValue("Registro de ventas — " + reporte.getNombreClub());
            Row dateRow = sh.createRow(r++);
            dateRow.createCell(0).setCellValue("Fecha");
            dateRow.createCell(1).setCellValue(reporte.getFecha().toString());
            r++;

            Row header = sh.createRow(r++);
            String[] cols = {"#", "N/R", "Nombre", "Productos", "Tipo pago", "Total (Bs.)", "Hora"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            for (RegistroVentaDiariaDTO fila : reporte.getFilas()) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(fila.getNumeroFila() != null ? fila.getNumeroFila() : 0);
                row.createCell(1).setCellValue(fila.getEstatusVisita() != null ? fila.getEstatusVisita() : "");
                row.createCell(2).setCellValue(fila.getNombre() != null ? fila.getNombre() : "");
                row.createCell(3).setCellValue(formatearProductos(fila));
                row.createCell(4).setCellValue(fila.getTipoPago() != null ? fila.getTipoPago() : "");
                row.createCell(5).setCellValue(fila.getTotalBs() != null ? fila.getTotalBs().doubleValue() : 0);
                row.createCell(6).setCellValue(fila.getHora() != null ? fila.getHora() : "");
            }

            r++;
            ResumenDiaVentasDTO res = reporte.getResumen();
            if (res != null) {
                Row pie1 = sh.createRow(r++);
                pie1.createCell(0).setCellValue("Total ventas del día");
                pie1.createCell(1).setCellValue(res.getTotalVentas());
                Row pie2 = sh.createRow(r++);
                pie2.createCell(0).setCellValue("Total ingresos (Bs.)");
                pie2.createCell(1).setCellValue(res.getTotalIngresosBs().doubleValue());
                Row pie3 = sh.createRow(r++);
                pie3.createCell(0).setCellValue("Nuevos (N)");
                pie3.createCell(1).setCellValue(res.getConteoNuevos());
                Row pie4 = sh.createRow(r++);
                pie4.createCell(0).setCellValue("Referidos (R)");
                pie4.createCell(1).setCellValue(res.getConteoReferidos());

                if (res.getRankingProductos() != null && !res.getRankingProductos().isEmpty()) {
                    r++;
                    Row rkTitle = sh.createRow(r++);
                    rkTitle.createCell(0).setCellValue("Ranking productos del día");
                    Row rkHeader = sh.createRow(r++);
                    rkHeader.createCell(0).setCellValue("Producto");
                    rkHeader.createCell(1).setCellValue("Cantidad");
                    for (RankingProductoDiaDTO rk : res.getRankingProductos()) {
                        Row rkRow = sh.createRow(r++);
                        rkRow.createCell(0).setCellValue(rk.getNombre());
                        rkRow.createCell(1).setCellValue(rk.getCantidad());
                    }
                }
            }

            for (int i = 0; i < 7; i++) {
                sh.setColumnWidth(i, i == 3 ? 12000 : 4500);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar Excel de ventas diarias", e);
        }
    }

    @Override
    public byte[] generarPdf(VentasDiariasReporteDTO reporte) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            doc.add(new Paragraph("Registro de ventas diarias", title));
            doc.add(new Paragraph("Club: " + reporte.getNombreClub(), normal));
            doc.add(new Paragraph("Fecha: " + reporte.getFecha(), normal));
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(7);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{1f, 1f, 2.5f, 3f, 1.5f, 1.5f, 1f});
            for (String h : new String[]{"#", "N/R", "Nombre", "Productos", "Pago", "Total Bs.", "Hora"}) {
                t.addCell(new PdfPCell(new Phrase(h, header)));
            }
            for (RegistroVentaDiariaDTO fila : reporte.getFilas()) {
                t.addCell(cell(String.valueOf(fila.getNumeroFila()), normal));
                t.addCell(cell(fila.getEstatusVisita() != null ? fila.getEstatusVisita() : "", normal));
                t.addCell(cell(fila.getNombre() != null ? fila.getNombre() : "", normal));
                t.addCell(cell(formatearProductos(fila), normal));
                t.addCell(cell(fila.getTipoPago() != null ? fila.getTipoPago() : "", normal));
                t.addCell(cell(fila.getTotalBs() != null ? fila.getTotalBs().toPlainString() : "0", normal));
                t.addCell(cell(fila.getHora() != null ? fila.getHora() : "", normal));
            }
            doc.add(t);

            ResumenDiaVentasDTO res = reporte.getResumen();
            if (res != null) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph(
                        "Total ventas: " + res.getTotalVentas()
                                + " | Ingresos: Bs. " + res.getTotalIngresosBs()
                                + " | N: " + res.getConteoNuevos()
                                + " | R: " + res.getConteoReferidos(),
                        normal));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar PDF de ventas diarias", e);
        }
    }

    private static PdfPCell cell(String text, Font font) {
        return new PdfPCell(new Phrase(text, font));
    }

    private static String formatearProductos(RegistroVentaDiariaDTO fila) {
        if (fila.getProductos() == null || fila.getProductos().isEmpty()) {
            return "";
        }
        return fila.getProductos().stream()
                .map(p -> {
                    String nombre = p.getNombre() != null ? p.getNombre() : "";
                    int cant = p.getCantidad() != null ? p.getCantidad() : 1;
                    return cant > 1 ? nombre + " (x" + cant + ")" : nombre;
                })
                .collect(Collectors.joining(", "));
    }
}
