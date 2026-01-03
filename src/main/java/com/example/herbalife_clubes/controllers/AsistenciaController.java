package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.services.AsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
@CrossOrigin("*")
public class AsistenciaController {
    @Autowired
    private AsistenciaService asistenciaService;

    @PostMapping("/registrar")
    public ResponseEntity<AsistenciaDTO> registrarAsistencia(@RequestParam Integer membresiaId,
                                                               @RequestParam Integer clubId,
                                                               @RequestParam(required = false) String qrClub) {
        AsistenciaDTO asistenciaDTO = asistenciaService.registrarAsistencia(membresiaId, clubId, qrClub);
        return new ResponseEntity<>(asistenciaDTO, HttpStatus.CREATED);
    }

    @GetMapping("/socio/{membresiaId}")
    public ResponseEntity<List<AsistenciaDTO>> listarAsistenciasBySocio(@PathVariable Integer membresiaId) {
        List<AsistenciaDTO> asistencias = asistenciaService.listarAsistenciasBySocio(membresiaId);
        return ResponseEntity.ok(asistencias);
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<AsistenciaDTO>> listarAsistenciasByClub(@PathVariable Integer clubId) {
        List<AsistenciaDTO> asistencias = asistenciaService.listarAsistenciasByClub(clubId);
        return ResponseEntity.ok(asistencias);
    }

    @GetMapping
    public ResponseEntity<List<AsistenciaDTO>> listarTodasAsistencias() {
        List<AsistenciaDTO> asistencias = asistenciaService.listarTodasAsistencias();
        return ResponseEntity.ok(asistencias);
    }
}

