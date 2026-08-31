package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.soporteticket.CrearSoporteTicketRequest;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/soporte-tickets")
@Validated
public class SoporteTicketController {
    @Autowired
    private SoporteTicketService ticketService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Crear ticket para el usuario autenticado.
     * El usuario del ticket se toma del token (usuario autenticado); no se acepta usuarioId en la petición.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SoporteTicketDTO> createTicket(@Valid @RequestBody CrearSoporteTicketRequest request) {
        Usuario usuario = getAuthenticatedUsuarioOrNull();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SoporteTicketDTO savedTicketDTO = ticketService.createTicket(request, usuario.getId());
        return new ResponseEntity<>(savedTicketDTO, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SoporteTicketDTO> getTicket(@PathVariable Integer id) {
        Usuario usuario = getAuthenticatedUsuarioOrNull();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SoporteTicketDTO ticketDTO = ticketService.getTicketAuthorized(id, usuario);
        return ResponseEntity.ok(ticketDTO);
    }

    /**
     * Listar tickets del usuario autenticado.
     */
    @GetMapping("/mios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SoporteTicketDTO>> getMyTickets() {
        Usuario usuario = getAuthenticatedUsuarioOrNull();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<SoporteTicketDTO> tickets = ticketService.getMyTickets(usuario.getId());
        return ResponseEntity.ok(tickets);
    }

    /**
     * Listar tickets de un usuario arbitrario. Solo ADMIN.
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SoporteTicketDTO>> getTicketsByUsuario(@PathVariable Integer usuarioId) {
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

    private Usuario getAuthenticatedUsuarioOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return usuarioRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
