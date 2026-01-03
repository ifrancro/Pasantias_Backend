package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.hub.HubDTO;
import com.example.herbalife_clubes.entities.Hub;

public class HubMapper {
    public static HubDTO mapHubToHubDTO(Hub hub) {
        HubDTO dto = new HubDTO();
        dto.setId(hub.getId());
        dto.setAdminId(hub.getAdmin() != null ? hub.getAdmin().getId() : null);
        dto.setAdminNombre(hub.getAdmin() != null ? 
                hub.getAdmin().getNombre() + " " + hub.getAdmin().getApellido() : null);
        dto.setNombre(hub.getNombre());
        dto.setDescripcion(hub.getDescripcion());
        dto.setEstado(hub.getEstado());
        dto.setCreatedAt(hub.getCreatedAt());
        return dto;
    }

    public static Hub mapHubDTOToHub(HubDTO dto) {
        Hub hub = new Hub();
        hub.setId(dto.getId());
        hub.setNombre(dto.getNombre());
        hub.setDescripcion(dto.getDescripcion());
        hub.setEstado(dto.getEstado());
        return hub;
    }
}

