package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.NivelSocio;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.MembresiaMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.NivelSocioRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.MembresiaService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Override
    public MembresiaDTO createMembresia(MembresiaDTO membresiaDTO, Integer usuarioId, Integer clubId, Integer nivelId) {
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
        return MembresiaMapper.mapMembresiaToMembresiaDTO(savedMembresia);
    }

    @Override
    public MembresiaDTO getMembresia(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        return MembresiaMapper.mapMembresiaToMembresiaDTO(membresia);
    }

    @Override
    public MembresiaDTO getMembresiaByUsuario(Integer usuarioId) {
        Membresia membresia = membresiaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada para el usuario: " + usuarioId));
        return MembresiaMapper.mapMembresiaToMembresiaDTO(membresia);
    }

    @Override
    public List<MembresiaDTO> getMembresiasByClub(Integer clubId) {
        List<Membresia> membresias = membresiaRepository.findByClubId(clubId);
        return membresias.stream()
                .map(MembresiaMapper::mapMembresiaToMembresiaDTO)
                .collect(Collectors.toList());
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
}

