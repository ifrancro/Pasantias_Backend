package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.prospecto.ProspectoCreateRequest;
import com.example.herbalife_clubes.dtos.prospecto.ProspectoDTO;
import com.example.herbalife_clubes.dtos.prospecto.ProspectoEstadoUpdateRequest;
import com.example.herbalife_clubes.services.UserLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProspectoController {

    private final UserLifecycleService userLifecycleService;

    @PostMapping("/clubes/{clubId}/prospectos")
    public ResponseEntity<ProspectoDTO> crearProspecto(
            @PathVariable Integer clubId,
            @Valid @RequestBody ProspectoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userLifecycleService.crearProspecto(clubId, request));
    }

    @GetMapping("/clubes/{clubId}/prospectos")
    public ResponseEntity<List<ProspectoDTO>> listarProspectos(@PathVariable Integer clubId) {
        return ResponseEntity.ok(userLifecycleService.listarProspectos(clubId));
    }

    @PatchMapping("/prospectos/{id}")
    public ResponseEntity<ProspectoDTO> actualizarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody ProspectoEstadoUpdateRequest request) {
        return ResponseEntity.ok(userLifecycleService.actualizarEstadoProspecto(id, request));
    }
}
