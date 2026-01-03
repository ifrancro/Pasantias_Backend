package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.soporteticket.SoporteTicketDTO;
import com.example.herbalife_clubes.services.SoporteTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/soporte-tickets")
@CrossOrigin("*")
public class SoporteTicketController {
    @Autowired
    private SoporteTicketService ticketService;

    @PostMapping
    public ResponseEntity<SoporteTicketDTO> createTicket(@RequestBody SoporteTicketDTO ticketDTO,
                                                          @RequestParam Integer usuarioId) {
        SoporteTicketDTO savedTicketDTO = ticketService.createTicket(ticketDTO, usuarioId);
        return new ResponseEntity<>(savedTicketDTO, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<SoporteTicketDTO> getTicket(@PathVariable Integer id) {
        SoporteTicketDTO ticketDTO = ticketService.getTicket(id);
        return ResponseEntity.ok(ticketDTO);
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<SoporteTicketDTO>> getTicketsByUsuario(@PathVariable Integer usuarioId) {
        List<SoporteTicketDTO> tickets = ticketService.getTicketsByUsuario(usuarioId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping
    public ResponseEntity<List<SoporteTicketDTO>> getAllTickets() {
        List<SoporteTicketDTO> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    @PatchMapping("{id}/responder")
    public ResponseEntity<SoporteTicketDTO> responderTicket(@PathVariable Integer id,
                                                             @RequestParam String respuestaAdmin) {
        SoporteTicketDTO ticketDTO = ticketService.responderTicket(id, respuestaAdmin);
        return ResponseEntity.ok(ticketDTO);
    }

    @PatchMapping("{id}/estado")
    public ResponseEntity<SoporteTicketDTO> cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {
        SoporteTicketDTO ticketDTO = ticketService.cambiarEstado(id, estado);
        return ResponseEntity.ok(ticketDTO);
    }
}

