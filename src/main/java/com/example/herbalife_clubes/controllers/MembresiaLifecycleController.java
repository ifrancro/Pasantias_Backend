package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.dtos.membresia.*;
import com.example.herbalife_clubes.services.UserLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresias")
@RequiredArgsConstructor
public class MembresiaLifecycleController {

    private final UserLifecycleService userLifecycleService;

    @GetMapping("/{membresiaId}/compras")
    public ResponseEntity<List<CompraManualDTO>> listarCompras(@PathVariable Integer membresiaId) {
        return ResponseEntity.ok(userLifecycleService.listarCompras(membresiaId));
    }

    @PostMapping("/{membresiaId}/compras")
    public ResponseEntity<CompraManualDTO> crearCompra(
            @PathVariable Integer membresiaId,
            @Valid @RequestBody CompraManualCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userLifecycleService.crearCompra(membresiaId, request));
    }

    @GetMapping("/{membresiaId}/referidos")
    public ResponseEntity<List<ReferidoSocioDTO>> listarReferidos(@PathVariable Integer membresiaId) {
        return ResponseEntity.ok(userLifecycleService.listarReferidos(membresiaId));
    }

    @PostMapping("/{membresiaId}/asistencias")
    public ResponseEntity<AsistenciaDTO> registrarAsistenciaManual(
            @PathVariable Integer membresiaId,
            @RequestBody(required = false) AsistenciaManualCreateRequest request) {
        AsistenciaManualCreateRequest safeRequest = request != null ? request : new AsistenciaManualCreateRequest();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userLifecycleService.registrarAsistenciaManual(membresiaId, safeRequest));
    }
}
