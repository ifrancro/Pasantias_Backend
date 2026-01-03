package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.nivelsocio.NivelSocioDTO;
import com.example.herbalife_clubes.entities.NivelSocio;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.NivelSocioMapper;
import com.example.herbalife_clubes.repositories.NivelSocioRepository;
import com.example.herbalife_clubes.services.NivelSocioService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class NivelSocioServiceImpl implements NivelSocioService {
    @Autowired
    private NivelSocioRepository nivelSocioRepository;

    @Override
    public NivelSocioDTO createNivelSocio(NivelSocioDTO nivelSocioDTO) {
        NivelSocio nivel = NivelSocioMapper.mapNivelSocioDTOToNivelSocio(nivelSocioDTO);
        NivelSocio savedNivel = nivelSocioRepository.save(nivel);
        return NivelSocioMapper.mapNivelSocioToNivelSocioDTO(savedNivel);
    }

    @Override
    public NivelSocioDTO updateNivelSocio(Integer nivelId, NivelSocioDTO nivelSocioDTO) {
        NivelSocio nivel = nivelSocioRepository.findById(nivelId)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel socio no encontrado con id: " + nivelId));
        
        nivel.setNombre(nivelSocioDTO.getNombre());
        nivel.setVisitasRequeridas(nivelSocioDTO.getVisitasRequeridas());
        nivel.setDescripcionBeneficios(nivelSocioDTO.getDescripcionBeneficios());
        
        NivelSocio updatedNivel = nivelSocioRepository.save(nivel);
        return NivelSocioMapper.mapNivelSocioToNivelSocioDTO(updatedNivel);
    }

    @Override
    public String deleteNivelSocio(Integer nivelId) {
        NivelSocio nivel = nivelSocioRepository.findById(nivelId)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel socio no encontrado con id: " + nivelId));
        nivelSocioRepository.delete(nivel);
        return "Nivel socio ha sido eliminado";
    }

    @Override
    public NivelSocioDTO getNivelSocio(Integer nivelId) {
        NivelSocio nivel = nivelSocioRepository.findById(nivelId)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel socio no encontrado con id: " + nivelId));
        return NivelSocioMapper.mapNivelSocioToNivelSocioDTO(nivel);
    }

    @Override
    public List<NivelSocioDTO> getNivelesSocio() {
        List<NivelSocio> niveles = nivelSocioRepository.findAll();
        return niveles.stream()
                .map(NivelSocioMapper::mapNivelSocioToNivelSocioDTO)
                .collect(Collectors.toList());
    }
}

