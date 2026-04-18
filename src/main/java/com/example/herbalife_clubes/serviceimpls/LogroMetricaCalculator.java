package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.repositories.AsistenciaRepository;
import com.example.herbalife_clubes.repositories.ConsumoRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class LogroMetricaCalculator {

    private final AsistenciaRepository asistenciaRepository;
    private final ConsumoRepository consumoRepository;
    private final MembresiaRepository membresiaRepository;

    public boolean aplicaAMembresia(Logro logro, Membresia membresia) {
        if (logro.getEstadoAprobacion() == null || !"APROBADO".equalsIgnoreCase(logro.getEstadoAprobacion())) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (logro.getFechaInicio() != null && today.isBefore(logro.getFechaInicio())) {
            return false;
        }
        if (logro.getFechaFin() != null && today.isAfter(logro.getFechaFin())) {
            return false;
        }
        if (logro.getClubCreador() == null) {
            return true;
        }
        return membresia.getClub() != null
                && logro.getClubCreador().getId().equals(membresia.getClub().getId());
    }

    public int cantidadActual(Membresia membresia, Logro logro, RequisitoLogro req) {
        if (req.getTipoMetrica() == null) {
            return 0;
        }
        String tipo = req.getTipoMetrica().trim().toUpperCase();
        LocalDate ini = logro.getFechaInicio() != null ? logro.getFechaInicio() : LocalDate.MIN;
        LocalDate fin = logro.getFechaFin() != null ? logro.getFechaFin() : LocalDate.MAX;
        LocalDateTime desde = ini.atStartOfDay();
        LocalDateTime hasta = fin.atTime(LocalTime.MAX);

        return switch (tipo) {
            case "ASISTENCIA" -> (int) asistenciaRepository.findByMembresiaId(membresia.getId()).stream()
                    .filter(a -> a.getFechaDia() != null
                            && !a.getFechaDia().isBefore(ini)
                            && !a.getFechaDia().isAfter(fin))
                    .count();
            case "CONSUMO" -> (int) consumoRepository.findByMembresiaId(membresia.getId()).stream()
                    .filter(c -> c.getFechaHora() != null
                            && !c.getFechaHora().isBefore(desde)
                            && !c.getFechaHora().isAfter(hasta))
                    .count();
            case "REFERIDOS", "REFERIDO" -> (int) membresiaRepository.findByReferidoPorMembresiaId(membresia.getId()).stream()
                    .filter(ref -> ref.getClub() != null && membresia.getClub() != null
                            && ref.getClub().getId().equals(membresia.getClub().getId()))
                    .filter(ref -> ref.getFechaRegistro() != null
                            && !ref.getFechaRegistro().isBefore(desde)
                            && !ref.getFechaRegistro().isAfter(hasta))
                    .count();
            case "RACHA" -> membresia.getRachaActual() != null ? membresia.getRachaActual() : 0;
            default -> 0;
        };
    }

    public boolean cumpleTodosRequisitos(Membresia membresia, Logro logro) {
        if (logro.getRequisitos() == null || logro.getRequisitos().isEmpty()) {
            return false;
        }
        for (RequisitoLogro req : logro.getRequisitos()) {
            if (req.getCantidadEsperada() == null || req.getCantidadEsperada() <= 0) {
                return false;
            }
            if (cantidadActual(membresia, logro, req) < req.getCantidadEsperada()) {
                return false;
            }
        }
        return true;
    }
}
