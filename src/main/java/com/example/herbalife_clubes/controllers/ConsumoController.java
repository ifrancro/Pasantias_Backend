package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.consumo.ConsumoDTO;
import com.example.herbalife_clubes.services.ConsumoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/consumos")
@CrossOrigin("*")
public class ConsumoController {
    @Autowired
    private ConsumoService consumoService;

    @PostMapping("/registrar")
    public ResponseEntity<ConsumoDTO> registrarConsumo(@RequestParam Integer membresiaId,
                                                        @RequestParam Integer clubId,
                                                        @RequestParam(required = false) Integer productoId,
                                                        @RequestParam(required = false) String descripcion,
                                                        @RequestParam(required = false) Integer cantidad,
                                                        @RequestParam(required = false) BigDecimal precioReferencial,
                                                        @RequestParam(required = false) Integer asistenciaId) {
        ConsumoDTO consumoDTO = consumoService.registrarConsumo(membresiaId, clubId, productoId, 
                descripcion, cantidad, precioReferencial, asistenciaId);
        return new ResponseEntity<>(consumoDTO, HttpStatus.CREATED);
    }

    @GetMapping("/socio/{membresiaId}")
    public ResponseEntity<List<ConsumoDTO>> getHistorialBySocio(@PathVariable Integer membresiaId) {
        List<ConsumoDTO> consumos = consumoService.getHistorialBySocio(membresiaId);
        return ResponseEntity.ok(consumos);
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<ConsumoDTO>> getHistorialByClub(@PathVariable Integer clubId) {
        List<ConsumoDTO> consumos = consumoService.getHistorialByClub(clubId);
        return ResponseEntity.ok(consumos);
    }

    @GetMapping("/asistencia/{asistenciaId}")
    public ResponseEntity<List<ConsumoDTO>> getHistorialByAsistencia(@PathVariable Integer asistenciaId) {
        List<ConsumoDTO> consumos = consumoService.getHistorialByAsistencia(asistenciaId);
        return ResponseEntity.ok(consumos);
    }
}

