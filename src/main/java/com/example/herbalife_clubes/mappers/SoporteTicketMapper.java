package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.soporteticket.SoporteTicketDTO;
import com.example.herbalife_clubes.entities.SoporteTicket;

public class SoporteTicketMapper {
    public static SoporteTicketDTO mapSoporteTicketToSoporteTicketDTO(SoporteTicket ticket) {
        SoporteTicketDTO dto = new SoporteTicketDTO();
        dto.setId(ticket.getId());
        dto.setUsuarioId(ticket.getUsuario() != null ? ticket.getUsuario().getId() : null);
        dto.setUsuarioNombre(ticket.getUsuario() != null ? 
                ticket.getUsuario().getNombre() + " " + ticket.getUsuario().getApellido() : null);
        dto.setTipoSolicitud(ticket.getTipoSolicitud());
        dto.setAsunto(ticket.getAsunto());
        dto.setMensaje(ticket.getMensaje());
        dto.setEstado(ticket.getEstado());
        dto.setRespuestaAdmin(ticket.getRespuestaAdmin());
        dto.setFechaCreacion(ticket.getFechaCreacion());
        dto.setFechaRespuesta(ticket.getFechaRespuesta());
        return dto;
    }

    public static SoporteTicket mapSoporteTicketDTOToSoporteTicket(SoporteTicketDTO dto) {
        SoporteTicket ticket = new SoporteTicket();
        ticket.setId(dto.getId());
        ticket.setTipoSolicitud(dto.getTipoSolicitud());
        ticket.setAsunto(dto.getAsunto());
        ticket.setMensaje(dto.getMensaje());
        ticket.setEstado(dto.getEstado());
        ticket.setRespuestaAdmin(dto.getRespuestaAdmin());
        return ticket;
    }
}

