package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.fotoclub.FotoClubDTO;
import com.example.herbalife_clubes.entities.FotoClub;

public class FotoClubMapper {
    public static FotoClubDTO mapFotoClubToFotoClubDTO(FotoClub foto) {
        FotoClubDTO dto = new FotoClubDTO();
        dto.setId(foto.getId());
        dto.setClubId(foto.getClub() != null ? foto.getClub().getId() : null);
        dto.setClubNombre(foto.getClub() != null ? foto.getClub().getNombreClub() : null);
        dto.setUrlFoto(foto.getUrlFoto());
        dto.setTipo(foto.getTipo());
        return dto;
    }
}

