package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.entities.Club;

public class ClubMapper {
    public static ClubDTO mapClubToClubDTO(Club club) {
        ClubDTO dto = new ClubDTO();
        dto.setId(club.getId());
        dto.setHubId(club.getHub() != null ? club.getHub().getId() : null);
        dto.setHubNombre(club.getHub() != null ? club.getHub().getNombre() : null);
        dto.setAnfitrionId(club.getAnfitrion() != null ? club.getAnfitrion().getId() : null);
        dto.setAnfitrionNombre(club.getAnfitrion() != null ? 
                club.getAnfitrion().getNombre() + " " + club.getAnfitrion().getApellido() : null);
        dto.setNombreClub(club.getNombreClub());
        dto.setDireccion(club.getDireccion());
        dto.setHorario(club.getHorario());
        dto.setLat(club.getLat());
        dto.setLng(club.getLng());
        dto.setEstado(club.getEstado());
        dto.setCreatedAt(club.getCreatedAt());
        return dto;
    }

    public static Club mapClubDTOToClub(ClubDTO dto) {
        Club club = new Club();
        club.setId(dto.getId());
        club.setNombreClub(dto.getNombreClub());
        club.setDireccion(dto.getDireccion());
        club.setHorario(dto.getHorario());
        club.setLat(dto.getLat());
        club.setLng(dto.getLng());
        club.setEstado(dto.getEstado());
        return club;
    }
}

