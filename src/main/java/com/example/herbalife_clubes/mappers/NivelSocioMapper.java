package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.nivelsocio.NivelSocioDTO;
import com.example.herbalife_clubes.entities.NivelSocio;

public class NivelSocioMapper {
    public static NivelSocioDTO mapNivelSocioToNivelSocioDTO(NivelSocio nivel) {
        NivelSocioDTO dto = new NivelSocioDTO();
        dto.setId(nivel.getId());
        dto.setNombre(nivel.getNombre());
        dto.setVisitasRequeridas(nivel.getVisitasRequeridas());
        dto.setDescripcionBeneficios(nivel.getDescripcionBeneficios());
        return dto;
    }

    public static NivelSocio mapNivelSocioDTOToNivelSocio(NivelSocioDTO dto) {
        NivelSocio nivel = new NivelSocio();
        nivel.setId(dto.getId());
        nivel.setNombre(dto.getNombre());
        nivel.setVisitasRequeridas(dto.getVisitasRequeridas());
        nivel.setDescripcionBeneficios(dto.getDescripcionBeneficios());
        return nivel;
    }
}

