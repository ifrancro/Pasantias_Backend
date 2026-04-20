package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.reporte.ReporteGestionSnapshot;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ReporteGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Reportes de gestión para anfitriones: PDF y Excel generados en memoria (sin persistir).
 */
@RestController
@RequestMapping("/api/reportes")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ReporteController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteGestionService reporteGestionService;
    private final ClubRepository clubRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Descarga reporte de gestión del club en el rango indicado.
     * Query: fechaInicio, fechaFin (ISO-8601), formato=PDF|EXCEL
     */
    @GetMapping("/anfitrion/{clubId}/descargar")
    public ResponseEntity<byte[]> descargarReporteGestion(
            @PathVariable Integer clubId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam String formato) {

        if (fechaInicio.isAfter(fechaFin)) {
            return ResponseEntity.badRequest().build();
        }

        String formatoNorm = formato != null ? formato.trim().toUpperCase() : "";
        if (!"PDF".equals(formatoNorm) && !"EXCEL".equals(formatoNorm)) {
            return ResponseEntity.badRequest().build();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean esAdmin = usuario.getRol() != null
                && "ADMIN".equalsIgnoreCase(usuario.getRol().getNombre());

        boolean esAnfitrion = clubRepository.findByIdAndAnfitrionId(clubId, usuario.getId()).isPresent();
        if (!esAnfitrion) {
            var clubOpt = clubRepository.findById(clubId);
            if (clubOpt.isPresent()) {
                var club = clubOpt.get();
                if (club.getAnfitrion() != null && club.getAnfitrion().getId().equals(usuario.getId())) {
                    esAnfitrion = true;
                }
            }
        }

        if (!esAdmin && !esAnfitrion) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ReporteGestionSnapshot datos = reporteGestionService.recopilarDatos(clubId, fechaInicio, fechaFin);

        byte[] body;
        String extension;
        MediaType contentType;
        if ("EXCEL".equals(formatoNorm)) {
            body = reporteGestionService.generarExcel(datos);
            extension = "xlsx";
            contentType = XLSX;
        } else {
            body = reporteGestionService.generarPdf(datos);
            extension = "pdf";
            contentType = MediaType.APPLICATION_PDF;
        }

        String filename = String.format(
                "reporte_gestion_club%d_%s_%s.%s",
                clubId,
                fechaInicio,
                fechaFin,
                extension);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build());
        headers.setContentLength(body.length);

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
