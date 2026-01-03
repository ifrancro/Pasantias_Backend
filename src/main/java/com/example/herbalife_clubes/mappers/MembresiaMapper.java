package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;
import com.example.herbalife_clubes.entities.Membresia;

public class MembresiaMapper {
    public static MembresiaDTO mapMembresiaToMembresiaDTO(Membresia membresia) {
        MembresiaDTO dto = new MembresiaDTO();
        dto.setId(membresia.getId());
        dto.setUsuarioId(membresia.getUsuario() != null ? membresia.getUsuario().getId() : null);
        dto.setUsuarioNombre(membresia.getUsuario() != null ? 
                membresia.getUsuario().getNombre() + " " + membresia.getUsuario().getApellido() : null);
        dto.setClubId(membresia.getClub() != null ? membresia.getClub().getId() : null);
        dto.setClubNombre(membresia.getClub() != null ? membresia.getClub().getNombreClub() : null);
        dto.setNivelId(membresia.getNivel() != null ? membresia.getNivel().getId() : null);
        dto.setNivelNombre(membresia.getNivel() != null ? membresia.getNivel().getNombre() : null);
        dto.setNumeroSocio(membresia.getNumeroSocio());
        dto.setPuntosAcumulados(membresia.getPuntosAcumulados());
        dto.setReferidoPor(membresia.getReferidoPor());
        dto.setComoConocio(membresia.getComoConocio());
        dto.setFechaRegistro(membresia.getFechaRegistro());
        dto.setEstado(membresia.getEstado());
        return dto;
    }

    public static Membresia mapMembresiaDTOToMembresia(MembresiaDTO dto) {
        Membresia membresia = new Membresia();
        membresia.setId(dto.getId());
        membresia.setNumeroSocio(dto.getNumeroSocio());
        membresia.setPuntosAcumulados(dto.getPuntosAcumulados());
        membresia.setReferidoPor(dto.getReferidoPor());
        membresia.setComoConocio(dto.getComoConocio());
        membresia.setEstado(dto.getEstado());
        return membresia;
    }
}

