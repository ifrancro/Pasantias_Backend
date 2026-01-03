package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.entities.Asistencia;

public class AsistenciaMapper {
    public static AsistenciaDTO mapAsistenciaToAsistenciaDTO(Asistencia asistencia) {
        AsistenciaDTO dto = new AsistenciaDTO();
        dto.setId(asistencia.getId());
        dto.setMembresiaId(asistencia.getMembresia() != null ? asistencia.getMembresia().getId() : null);
        dto.setMembresiaNumeroSocio(asistencia.getMembresia() != null ? asistencia.getMembresia().getNumeroSocio() : null);
        dto.setClubId(asistencia.getClub() != null ? asistencia.getClub().getId() : null);
        dto.setClubNombre(asistencia.getClub() != null ? asistencia.getClub().getNombreClub() : null);
        dto.setFechaHora(asistencia.getFechaHora());
        dto.setFechaDia(asistencia.getFechaDia());
        dto.setEstado(asistencia.getEstado());
        return dto;
    }
}

