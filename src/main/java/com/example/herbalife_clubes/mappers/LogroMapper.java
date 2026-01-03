package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.entities.Logro;

public class LogroMapper {
    public static LogroDTO mapLogroToLogroDTO(Logro logro) {
        LogroDTO dto = new LogroDTO();
        dto.setId(logro.getId());
        dto.setNombre(logro.getNombre());
        dto.setDescripcion(logro.getDescripcion());
        dto.setIconoUrl(logro.getIconoUrl());
        dto.setTipoRequisito(logro.getTipoRequisito());
        return dto;
    }

    public static Logro mapLogroDTOToLogro(LogroDTO dto) {
        Logro logro = new Logro();
        logro.setId(dto.getId());
        logro.setNombre(dto.getNombre());
        logro.setDescripcion(dto.getDescripcion());
        logro.setIconoUrl(dto.getIconoUrl());
        logro.setTipoRequisito(dto.getTipoRequisito());
        return logro;
    }
}

