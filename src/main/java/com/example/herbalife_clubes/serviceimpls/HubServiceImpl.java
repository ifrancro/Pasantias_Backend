package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.hub.HubDTO;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.HubMapper;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.HubService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class HubServiceImpl implements HubService {
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public HubDTO createHub(HubDTO hubDTO, Integer adminId) {
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado con id: " + adminId));
        
        Hub hub = HubMapper.mapHubDTOToHub(hubDTO);
        hub.setAdmin(admin);
        if (hub.getEstado() == null) {
            hub.setEstado("ACTIVO");
        }
        
        Hub savedHub = hubRepository.save(hub);
        return HubMapper.mapHubToHubDTO(savedHub);
    }

    @Override
    public HubDTO updateHub(Integer hubId, HubDTO hubDTO) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
        
        hub.setNombre(hubDTO.getNombre());
        hub.setDescripcion(hubDTO.getDescripcion());
        hub.setEstado(hubDTO.getEstado());
        
        Hub updatedHub = hubRepository.save(hub);
        return HubMapper.mapHubToHubDTO(updatedHub);
    }

    @Override
    public String deleteHub(Integer hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
        hubRepository.delete(hub);
        return "Hub ha sido eliminado";
    }

    @Override
    public HubDTO getHub(Integer hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
        return HubMapper.mapHubToHubDTO(hub);
    }

    @Override
    public List<HubDTO> getAllHubs() {
        List<Hub> hubs = hubRepository.findAll();
        return hubs.stream()
                .map(HubMapper::mapHubToHubDTO)
                .collect(Collectors.toList());
    }

    @Override
    public HubDTO activarHub(Integer hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
        hub.setEstado("ACTIVO");
        Hub updatedHub = hubRepository.save(hub);
        return HubMapper.mapHubToHubDTO(updatedHub);
    }

    @Override
    public HubDTO inactivarHub(Integer hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
        hub.setEstado("INACTIVO");
        Hub updatedHub = hubRepository.save(hub);
        return HubMapper.mapHubToHubDTO(updatedHub);
    }
}

