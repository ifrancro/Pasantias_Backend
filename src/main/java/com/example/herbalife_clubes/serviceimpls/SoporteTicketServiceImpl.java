package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.soporteticket.CrearSoporteTicketRequest;
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
import org.springframework.security.access.AccessDeniedException;
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
    public SoporteTicketDTO createTicket(CrearSoporteTicketRequest request, Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        SoporteTicket ticket = new SoporteTicket();
        ticket.setUsuario(usuario);
        ticket.setTipoSolicitud(normalizeRequiredField(request.getTipoSolicitud(), "tipoSolicitud"));
        ticket.setAsunto(normalizeRequiredField(request.getAsunto(), "asunto"));
        ticket.setMensaje(normalizeRequiredField(request.getMensaje(), "mensaje"));
        // Fuerza valores controlados por backend para evitar mass assignment.
        ticket.setEstado("ABIERTO");
        ticket.setRespuestaAdmin(null);
        ticket.setFechaRespuesta(null);

        SoporteTicket savedTicket = ticketRepository.save(ticket);
        return SoporteTicketMapper.mapSoporteTicketToSoporteTicketDTO(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public SoporteTicketDTO getTicketAuthorized(Integer ticketId, Usuario currentUser) {
        SoporteTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));
        boolean isAdmin = isAdmin(currentUser);
        Integer ownerId = ticket.getUsuario() != null ? ticket.getUsuario().getId() : null;
        if (!isAdmin && (ownerId == null || !ownerId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("No tienes permisos para acceder a este ticket.");
        }
        return SoporteTicketMapper.mapSoporteTicketToSoporteTicketDTO(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoporteTicketDTO> getMyTickets(Integer usuarioId) {
        List<SoporteTicket> tickets = ticketRepository.findByUsuarioId(usuarioId);
        return tickets.stream()
                .map(SoporteTicketMapper::mapSoporteTicketToSoporteTicketDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoporteTicketDTO> getTicketsByUsuario(Integer usuarioId) {
        List<SoporteTicket> tickets = ticketRepository.findByUsuarioId(usuarioId);
        return tickets.stream()
                .map(SoporteTicketMapper::mapSoporteTicketToSoporteTicketDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
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

    private static boolean isAdmin(Usuario user) {
        return user != null
                && user.getRol() != null
                && "ADMIN".equalsIgnoreCase(user.getRol().getNombre());
    }

    private static String normalizeRequiredField(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " es requerido");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " es requerido");
        }
        return trimmed;
    }
}

