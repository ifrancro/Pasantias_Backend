package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.AsistenciaMapper;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.AsistenciaService;
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
    private MembresiaLogroRepository membresiaLogroRepository;
    @Autowired
    private LogroRepository logroRepository;

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
        
        // Incrementar puntos acumulados (1 punto por asistencia)
        Integer puntosActuales = membresia.getPuntosAcumulados() != null ? membresia.getPuntosAcumulados() : 0;
        membresia.setPuntosAcumulados(puntosActuales + 1);
        
        membresiaRepository.save(membresia);
        
        // Crear la asistencia
        Asistencia asistencia = new Asistencia();
        asistencia.setMembresia(membresia);
        asistencia.setClub(club);
        asistencia.setFechaDia(fechaDia);
        asistencia.setEstado("CONFIRMADA");
        
        Asistencia savedAsistencia = asistenciaRepository.save(asistencia);
        
        // Gamificación opcional: otorgar logros por racha
        otorgarLogrosPorRacha(membresia);
        
        return AsistenciaMapper.mapAsistenciaToAsistenciaDTO(savedAsistencia);
    }
    
    /**
     * Otorga logros cuando la racha alcanza umbrales específicos (3, 7, 14 días)
     */
    private void otorgarLogrosPorRacha(Membresia membresia) {
        if (membresia.getRachaActual() == null) {
            return;
        }
        
        Integer rachaActual = membresia.getRachaActual();
        Integer[] umbrales = {3, 7, 14};
        
        for (Integer umbral : umbrales) {
            if (rachaActual.equals(umbral)) {
                // Buscar logro por tipo de requisito
                String tipoRequisito = "RACHA_" + umbral;
                List<Logro> logros = logroRepository.findAll().stream()
                        .filter(l -> tipoRequisito.equals(l.getTipoRequisito()))
                        .collect(Collectors.toList());
                
                for (Logro logro : logros) {
                    // Verificar si el socio ya tiene este logro
                    Optional<MembresiaLogro> logroExistente = membresiaLogroRepository
                            .findByMembresiaIdAndLogroId(membresia.getId(), logro.getId());
                    
                    if (logroExistente.isEmpty()) {
                        // Otorgar logro
                        MembresiaLogro membresiaLogro = new MembresiaLogro();
                        membresiaLogro.setMembresia(membresia);
                        membresiaLogro.setLogro(logro);
                        membresiaLogroRepository.save(membresiaLogro);
                    }
                }
            }
        }
    }

    @Override
    public List<AsistenciaDTO> listarAsistenciasBySocio(Integer membresiaId) {
        List<Asistencia> asistencias = asistenciaRepository.findByMembresiaId(membresiaId);
        return asistencias.stream()
                .map(AsistenciaMapper::mapAsistenciaToAsistenciaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AsistenciaDTO> listarAsistenciasByClub(Integer clubId) {
        List<Asistencia> asistencias = asistenciaRepository.findByClubId(clubId);
        return asistencias.stream()
                .map(AsistenciaMapper::mapAsistenciaToAsistenciaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AsistenciaDTO> listarTodasAsistencias() {
        List<Asistencia> asistencias = asistenciaRepository.findAll();
        return asistencias.stream()
                .map(AsistenciaMapper::mapAsistenciaToAsistenciaDTO)
                .collect(Collectors.toList());
    }
}

