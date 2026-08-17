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
        dto.setReferidoPorMembresiaId(membresia.getReferidoPorMembresia() != null ? membresia.getReferidoPorMembresia().getId() : null);
        dto.setReferidoPorMembresiaNombre(membresia.getReferidoPorMembresia() != null && membresia.getReferidoPorMembresia().getUsuario() != null ?
                membresia.getReferidoPorMembresia().getUsuario().getNombre() + " " + membresia.getReferidoPorMembresia().getUsuario().getApellido() : null);
        dto.setComoConocio(membresia.getComoConocio());
        dto.setEsClientePreferenteODistribuidor(membresia.getEsClientePreferenteODistribuidor());
        dto.setFechaRegistro(membresia.getFechaRegistro());
        dto.setEstado(membresia.getEstado());
        dto.setRachaActual(membresia.getRachaActual());
        dto.setRachaMaxima(membresia.getRachaMaxima());
        dto.setUltimaAsistenciaDia(membresia.getUltimaAsistenciaDia());
        return dto;
    }

    public static Membresia mapMembresiaDTOToMembresia(MembresiaDTO dto) {
        Membresia membresia = new Membresia();
        membresia.setId(dto.getId());
        membresia.setNumeroSocio(dto.getNumeroSocio());
        membresia.setPuntosAcumulados(dto.getPuntosAcumulados());
        // referidoPorMembresia se establece en el servicio, no desde el DTO
        membresia.setComoConocio(dto.getComoConocio());
        membresia.setEsClientePreferenteODistribuidor(dto.getEsClientePreferenteODistribuidor());
        membresia.setEstado(dto.getEstado());
        return membresia;
    }
}

