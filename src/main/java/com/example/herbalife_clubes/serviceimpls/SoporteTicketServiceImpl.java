package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.soporteticket.SoporteTicketDTO;
import com.example.herbalife_clubes.entities.SoporteTicket;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.SoporteTicketMapper;
import com.example.herbalife_clubes.repositories.SoporteTicketRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.SoporteTicketService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SoporteTicketServiceImpl implements SoporteTicketService {
    @Autowired
    private SoporteTicketRepository ticketRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public SoporteTicketDTO createTicket(SoporteTicketDTO ticketDTO, Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        
        SoporteTicket ticket = SoporteTicketMapper.mapSoporteTicketDTOToSoporteTicket(ticketDTO);
        ticket.setUsuario(usuario);
        if (ticket.getEstado() == null) {
            ticket.setEstado("ABIERTO");
        }
        
        SoporteTicket savedTicket = ticketRepository.save(ticket);
        return SoporteTicketMapper.mapSoporteTicketToSoporteTicketDTO(savedTicket);
    }

    @Override
    public SoporteTicketDTO getTicket(Integer ticketId) {
        SoporteTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));
        return SoporteTicketMapper.mapSoporteTicketToSoporteTicketDTO(ticket);
    }

    @Override
    public List<SoporteTicketDTO> getTicketsByUsuario(Integer usuarioId) {
        List<SoporteTicket> tickets = ticketRepository.findByUsuarioId(usuarioId);
        return tickets.stream()
                .map(SoporteTicketMapper::mapSoporteTicketToSoporteTicketDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SoporteTicketDTO> getAllTickets() {
        List<SoporteTicket> tickets = ticketRepository.findAll();
        return tickets.stream()
                .map(SoporteTicketMapper::mapSoporteTicketToSoporteTicketDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SoporteTicketDTO responderTicket(Integer ticketId, String respuestaAdmin) {
        SoporteTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));
        
        ticket.setRespuestaAdmin(respuestaAdmin);
        ticket.setFechaRespuesta(LocalDateTime.now());
        ticket.setEstado("RESUELTO");
        
        SoporteTicket updatedTicket = ticketRepository.save(ticket);
        return SoporteTicketMapper.mapSoporteTicketToSoporteTicketDTO(updatedTicket);
    }

    @Override
    public SoporteTicketDTO cambiarEstado(Integer ticketId, String estado) {
        SoporteTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));
        ticket.setEstado(estado);
        SoporteTicket updatedTicket = ticketRepository.save(ticket);
        return SoporteTicketMapper.mapSoporteTicketToSoporteTicketDTO(updatedTicket);
    }
}

