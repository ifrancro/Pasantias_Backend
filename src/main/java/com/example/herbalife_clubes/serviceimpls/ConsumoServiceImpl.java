package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.consumo.ConsumoDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.ConsumoMapper;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.ConsumoService;
import com.example.herbalife_clubes.services.MembresiaLogroService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ConsumoServiceImpl implements ConsumoService {
    @Autowired
    private ConsumoRepository consumoRepository;
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private AsistenciaRepository asistenciaRepository;
    @Autowired
    private MembresiaLogroService membresiaLogroService;

    @Override
    @Transactional
    public ConsumoDTO registrarConsumo(Integer membresiaId, Integer clubId, Integer productoId,
                                        String descripcion, Integer cantidad, BigDecimal precioReferencial,
                                        Integer asistenciaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        
        Consumo consumo = new Consumo();
        consumo.setMembresia(membresia);
        consumo.setClub(club);
        consumo.setDescripcion(descripcion);
        // Compatibilidad: antes existía productoId/cantidad/precioReferencial. Ahora NO se persisten.
        // Si vienen, los agregamos a la descripción para no perder contexto.
        if (productoId != null || cantidad != null || precioReferencial != null) {
            String extra = " [compat productoId=" + productoId + ", cantidad=" + cantidad + ", ref=" + precioReferencial + "]";
            consumo.setDescripcion((descripcion != null ? descripcion : "") + extra);
        }
        
        // Asistencia opcional
        if (asistenciaId != null) {
            Asistencia asistencia = asistenciaRepository.findById(asistenciaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Asistencia no encontrada con id: " + asistenciaId));
            consumo.setAsistencia(asistencia);
        }
        
        Consumo savedConsumo = consumoRepository.save(consumo);
        membresiaLogroService.evaluarLogrosAutomaticamente(membresiaId);
        return ConsumoMapper.mapConsumoToConsumoDTO(savedConsumo);
    }

    @Override
    public List<ConsumoDTO> getHistorialBySocio(Integer membresiaId) {
        List<Consumo> consumos = consumoRepository.findByMembresiaId(membresiaId);
        return consumos.stream()
                .map(ConsumoMapper::mapConsumoToConsumoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConsumoDTO> getHistorialByClub(Integer clubId) {
        List<Consumo> consumos = consumoRepository.findByClubId(clubId);
        return consumos.stream()
                .map(ConsumoMapper::mapConsumoToConsumoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConsumoDTO> getHistorialByAsistencia(Integer asistenciaId) {
        // Necesitamos agregar este método al repositorio
        List<Consumo> consumos = consumoRepository.findAll().stream()
                .filter(c -> c.getAsistencia() != null && c.getAsistencia().getId().equals(asistenciaId))
                .collect(Collectors.toList());
        return consumos.stream()
                .map(ConsumoMapper::mapConsumoToConsumoDTO)
                .collect(Collectors.toList());
    }
}

