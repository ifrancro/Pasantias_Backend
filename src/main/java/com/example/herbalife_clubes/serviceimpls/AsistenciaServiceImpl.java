package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.asistencias.AttendanceLocationValidator;
import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.AsistenciaMapper;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.AsistenciaService;
import com.example.herbalife_clubes.services.ComboConsumoService;
import com.example.herbalife_clubes.services.MembresiaLogroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AsistenciaServiceImpl implements AsistenciaService {

    private static final String MSG_MEMBRESIA_FORBIDDEN = "No tienes permisos para usar esta membresía.";

    @Autowired
    private AsistenciaRepository asistenciaRepository;
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private MembresiaLogroService membresiaLogroService;
    @Autowired
    private ComboConsumoService comboConsumoService;

    @Value("${attendance.max-distance-meters:100}")
    private double maxDistanceMeters;

    @Override
    @Transactional
    public AsistenciaDTO registrarAsistencia(
            Integer membresiaId,
            Integer clubId,
            String qrClub,
            Double latitud,
            Double longitud,
            Double precisionMetros) {
        Usuario usuarioAutenticado = requireAuthenticatedUsuario();

        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        assertMembresiaOwnedByUsuario(membresia, usuarioAutenticado);

        if (membresia.getEstado() == null || !membresia.getEstado().equals("ACTIVA")) {
            throw new IllegalArgumentException("La membresía no está activa. Estado actual: " + membresia.getEstado());
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));

        if (club.getEstado() == null || (!club.getEstado().equals("APROBADO") && !club.getEstado().equals("ACTIVO"))) {
            throw new IllegalArgumentException("El club no está activo. Estado actual: " + club.getEstado());
        }

        AttendanceLocationValidator.validateRequestCoordinates(latitud, longitud, precisionMetros);
        AttendanceLocationValidator.validateClubCoordinates(club);
        AttendanceLocationValidator.validateWithinRange(latitud, longitud, club, maxDistanceMeters);

        comboConsumoService.validarComboConsumidoAntesDeAsistencia(membresiaId);

        LocalDate fechaDia = LocalDate.now();
        Optional<Asistencia> asistenciaExistente = asistenciaRepository.findByMembresiaIdAndFechaDia(membresiaId, fechaDia);
        if (asistenciaExistente.isPresent()) {
            throw new IllegalArgumentException("Ya existe una asistencia registrada para este socio hoy. No se puede registrar asistencia duplicada en el mismo día.");
        }

        LocalDate ultimaAsistencia = membresia.getUltimaAsistenciaDia();
        LocalDate ayer = fechaDia.minusDays(1);

        if (ultimaAsistencia == null) {
            membresia.setRachaActual(1);
        } else if (ultimaAsistencia.equals(fechaDia)) {
            throw new IllegalArgumentException("Ya existe una asistencia registrada para este socio hoy");
        } else if (ultimaAsistencia.equals(ayer)) {
            membresia.setRachaActual((membresia.getRachaActual() != null ? membresia.getRachaActual() : 0) + 1);
        } else {
            membresia.setRachaActual(1);
        }

        if (membresia.getRachaMaxima() == null || membresia.getRachaActual() > membresia.getRachaMaxima()) {
            membresia.setRachaMaxima(membresia.getRachaActual());
        }

        membresia.setUltimaAsistenciaDia(fechaDia);
        membresiaRepository.save(membresia);

        Asistencia asistencia = new Asistencia();
        asistencia.setMembresia(membresia);
        asistencia.setClub(club);
        asistencia.setFechaDia(fechaDia);
        asistencia.setEstado("CONFIRMADA");

        Asistencia savedAsistencia = asistenciaRepository.save(asistencia);

        recalcularPuntosAcumulados(membresiaId);

        membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        evaluarLogrosPorAsistencias(membresiaId);

        return AsistenciaMapper.mapAsistenciaToAsistenciaDTO(savedAsistencia);
    }

    private Usuario requireAuthenticatedUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado"));
    }

    private void assertMembresiaOwnedByUsuario(Membresia membresia, Usuario usuario) {
        if (membresia.getUsuario() == null || usuario.getId() == null
                || !usuario.getId().equals(membresia.getUsuario().getId())) {
            throw new AccessDeniedException(MSG_MEMBRESIA_FORBIDDEN);
        }
    }

    private void recalcularPuntosAcumulados(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        List<Asistencia> asistencias = asistenciaRepository.findByMembresiaId(membresiaId);
        Integer totalAsistencias = asistencias.size();

        membresia.setPuntosAcumulados(totalAsistencias);
        membresiaRepository.save(membresia);
    }

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
