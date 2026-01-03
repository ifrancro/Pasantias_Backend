package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.notificacion.NotificacionDTO;
import com.example.herbalife_clubes.services.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin("*")
public class NotificacionController {
    @Autowired
    private NotificacionService notificacionService;

    @PostMapping("/enviar")
    public ResponseEntity<NotificacionDTO> enviarNotificacion(@RequestBody NotificacionDTO notificacionDTO,
                                                                @RequestParam(required = false) Integer hubId,
                                                                @RequestParam(required = false) Integer clubId,
                                                                @RequestParam(required = false) Integer usuarioId,
                                                                @RequestParam(required = false) Integer pedidoId) {
        NotificacionDTO savedNotificacionDTO = notificacionService.enviarNotificacion(
                notificacionDTO, hubId, clubId, usuarioId, pedidoId);
        return new ResponseEntity<>(savedNotificacionDTO, HttpStatus.CREATED);
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<NotificacionDTO>> getHistorialByUsuario(@PathVariable Integer usuarioId) {
        List<NotificacionDTO> notificaciones = notificacionService.getHistorialByUsuario(usuarioId);
        return ResponseEntity.ok(notificaciones);
    }

    @GetMapping("/hub/{hubId}")
    public ResponseEntity<List<NotificacionDTO>> getHistorialByHub(@PathVariable Integer hubId) {
        List<NotificacionDTO> notificaciones = notificacionService.getHistorialByHub(hubId);
        return ResponseEntity.ok(notificaciones);
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<NotificacionDTO>> getHistorialByClub(@PathVariable Integer clubId) {
        List<NotificacionDTO> notificaciones = notificacionService.getHistorialByClub(clubId);
        return ResponseEntity.ok(notificaciones);
    }

    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> getHistorial() {
        List<NotificacionDTO> notificaciones = notificacionService.getHistorial();
        return ResponseEntity.ok(notificaciones);
    }
}

