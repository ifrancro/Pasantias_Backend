package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.dtos.logro.RequisitoLogroDTO;
import com.example.herbalife_clubes.entities.Logro;
import com.example.herbalife_clubes.entities.RequisitoLogro;

import java.util.ArrayList;
import java.util.List;

public class LogroMapper {
    public static LogroDTO mapLogroToLogroDTO(Logro logro) {
        LogroDTO dto = new LogroDTO();
        dto.setId(logro.getId());
        dto.setClubCreadorId(logro.getClubCreador() != null ? logro.getClubCreador().getId() : null);
        dto.setClubCreadorNombre(logro.getClubCreador() != null ? logro.getClubCreador().getNombreClub() : null);
        dto.setNombre(logro.getNombre());
        dto.setDescripcion(logro.getDescripcion());
        dto.setIconoUrl(logro.getIconoUrl());
        dto.setPuntosRecompensa(logro.getPuntosRecompensa());
        dto.setFechaInicio(logro.getFechaInicio());
        dto.setFechaFin(logro.getFechaFin());
        dto.setEstadoAprobacion(logro.getEstadoAprobacion());
        if (logro.getRequisitos() != null) {
            List<RequisitoLogroDTO> reqs = new ArrayList<>();
            for (RequisitoLogro r : logro.getRequisitos()) {
                reqs.add(new RequisitoLogroDTO(r.getId(), r.getTipoMetrica(), r.getCantidadEsperada()));
            }
            dto.setRequisitos(reqs);
        }
        return dto;
    }

    public static Logro mapLogroDTOToLogro(LogroDTO dto) {
        Logro logro = new Logro();
        logro.setId(dto.getId());
        logro.setNombre(dto.getNombre());
        logro.setDescripcion(dto.getDescripcion());
        logro.setIconoUrl(dto.getIconoUrl());
        logro.setPuntosRecompensa(dto.getPuntosRecompensa());
        logro.setFechaInicio(dto.getFechaInicio());
        logro.setFechaFin(dto.getFechaFin());
        logro.setRequisitos(new ArrayList<>());
        if (dto.getRequisitos() != null) {
            for (RequisitoLogroDTO rd : dto.getRequisitos()) {
                RequisitoLogro r = new RequisitoLogro();
                r.setId(rd.getId());
                r.setTipoMetrica(rd.getTipoMetrica() != null ? rd.getTipoMetrica().trim().toUpperCase() : null);
                r.setCantidadEsperada(rd.getCantidadEsperada());
                r.setLogro(logro);
                logro.getRequisitos().add(r);
            }
        }
        return logro;
    }
}
