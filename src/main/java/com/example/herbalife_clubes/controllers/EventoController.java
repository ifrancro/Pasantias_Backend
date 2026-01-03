package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.evento.EventoDTO;
import com.example.herbalife_clubes.services.EventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin("*")
public class EventoController {
    @Autowired
    private EventoService eventoService;

    @PostMapping
    public ResponseEntity<EventoDTO> createEvento(@RequestBody EventoDTO eventoDTO,
                                                  @RequestParam(required = false) Integer hubId,
                                                  @RequestParam(required = false) Integer clubId) {
        EventoDTO savedEventoDTO = eventoService.createEvento(eventoDTO, hubId, clubId);
        return new ResponseEntity<>(savedEventoDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EventoDTO>> getEventos(@RequestParam(required = false) Integer hubId,
                                                       @RequestParam(required = false) Integer clubId) {
        List<EventoDTO> eventos;
        if (hubId != null) {
            eventos = eventoService.getEventosByHub(hubId);
        } else if (clubId != null) {
            eventos = eventoService.getEventosByClub(clubId);
        } else {
            eventos = eventoService.getEventos();
        }
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("{id}")
    public ResponseEntity<EventoDTO> getEvento(@PathVariable Integer id) {
        EventoDTO eventoDTO = eventoService.getEvento(id);
        return ResponseEntity.ok(eventoDTO);
    }

    @PutMapping("{id}")
    public ResponseEntity<EventoDTO> updateEvento(@PathVariable Integer id, @RequestBody EventoDTO eventoDTO) {
        EventoDTO updatedEventoDTO = eventoService.updateEvento(id, eventoDTO);
        return ResponseEntity.ok(updatedEventoDTO);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteEvento(@PathVariable Integer id) {
        eventoService.deleteEvento(id);
        return ResponseEntity.ok("Evento eliminado exitosamente");
    }
}

