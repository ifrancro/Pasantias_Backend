package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.prospecto.MisionProspectoCreateRequest;
import com.example.herbalife_clubes.dtos.prospecto.MisionProspectoDTO;
import com.example.herbalife_clubes.services.UserLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MisionProspectoController {

    private final UserLifecycleService userLifecycleService;

    @PostMapping("/prospectos/{prospectoId}/misiones")
    public ResponseEntity<MisionProspectoDTO> crearMision(
            @PathVariable Integer prospectoId,
            @Valid @RequestBody MisionProspectoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userLifecycleService.crearMision(prospectoId, request));
    }

    @PatchMapping("/misiones/{id}/progreso")
    public ResponseEntity<MisionProspectoDTO> incrementarProgreso(@PathVariable Integer id) {
        return ResponseEntity.ok(userLifecycleService.incrementarProgresoMision(id));
    }

    @DeleteMapping("/misiones/{id}")
    public ResponseEntity<Void> eliminarMision(@PathVariable Integer id) {
        userLifecycleService.eliminarMision(id);
        return ResponseEntity.noContent().build();
    }
}
