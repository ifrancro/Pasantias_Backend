package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.membresia.ArbolReferidosDTO;
import com.example.herbalife_clubes.dtos.membresia.EstadoComboDTO;
import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.NivelSocio;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.MembresiaMapper;
import com.example.herbalife_clubes.repositories.AsistenciaRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.NivelSocioRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ComboConsumoService;
import com.example.herbalife_clubes.services.MembresiaLogroService;
import com.example.herbalife_clubes.services.MembresiaService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MembresiaServiceImpl implements MembresiaService {
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private NivelSocioRepository nivelSocioRepository;
    @Autowired
    private AsistenciaRepository asistenciaRepository;
    @Autowired
    private MembresiaLogroService membresiaLogroService;
    @Autowired
    private ComboConsumoService comboConsumoService;

    @Override
    public MembresiaDTO createMembresia(MembresiaDTO membresiaDTO, Integer usuarioId, Integer clubId, Integer nivelId, Integer referidoPorMembresiaId) {
        // Validar que el usuario no tenga ya una membresía
        if (membresiaRepository.findByUsuarioId(usuarioId).isPresent()) {
            throw new IllegalArgumentException("El usuario ya tiene una membresía activa");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        
        Membresia membresia = MembresiaMapper.mapMembresiaDTOToMembresia(membresiaDTO);
        membresia.setUsuario(usuario);
        membresia.setClub(club);
        
        if (nivelId != null) {
            NivelSocio nivel = nivelSocioRepository.findById(nivelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Nivel socio no encontrado con id: " + nivelId));
            membresia.setNivel(nivel);
        }
        
        // Establecer referido por membresía si se proporciona
        if (referidoPorMembresiaId != null) {
            Membresia referidoPorMembresia = membresiaRepository.findById(referidoPorMembresiaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Membresía referente no encontrada con id: " + referidoPorMembresiaId));
            if (!"ACTIVA".equals(referidoPorMembresia.getEstado())) {
                throw new IllegalArgumentException("La membresía referente no está ACTIVA");
            }
            if (referidoPorMembresia.getUsuario() != null && referidoPorMembresia.getUsuario().getId().equals(usuarioId)) {
                throw new IllegalArgumentException("Un socio no puede referirse a sí mismo");
            }
            if (esAncestroDelUsuario(referidoPorMembresia, usuarioId)) {
                throw new IllegalArgumentException("No se puede crear un ciclo de referidos");
            }
            membresia.setReferidoPorMembresia(referidoPorMembresia);
        }
        
        // Generar número de socio único si no se proporciona
        if (membresia.getNumeroSocio() == null || membresia.getNumeroSocio().isBlank()) {
            membresia.setNumeroSocio("SOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        
        if (membresia.getPuntosAcumulados() == null) {
            membresia.setPuntosAcumulados(0);
        }
        
        if (membresia.getEstado() == null) {
            membresia.setEstado("ACTIVA");
        }
        
        Membresia savedMembresia = membresiaRepository.save(membresia);
        membresiaLogroService.evaluarLogrosAutomaticamente(savedMembresia.getId());
        if (referidoPorMembresiaId != null) {
            membresiaLogroService.evaluarLogrosAutomaticamente(referidoPorMembresiaId);
        }
        return MembresiaMapper.mapMembresiaToMembresiaDTO(savedMembresia);
    }

    @Override
    public MembresiaDTO getMembresia(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        return enriquecerConEstadoCombo(MembresiaMapper.mapMembresiaToMembresiaDTO(membresia), membresiaId);
    }

    @Override
    public MembresiaDTO getMembresiaByUsuario(Integer usuarioId) {
        Membresia membresia = membresiaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada para el usuario: " + usuarioId));
        return enriquecerConEstadoCombo(MembresiaMapper.mapMembresiaToMembresiaDTO(membresia), membresia.getId());
    }

    @Override
    public List<MembresiaDTO> getMembresiasByClub(Integer clubId) {
        List<Membresia> membresias = membresiaRepository.findByClubId(clubId);
        return membresias.stream()
                .map(m -> enriquecerConEstadoCombo(MembresiaMapper.mapMembresiaToMembresiaDTO(m), m.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public EstadoComboDTO getEstadoCombo(Integer membresiaId) {
        return comboConsumoService.obtenerEstadoCombo(membresiaId);
    }

    private MembresiaDTO enriquecerConEstadoCombo(MembresiaDTO dto, Integer membresiaId) {
        if (dto == null || membresiaId == null) {
            return dto;
        }
        EstadoComboDTO estado = comboConsumoService.obtenerEstadoCombo(membresiaId);
        dto.setHaConsumidoCombo(estado.isHaConsumidoCombo());
        return dto;
    }

    @Override
    public MembresiaDTO cambiarEstado(Integer membresiaId, String estado) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        membresia.setEstado(estado);
        Membresia updatedMembresia = membresiaRepository.save(membresia);
        return MembresiaMapper.mapMembresiaToMembresiaDTO(updatedMembresia);
    }

    @Override
    public MembresiaDTO cambiarNivel(Integer membresiaId, Integer nivelId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        NivelSocio nivel = nivelSocioRepository.findById(nivelId)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel socio no encontrado con id: " + nivelId));
        membresia.setNivel(nivel);
        Membresia updatedMembresia = membresiaRepository.save(membresia);
        return MembresiaMapper.mapMembresiaToMembresiaDTO(updatedMembresia);
    }

    @Override
    public MembresiaDTO actualizarPuntos(Integer membresiaId, Integer puntos) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        membresia.setPuntosAcumulados(puntos);
        Membresia updatedMembresia = membresiaRepository.save(membresia);
        return MembresiaMapper.mapMembresiaToMembresiaDTO(updatedMembresia);
    }

    @Override
    public MembresiaDTO recalcularPuntosPorAsistencias(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        
        // Contar todas las asistencias del socio
        int totalAsistencias = asistenciaRepository.findByMembresiaId(membresiaId).size();
        
        // Actualizar puntos acumulados (1 punto por asistencia)
        membresia.setPuntosAcumulados(totalAsistencias);
        Membresia updatedMembresia = membresiaRepository.save(membresia);
        return MembresiaMapper.mapMembresiaToMembresiaDTO(updatedMembresia);
    }

    @Override
    public List<MembresiaDTO> buscarMiembrosGlobal(String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<Membresia> membresias;
        if (query != null && !query.isBlank()) {
            membresias = membresiaRepository.buscarMiembrosGlobal(query, pageRequest);
        } else {
            membresias = membresiaRepository.findAllActiveWithUsuario(pageRequest);
        }
        return membresias.stream()
                .map(MembresiaMapper::mapMembresiaToMembresiaDTO)
                .collect(Collectors.toList());
    }

    private boolean esAncestroDelUsuario(Membresia referente, Integer usuarioId) {
        Set<Integer> visitados = new HashSet<>();
        Membresia actual = referente;
        while (actual != null) {
            if (!visitados.add(actual.getId())) {
                return true;
            }
            if (actual.getUsuario() != null && actual.getUsuario().getId().equals(usuarioId)) {
                return true;
            }
            actual = actual.getReferidoPorMembresia();
        }
        return false;
    }

    @Override
    public ArbolReferidosDTO getArbolReferidos(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        
        return construirArbolRecursivo(membresia);
    }

    private ArbolReferidosDTO construirArbolRecursivo(Membresia membresia) {
        String nombreCompleto = membresia.getUsuario() != null ?
                membresia.getUsuario().getNombre() + " " + membresia.getUsuario().getApellido() : "N/A";
        String clubNombre = membresia.getClub() != null ? membresia.getClub().getNombreClub() : null;

        ArbolReferidosDTO nodo = new ArbolReferidosDTO(
                membresia.getId(),
                membresia.getNumeroSocio(),
                nombreCompleto,
                membresia.getPuntosAcumulados(),
                membresia.getEstado(),
                clubNombre
        );

        List<Membresia> referidosDirectos = membresiaRepository.findByReferidoPorMembresiaId(membresia.getId());

        for (Membresia referido : referidosDirectos) {
            ArbolReferidosDTO nodoReferido = construirArbolRecursivo(referido);
            nodo.agregarReferido(nodoReferido);
        }

        return nodo;
    }
}

