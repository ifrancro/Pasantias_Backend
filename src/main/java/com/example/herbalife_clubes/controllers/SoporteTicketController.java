package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.soporteticket.ResponderTicketRequest;
import com.example.herbalife_clubes.dtos.soporteticket.SoporteTicketDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.SoporteTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/soporte-tickets")
@CrossOrigin("*")
public class SoporteTicketController {
    @Autowired
    private SoporteTicketService ticketService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Crear ticket. Solo ANFITRION y ADMIN.
     * El usuario del ticket se toma del token (usuario autenticado); no se acepta usuarioId en la petición.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ANFITRION', 'ADMIN')")
    public ResponseEntity<SoporteTicketDTO> createTicket(@RequestBody SoporteTicketDTO ticketDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Integer usuarioId = usuario.getId();
        SoporteTicketDTO savedTicketDTO = ticketService.createTicket(ticketDTO, usuarioId);
        return new ResponseEntity<>(savedTicketDTO, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<SoporteTicketDTO> getTicket(@PathVariable Integer id) {
        SoporteTicketDTO ticketDTO = ticketService.getTicket(id);
        return ResponseEntity.ok(ticketDTO);
    }

    /**
     * Listar tickets de un usuario. Solo ANFITRION y ADMIN.
     * ANFITRION solo puede consultar sus propios tickets (usuarioId debe coincidir con el del token).
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('ANFITRION', 'ADMIN')")
    public ResponseEntity<List<SoporteTicketDTO>> getTicketsByUsuario(@PathVariable Integer usuarioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario currentUser = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean isAdmin = currentUser.getRol() != null && "ADMIN".equalsIgnoreCase(currentUser.getRol().getNombre());
        if (!isAdmin && !currentUser.getId().equals(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<SoporteTicketDTO> tickets = ticketService.getTicketsByUsuario(usuarioId);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Listar todos los tickets. Solo ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SoporteTicketDTO>> getAllTickets() {
        List<SoporteTicketDTO> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    /**
     * Responder ticket. Solo ADMIN.
     * Body: { "respuesta": "texto de la respuesta" }
     */
    @PatchMapping("{id}/responder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SoporteTicketDTO> responderTicket(@PathVariable Integer id,
                                                             @RequestBody(required = false) ResponderTicketRequest body) {
        if (body == null || body.getRespuesta() == null || body.getRespuesta().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String textoRespuesta = body.getRespuesta().trim();
        SoporteTicketDTO ticketDTO = ticketService.responderTicket(id, textoRespuesta);
        return ResponseEntity.ok(ticketDTO);
    }

    /**
     * Cambiar estado del ticket. Solo ADMIN.
     */
    @PatchMapping("{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SoporteTicketDTO> cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {
        SoporteTicketDTO ticketDTO = ticketService.cambiarEstado(id, estado);
        return ResponseEntity.ok(ticketDTO);
    }
}
