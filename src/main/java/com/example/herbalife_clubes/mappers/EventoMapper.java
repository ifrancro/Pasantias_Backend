package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.evento.EventoDTO;
import com.example.herbalife_clubes.entities.Evento;

public class EventoMapper {
    public static EventoDTO mapEventoToEventoDTO(Evento evento) {
        EventoDTO dto = new EventoDTO();
        dto.setId(evento.getId());
        dto.setHubId(evento.getHub() != null ? evento.getHub().getId() : null);
        dto.setHubNombre(evento.getHub() != null ? evento.getHub().getNombre() : null);
        dto.setClubId(evento.getClub() != null ? evento.getClub().getId() : null);
        dto.setClubNombre(evento.getClub() != null ? evento.getClub().getNombreClub() : null);
        dto.setNombre(evento.getNombre());
        dto.setFechaEvento(evento.getFechaEvento());
        dto.setDescripcion(evento.getDescripcion());
        return dto;
    }

    public static Evento mapEventoDTOToEvento(EventoDTO dto) {
        Evento evento = new Evento();
        evento.setId(dto.getId());
        evento.setNombre(dto.getNombre());
        evento.setFechaEvento(dto.getFechaEvento());
        evento.setDescripcion(dto.getDescripcion());
        return evento;
    }
}

