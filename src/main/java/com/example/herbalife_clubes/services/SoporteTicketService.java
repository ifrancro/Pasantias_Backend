package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.soporteticket.SoporteTicketDTO;

import java.util.List;

public interface SoporteTicketService {
    SoporteTicketDTO createTicket(SoporteTicketDTO ticketDTO, Integer usuarioId);
    SoporteTicketDTO getTicket(Integer ticketId);
    List<SoporteTicketDTO> getTicketsByUsuario(Integer usuarioId);
    List<SoporteTicketDTO> getAllTickets();
    SoporteTicketDTO responderTicket(Integer ticketId, String respuestaAdmin);
    SoporteTicketDTO cambiarEstado(Integer ticketId, String estado);
}

