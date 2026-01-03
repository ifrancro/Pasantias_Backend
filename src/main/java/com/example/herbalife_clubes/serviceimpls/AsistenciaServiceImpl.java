package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.entities.Asistencia;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.AsistenciaMapper;
import com.example.herbalife_clubes.repositories.AsistenciaRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.services.AsistenciaService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
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

    @Override
    @Transactional
    public AsistenciaDTO registrarAsistencia(Integer membresiaId, Integer clubId, String qrClub) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        
        // Validar que el club visitado pertenezca al mismo HUB que el club de la membresía
        Club clubMembresia = membresia.getClub();
        if (clubMembresia == null || clubMembresia.getHub() == null) {
            throw new IllegalArgumentException("La membresía no tiene un club válido asociado");
        }
        
        if (club.getHub() == null) {
            throw new IllegalArgumentException("El club visitado no tiene un HUB válido asociado");
        }
        
        if (!clubMembresia.getHub().getId().equals(club.getHub().getId())) {
            throw new IllegalArgumentException("El club visitado no pertenece al mismo HUB que el club de la membresía");
        }
        
        // Validar unique constraint: no puede haber dos asistencias del mismo socio en el mismo club el mismo día
        LocalDate fechaDia = LocalDate.now();
        if (asistenciaRepository.findByMembresiaIdAndClubIdAndFechaDia(membresiaId, clubId, fechaDia).isPresent()) {
            throw new IllegalArgumentException("Ya existe una asistencia registrada para este socio en este club hoy");
        }
        
        // Crear la asistencia
        Asistencia asistencia = new Asistencia();
        asistencia.setMembresia(membresia);
        asistencia.setClub(club);
        asistencia.setFechaDia(fechaDia);
        asistencia.setEstado("CONFIRMADA");
        
        Asistencia savedAsistencia = asistenciaRepository.save(asistencia);
        return AsistenciaMapper.mapAsistenciaToAsistenciaDTO(savedAsistencia);
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

