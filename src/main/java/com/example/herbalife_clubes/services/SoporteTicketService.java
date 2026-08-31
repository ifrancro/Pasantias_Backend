package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.soporteticket.SoporteTicketDTO;
import com.example.herbalife_clubes.dtos.soporteticket.CrearSoporteTicketRequest;
import com.example.herbalife_clubes.entities.Usuario;

import java.util.List;

public interface SoporteTicketService {
    SoporteTicketDTO createTicket(CrearSoporteTicketRequest request, Integer usuarioId);
    SoporteTicketDTO getTicketAuthorized(Integer ticketId, Usuario currentUser);
    List<SoporteTicketDTO> getMyTickets(Integer usuarioId);
    List<SoporteTicketDTO> getTicketsByUsuario(Integer usuarioId);
    List<SoporteTicketDTO> getAllTickets();
    SoporteTicketDTO responderTicket(Integer ticketId, String respuestaAdmin);
    SoporteTicketDTO cambiarEstado(Integer ticketId, String estado);
}

