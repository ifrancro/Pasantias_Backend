package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.entities.Logro;

public class LogroMapper {
    public static LogroDTO mapLogroToLogroDTO(Logro logro) {
        LogroDTO dto = new LogroDTO();
        dto.setId(logro.getId());
        dto.setClubCreadorId(logro.getClubCreador() != null ? logro.getClubCreador().getId() : null);
        dto.setClubCreadorNombre(logro.getClubCreador() != null ? logro.getClubCreador().getNombreClub() : null);
        dto.setNombre(logro.getNombre());
        dto.setDescripcion(logro.getDescripcion());
        dto.setIconoUrl(logro.getIconoUrl());
        dto.setTipoRequisito(logro.getTipoRequisito());
        dto.setTipoMetrica(logro.getTipoMetrica());
        dto.setMetaCantidad(logro.getMetaCantidad());
        dto.setPuntosRecompensa(logro.getPuntosRecompensa());
        dto.setFechaInicio(logro.getFechaInicio());
        dto.setFechaFin(logro.getFechaFin());
        dto.setEstadoAprobacion(logro.getEstadoAprobacion());
        return dto;
    }

    public static Logro mapLogroDTOToLogro(LogroDTO dto) {
        Logro logro = new Logro();
        logro.setId(dto.getId());
        // clubCreador se establece en el servicio, no desde el DTO
        logro.setNombre(dto.getNombre());
        logro.setDescripcion(dto.getDescripcion());
        logro.setIconoUrl(dto.getIconoUrl());
        logro.setTipoRequisito(dto.getTipoRequisito());
        logro.setTipoMetrica(dto.getTipoMetrica());
        logro.setMetaCantidad(dto.getMetaCantidad());
        logro.setPuntosRecompensa(dto.getPuntosRecompensa());
        logro.setFechaInicio(dto.getFechaInicio());
        logro.setFechaFin(dto.getFechaFin());
        // estadoAprobacion se establece en el servicio según el rol
        return logro;
    }
}

