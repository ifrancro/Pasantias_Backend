package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.AsistenciaMapper;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.AsistenciaService;
import com.example.herbalife_clubes.services.ComboConsumoService;
import com.example.herbalife_clubes.services.MembresiaLogroService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AsistenciaServiceImpl implements AsistenciaService {
    @Autowired
    private AsistenciaRepository asistenciaRepository;
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private MembresiaLogroService membresiaLogroService;
    @Autowired
    private ComboConsumoService comboConsumoService;

    @Override
    @Transactional
    public AsistenciaDTO registrarAsistencia(Integer membresiaId, Integer clubId, String qrClub) {
        // Validar membresía existe y está activa
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        
        if (membresia.getEstado() == null || !membresia.getEstado().equals("ACTIVA")) {
            throw new IllegalArgumentException("La membresía no está activa. Estado actual: " + membresia.getEstado());
        }
        
        // Validar club existe y está activo (APROBADO o ACTIVO)
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        
        if (club.getEstado() == null || (!club.getEstado().equals("APROBADO") && !club.getEstado().equals("ACTIVO"))) {
            throw new IllegalArgumentException("El club no está activo. Estado actual: " + club.getEstado());
        }

        comboConsumoService.validarComboConsumidoAntesDeAsistencia(membresiaId);
        
        // Validar 1 asistencia por día por socio (GLOBAL, sin importar el club)
        LocalDate fechaDia = LocalDate.now();
        Optional<Asistencia> asistenciaExistente = asistenciaRepository.findByMembresiaIdAndFechaDia(membresiaId, fechaDia);
        if (asistenciaExistente.isPresent()) {
            throw new IllegalArgumentException("Ya existe una asistencia registrada para este socio hoy. No se puede registrar asistencia duplicada en el mismo día.");
        }
        
        // Actualizar racha en membresía
        LocalDate ultimaAsistencia = membresia.getUltimaAsistenciaDia();
        LocalDate ayer = fechaDia.minusDays(1);
        
        if (ultimaAsistencia == null) {
            // Primera asistencia
            membresia.setRachaActual(1);
        } else if (ultimaAsistencia.equals(fechaDia)) {
            // Ya registrado hoy (no debería llegar aquí por la validación anterior, pero por seguridad)
            throw new IllegalArgumentException("Ya existe una asistencia registrada para este socio hoy");
        } else if (ultimaAsistencia.equals(ayer)) {
            // Continúa la racha (asistencia consecutiva)
            membresia.setRachaActual((membresia.getRachaActual() != null ? membresia.getRachaActual() : 0) + 1);
        } else {
            // Racha rota, empezar de nuevo
            membresia.setRachaActual(1);
        }
        
        // Actualizar racha máxima
        if (membresia.getRachaMaxima() == null || membresia.getRachaActual() > membresia.getRachaMaxima()) {
            membresia.setRachaMaxima(membresia.getRachaActual());
        }
        
        // Actualizar última asistencia
        membresia.setUltimaAsistenciaDia(fechaDia);
        
        membresiaRepository.save(membresia);
        
        // Crear la asistencia
        Asistencia asistencia = new Asistencia();
        asistencia.setMembresia(membresia);
        asistencia.setClub(club);
        asistencia.setFechaDia(fechaDia);
        asistencia.setEstado("CONFIRMADA");
        
        Asistencia savedAsistencia = asistenciaRepository.save(asistencia);
        
        // Recalcular puntos acumulados basándose en el conteo total de asistencias
        // Esto garantiza que los puntos siempre estén sincronizados con las asistencias
        recalcularPuntosAcumulados(membresiaId);
        
        // Recargar membresía para obtener datos actualizados (puntos, racha, etc.)
        membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        
        // Gamificación: evaluar retos compuestos (asistencias, consumos, referidos, racha, etc.)
        evaluarLogrosPorAsistencias(membresiaId);
        
        return AsistenciaMapper.mapAsistenciaToAsistenciaDTO(savedAsistencia);
    }
    
    /**
     * Recalcula los puntos acumulados basándose en el conteo total de asistencias
     * 1 punto por cada asistencia registrada
     */
    private void recalcularPuntosAcumulados(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        
        // Contar todas las asistencias del socio
        List<Asistencia> asistencias = asistenciaRepository.findByMembresiaId(membresiaId);
        Integer totalAsistencias = asistencias.size();
        
        // Actualizar puntos acumulados (1 punto por asistencia)
        membresia.setPuntosAcumulados(totalAsistencias);
        membresiaRepository.save(membresia);
    }
    
    /**
     * Evalúa y otorga logros por asistencias basándose en puntos_acumulados
     */
    private void evaluarLogrosPorAsistencias(Integer membresiaId) {
        membresiaLogroService.evaluarLogrosAutomaticamente(membresiaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaDTO> listarAsistenciasBySocio(Integer membresiaId) {
        List<Asistencia> asistencias = asistenciaRepository.findByMembresiaId(membresiaId);
        return asistencias.stream()
                .map(AsistenciaMapper::mapAsistenciaToAsistenciaDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaDTO> listarAsistenciasByClub(Integer clubId) {
        List<Asistencia> asistencias = asistenciaRepository.findByClubId(clubId);
        return asistencias.stream()
                .map(AsistenciaMapper::mapAsistenciaToAsistenciaDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaDTO> listarTodasAsistencias() {
        List<Asistencia> asistencias = asistenciaRepository.findAll();
        return asistencias.stream()
                .map(AsistenciaMapper::mapAsistenciaToAsistenciaDTO)
                .collect(Collectors.toList());
    }
}

